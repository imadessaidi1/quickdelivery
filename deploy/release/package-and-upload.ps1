param(
    [string]$ServerIp = "15.224.36.99",
    [string]$SshUser = "ubuntu",
    [string]$SshKeyPath = "C:\Users\imess\.ssh\lightsail.pem",
    [string]$RemoteRoot = "/opt/quickdelivery",
    [switch]$SkipBackendBuild,
    [switch]$SkipFrontendBuild,
    [switch]$SkipBackendUpload,
    [switch]$SkipFrontendUpload,
    [string[]]$BackendModules = @()
)

$ErrorActionPreference = "Stop"

if ($BackendModules.Count -eq 1 -and $BackendModules[0] -match ",") {
    $BackendModules = $BackendModules[0].Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
}

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$frontendRoot = Join-Path $projectRoot "quickdelivery-googlemaps-front"
$distDir = Join-Path $frontendRoot "dist"
$configDir = Join-Path $projectRoot "config"
$envProd = Join-Path $projectRoot "deploy\docker\.env.production"
$remoteTarget = "$SshUser@$ServerIp"
$sshOptions = @(
    "-o", "BatchMode=yes",
    "-o", "ConnectTimeout=15",
    "-o", "ServerAliveInterval=15",
    "-o", "ServerAliveCountMax=3"
)

function Write-Step([string]$Message) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp] $Message"
}

function Invoke-SshCommand([string]$Description, [string]$Command) {
    Write-Step "$Description"
    Write-Host "  ssh $remoteTarget `"$Command`""
    & ssh @sshOptions -i $SshKeyPath $remoteTarget $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE"
    }
    Write-Step "$Description completed."
}

function Invoke-ScpUpload([string]$Description, [string[]]$Arguments) {
    Write-Step "$Description"
    Write-Host "  scp $($Arguments -join ' ')"
    & scp @sshOptions -i $SshKeyPath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE"
    }
    Write-Step "$Description completed."
}

$moduleMap = @{
    "config-server" = "quickdelivery-config-server.jar"
    "discovery-server" = "quickdelivery-registry-server.jar"
    "oauth" = "oauth-authorization-server.jar"
    "api-gateway" = "quickdelivery-api-gateway.jar"
    "users" = "quickdelivery-users.jar"
    "packages" = "quickdelivery-packages.jar"
}

if (-not (Test-Path $SshKeyPath)) {
    throw "SSH key not found: $SshKeyPath"
}

if (-not $SkipBackendBuild) {
    Write-Host "Preparing backend artifacts..."
    $prepareArgs = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $projectRoot "deploy\docker\prepare-artifacts.ps1")
    )
    if ($BackendModules.Count -gt 0) {
        $prepareArgs += "-RequestedModules"
        $prepareArgs += $BackendModules
    }
    & powershell @prepareArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Backend artifact preparation failed with exit code $LASTEXITCODE"
    }
}

if (-not $SkipFrontendBuild) {
    Write-Step "Building frontend..."
    Push-Location $frontendRoot
    try {
        & npm run build
        if ($LASTEXITCODE -ne 0) {
            throw "Frontend build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
    Write-Step "Frontend build completed."
}

if (-not $SkipFrontendUpload -and -not (Test-Path $distDir)) {
    throw "Frontend dist not found: $distDir"
}

Invoke-SshCommand "Ensuring remote directories" "sudo mkdir -p $RemoteRoot /tmp/quickdelivery-front-dist /tmp/quickdelivery-release && sudo chown -R ${SshUser}:${SshUser} $RemoteRoot /tmp/quickdelivery-front-dist /tmp/quickdelivery-release"

if (-not $SkipBackendUpload) {
    Write-Step "Uploading backend payload..."

    if ($BackendModules.Count -gt 0) {
        Invoke-SshCommand "Preparing remote artifacts directory" "mkdir -p $RemoteRoot/artifacts"
        foreach ($module in $BackendModules) {
            if (-not $moduleMap.ContainsKey($module)) {
                $valid = ($moduleMap.Keys | Sort-Object) -join ", "
                throw "Unknown backend module '$module'. Valid values: $valid"
            }

            $artifactName = $moduleMap[$module]
            $artifactPath = Join-Path $projectRoot "artifacts\$artifactName"
            if (-not (Test-Path $artifactPath)) {
                throw "Artifact not found for module '$module': $artifactPath"
            }
            Invoke-ScpUpload "Uploading artifact $artifactName" @($artifactPath, "${remoteTarget}:${RemoteRoot}/artifacts/$artifactName")
        }
    }
    else {
        Invoke-ScpUpload "Uploading full artifacts directory" @("-r", (Join-Path $projectRoot "artifacts"), "${remoteTarget}:${RemoteRoot}/")
    }
}
else {
    Write-Step "Skipping backend upload."
}

Write-Step "Uploading deploy scripts and configuration..."
Invoke-ScpUpload "Uploading deploy directory" @("-r", (Join-Path $projectRoot "deploy"), "${remoteTarget}:${RemoteRoot}/")
Invoke-ScpUpload "Uploading certs directory" @("-r", (Join-Path $projectRoot "certs"), "${remoteTarget}:${RemoteRoot}/")

if (-not $SkipBackendUpload) {
    $configStageDir = Join-Path ([System.IO.Path]::GetTempPath()) ("quickdelivery-config-upload-" + [System.Guid]::NewGuid().ToString("N"))
    try {
        New-Item -ItemType Directory -Force -Path $configStageDir | Out-Null
        $configTargetDir = Join-Path $configStageDir "config"
        New-Item -ItemType Directory -Force -Path $configTargetDir | Out-Null

        Get-ChildItem -Force $configDir | Where-Object { $_.Name -notin @(".git", ".DS_Store") } | ForEach-Object {
            Copy-Item $_.FullName -Destination $configTargetDir -Recurse -Force
        }

        Invoke-ScpUpload "Uploading config directory" @("-r", $configTargetDir, "${remoteTarget}:${RemoteRoot}/")
    }
    finally {
        if (Test-Path $configStageDir) {
            Remove-Item -Recurse -Force $configStageDir
        }
    }

    if (Test-Path $envProd) {
        Invoke-ScpUpload "Uploading deploy/docker/.env.production" @($envProd, "${remoteTarget}:${RemoteRoot}/deploy/docker/.env.production")
    }
    else {
        Write-Warning "deploy/docker/.env.production not found locally. Remote file will be kept as-is."
    }
}

if (-not $SkipFrontendUpload) {
    Write-Step "Uploading frontend build and nginx template..."
    Invoke-SshCommand "Resetting remote frontend temp directory" "rm -rf /tmp/quickdelivery-front-dist && mkdir -p /tmp/quickdelivery-front-dist"
    Invoke-ScpUpload "Uploading frontend dist" @("-r", "$distDir\*", "${remoteTarget}:/tmp/quickdelivery-front-dist/")
    Invoke-ScpUpload "Uploading nginx template" @((Join-Path $projectRoot "deploy\nginx\quickdelivery.conf.template"), "${remoteTarget}:/tmp/quickdelivery.conf")
}
else {
    Write-Step "Skipping frontend upload."
}

Write-Step "Upload complete."
Write-Host "Next:"
Write-Host "  ssh -i $SshKeyPath $remoteTarget"
Write-Host "  sudo bash $RemoteRoot/deploy/release/release-on-vm.sh"
