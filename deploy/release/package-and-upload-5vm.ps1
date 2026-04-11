param(
    [ValidateSet("vm1-gateway", "vm2-platform", "vm3-app", "vm4-app", "vm5-mysql")]
    [string]$Role,
    [string]$ServerIp,
    [string]$SshUser = "ubuntu",
    [string]$SshKeyPath = "C:\Users\imess\.ssh\lightsail.pem",
    [string]$RemoteRoot = "/opt/quickdelivery",
    [string]$EnvFile,
    [switch]$SkipBackendBuild,
    [switch]$SkipFrontendBuild,
    [switch]$SkipBackendUpload,
    [switch]$SkipFrontendUpload,
    [string[]]$BackendModules = @()
)

$ErrorActionPreference = "Stop"

if (-not $ServerIp) {
    throw "-ServerIp is required."
}

if ($BackendModules.Count -eq 1 -and $BackendModules[0] -match ",") {
    $BackendModules = $BackendModules[0].Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
}

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$frontendRoot = Join-Path $projectRoot "quickdelivery-googlemaps-front"
$distDir = Join-Path $frontendRoot "dist"
$configDir = Join-Path $projectRoot "config"
$remoteTarget = "$SshUser@$ServerIp"
$sshOptions = @(
    "-o", "BatchMode=yes",
    "-o", "ConnectTimeout=15",
    "-o", "ServerAliveInterval=15",
    "-o", "ServerAliveCountMax=3"
)

$roleDefaultModules = @{
    "vm1-gateway" = @("api-gateway")
    "vm2-platform" = @("config-server", "discovery-server", "oauth")
    "vm3-app" = @("users", "packages")
    "vm4-app" = @("users", "packages")
    "vm5-mysql" = @()
}

$moduleMap = @{
    "config-server" = "quickdelivery-config-server.jar"
    "discovery-server" = "quickdelivery-registry-server.jar"
    "oauth" = "oauth-authorization-server.jar"
    "api-gateway" = "quickdelivery-api-gateway.jar"
    "users" = "quickdelivery-users.jar"
    "packages" = "quickdelivery-packages.jar"
}

if (-not $EnvFile) {
    $EnvFile = Join-Path $projectRoot "deploy\lightsail\$Role.env"
}

function Write-Step([string]$Message) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp] $Message"
}

function Invoke-SshCommand([string]$Description, [string]$Command) {
    Write-Step $Description
    & ssh @sshOptions -i $SshKeyPath $remoteTarget $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE"
    }
}

function Invoke-ScpUpload([string]$Description, [string[]]$Arguments) {
    Write-Step $Description
    & scp @sshOptions -i $SshKeyPath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE"
    }
}

if (-not (Test-Path $SshKeyPath)) {
    throw "SSH key not found: $SshKeyPath"
}

$effectiveModules = if ($BackendModules.Count -gt 0) { $BackendModules } else { $roleDefaultModules[$Role] }

if (-not $SkipBackendBuild -and $effectiveModules.Count -gt 0) {
    Write-Step "Preparing backend artifacts for $Role..."
    $modulesStr = $effectiveModules -join ","
    $prepareArgs = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $projectRoot "deploy\docker\prepare-artifacts.ps1"),
        "-RequestedModules", $modulesStr
    )
    & powershell @prepareArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Backend artifact preparation failed with exit code $LASTEXITCODE"
    }
}

if (($Role -eq "vm1-gateway") -and -not $SkipFrontendBuild) {
    Write-Step "Building frontend..."
    Push-Location $frontendRoot
    try {
        $previousNodeOptions = $env:NODE_OPTIONS
        $env:NODE_OPTIONS = "--max-old-space-size=4096"
        & npm run build
        if ($LASTEXITCODE -ne 0) {
            throw "Frontend build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        $env:NODE_OPTIONS = $previousNodeOptions
        Pop-Location
    }
}

Invoke-SshCommand "Ensuring remote directories" "sudo mkdir -p $RemoteRoot /tmp/quickdelivery-front-dist /tmp/quickdelivery-release $RemoteRoot/deploy/docker/5vm/certs && sudo chown -R ${SshUser}:${SshUser} $RemoteRoot /tmp/quickdelivery-front-dist /tmp/quickdelivery-release"

if (-not $SkipBackendUpload -and $effectiveModules.Count -gt 0) {
    Invoke-SshCommand "Preparing remote artifacts directory" "mkdir -p $RemoteRoot/artifacts"
    foreach ($module in $effectiveModules) {
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

Write-Step "Uploading deploy scripts, configuration and certs..."
Invoke-ScpUpload "Uploading deploy directory" @("-r", (Join-Path $projectRoot "deploy"), "${remoteTarget}:${RemoteRoot}/")
Invoke-ScpUpload "Uploading certs directory (root)" @("-r", (Join-Path $projectRoot "certs"), "${remoteTarget}:${RemoteRoot}/")
Invoke-ScpUpload "Uploading certs directory (docker-compose side)" @("-r", (Join-Path $projectRoot "certs"), "${remoteTarget}:${RemoteRoot}/deploy/docker/5vm/")

if (($Role -in @("vm2-platform", "vm3-app", "vm4-app")) -and -not $SkipBackendUpload) {
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
}

if (Test-Path $EnvFile) {
    Invoke-ScpUpload "Uploading role env file" @($EnvFile, "${remoteTarget}:${RemoteRoot}/deploy/docker/5vm/.env.$Role")
}
else {
    Write-Warning "Role env file not found locally: $EnvFile. Remote file will be kept as-is."
}

if (($Role -eq "vm1-gateway") -and -not $SkipFrontendUpload) {
    if (-not (Test-Path $distDir)) {
        throw "Frontend dist not found: $distDir"
    }
    Invoke-SshCommand "Resetting remote frontend temp directory" "rm -rf /tmp/quickdelivery-front-dist && mkdir -p /tmp/quickdelivery-front-dist"
    Invoke-ScpUpload "Uploading frontend dist" @("-r", "$distDir\*", "${remoteTarget}:/tmp/quickdelivery-front-dist/")
    Invoke-ScpUpload "Uploading 5vm nginx template" @((Join-Path $projectRoot "deploy\nginx\quickdelivery.5vm.conf.template"), "${remoteTarget}:/tmp/quickdelivery.5vm.conf")
}

Write-Step "Upload complete for $Role."
Write-Host "Next:"
Write-Host "  ssh -i $SshKeyPath $remoteTarget"
Write-Host "  sudo RELEASE_ROLE=$Role bash $RemoteRoot/deploy/release/release-on-vm-5vm.sh"
