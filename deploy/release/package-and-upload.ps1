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
    Write-Host "Building frontend..."
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
}

if (-not $SkipFrontendUpload -and -not (Test-Path $distDir)) {
    throw "Frontend dist not found: $distDir"
}

Write-Host "Ensuring remote directories..."
& ssh @sshOptions -i $SshKeyPath $remoteTarget "mkdir -p $RemoteRoot /tmp/quickdelivery-front-dist /tmp/quickdelivery-release"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to prepare remote directories on $remoteTarget"
}
Write-Host "Remote directories ready."

if (-not $SkipBackendUpload) {
    Write-Host "Uploading backend payload..."

    if ($BackendModules.Count -gt 0) {
        Write-Host "Preparing remote artifacts directory..."
        & ssh @sshOptions -i $SshKeyPath $remoteTarget "mkdir -p $RemoteRoot/artifacts"
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to prepare remote artifacts directory"
        }
        Write-Host "Remote artifacts directory ready."
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
            Write-Host "Uploading artifact: $artifactName"
            & scp @sshOptions -i $SshKeyPath $artifactPath "${remoteTarget}:${RemoteRoot}/artifacts/$artifactName"
            if ($LASTEXITCODE -ne 0) {
                throw "Upload failed for artifact $artifactName"
            }
            Write-Host "Uploaded artifact: $artifactName"
        }
    }
    else {
        Write-Host "Uploading full artifacts directory..."
        & scp @sshOptions -i $SshKeyPath -r (Join-Path $projectRoot "artifacts") "${remoteTarget}:${RemoteRoot}/"
        if ($LASTEXITCODE -ne 0) {
            throw "Artifacts upload failed"
        }
        Write-Host "Uploaded full artifacts directory."
    }
}
else {
    Write-Host "Skipping backend upload."
}

Write-Host "Uploading deploy scripts and configuration..."
Write-Host "Uploading deploy directory..."
& scp @sshOptions -i $SshKeyPath -r (Join-Path $projectRoot "deploy") "${remoteTarget}:${RemoteRoot}/"
if ($LASTEXITCODE -ne 0) {
    throw "Deploy directory upload failed"
}
Write-Host "Uploaded deploy directory."

if (-not $SkipBackendUpload) {
    $configStageDir = Join-Path ([System.IO.Path]::GetTempPath()) ("quickdelivery-config-upload-" + [System.Guid]::NewGuid().ToString("N"))
    try {
        New-Item -ItemType Directory -Force -Path $configStageDir | Out-Null
        $configTargetDir = Join-Path $configStageDir "config"
        New-Item -ItemType Directory -Force -Path $configTargetDir | Out-Null

        Get-ChildItem -Force $configDir | Where-Object { $_.Name -notin @(".git", ".DS_Store") } | ForEach-Object {
            Copy-Item $_.FullName -Destination $configTargetDir -Recurse -Force
        }

        Write-Host "Uploading config directory..."
        & scp @sshOptions -i $SshKeyPath -r $configTargetDir "${remoteTarget}:${RemoteRoot}/"
        if ($LASTEXITCODE -ne 0) {
            throw "Config upload failed"
        }
        Write-Host "Uploaded config directory."
    }
    finally {
        if (Test-Path $configStageDir) {
            Remove-Item -Recurse -Force $configStageDir
        }
    }

    if (Test-Path $envProd) {
        Write-Host "Uploading deploy/docker/.env.production..."
        & scp @sshOptions -i $SshKeyPath $envProd "${remoteTarget}:${RemoteRoot}/deploy/docker/.env.production"
        if ($LASTEXITCODE -ne 0) {
            throw "deploy/docker/.env.production upload failed"
        }
        Write-Host "Uploaded deploy/docker/.env.production."
    }
    else {
        Write-Warning "deploy/docker/.env.production not found locally. Remote file will be kept as-is."
    }
}

if (-not $SkipFrontendUpload) {
    Write-Host "Uploading frontend build and nginx template..."
    & ssh @sshOptions -i $SshKeyPath $remoteTarget "rm -rf /tmp/quickdelivery-front-dist && mkdir -p /tmp/quickdelivery-front-dist"
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to reset remote frontend temp directory"
    }
    Write-Host "Remote frontend temp directory ready."
    & scp @sshOptions -i $SshKeyPath -r "$distDir\*" "${remoteTarget}:/tmp/quickdelivery-front-dist/"
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend dist upload failed"
    }
    Write-Host "Uploaded frontend dist."
    & scp @sshOptions -i $SshKeyPath (Join-Path $projectRoot "deploy\nginx\quickdelivery.conf.template") "${remoteTarget}:/tmp/quickdelivery.conf"
    if ($LASTEXITCODE -ne 0) {
        throw "Nginx template upload failed"
    }
    Write-Host "Uploaded nginx template."
}
else {
    Write-Host "Skipping frontend upload."
}

Write-Host "Upload complete."
Write-Host "Next:"
Write-Host "  ssh -i $SshKeyPath $remoteTarget"
Write-Host "  sudo bash $RemoteRoot/deploy/release/release-on-vm.sh"
