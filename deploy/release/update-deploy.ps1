param(
    [ValidateSet("full", "backend", "frontend", "module")]
    [string]$Mode = "full",
    [string]$ServerIp = "15.224.36.99",
    [string]$SshUser = "ubuntu",
    [string]$SshKeyPath = "C:\Users\imess\.ssh\lightsail.pem",
    [string]$RemoteRoot = "/opt/quickdelivery",
    [string[]]$Modules = @()
)

$ErrorActionPreference = "Stop"

if ($Modules.Count -eq 1 -and $Modules[0] -match ",") {
    $Modules = $Modules[0].Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
}

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$remoteTarget = "$SshUser@$ServerIp"
$packageScript = Join-Path $projectRoot "deploy\release\package-and-upload.ps1"

function Write-Step([string]$Message) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp] $Message"
}

$packageArgs = @(
    "-ExecutionPolicy", "Bypass",
    "-File", $packageScript,
    "-ServerIp", $ServerIp,
    "-SshUser", $SshUser,
    "-SshKeyPath", $SshKeyPath,
    "-RemoteRoot", $RemoteRoot
)

switch ($Mode) {
    "backend" {
        $packageArgs += "-SkipFrontendBuild"
        $packageArgs += "-SkipFrontendUpload"
    }
    "frontend" {
        $packageArgs += "-SkipBackendBuild"
        $packageArgs += "-SkipBackendUpload"
    }
    "module" {
        if ($Modules.Count -eq 0) {
            throw "Modules are required when -Mode module. Example: -Modules users packages"
        }
        $packageArgs += "-SkipFrontendBuild"
        $packageArgs += "-SkipFrontendUpload"
        $packageArgs += "-BackendModules"
        $packageArgs += $Modules
    }
}

Write-Step "Starting local package/upload step in mode $Mode"
& powershell @packageArgs
if ($LASTEXITCODE -ne 0) {
    throw "Local package/upload step failed with exit code $LASTEXITCODE"
}
Write-Step "Local package/upload step completed."

$remoteCommand = "sudo RELEASE_MODE=$Mode"
if ($Mode -eq "module") {
    $remoteCommand += " RELEASE_MODULES=$($Modules -join ',')"
}
$remoteCommand += " bash $RemoteRoot/deploy/release/release-on-vm.sh"
Write-Step "Starting remote release step"
Write-Host "  ssh $remoteTarget `"$remoteCommand`""
& ssh -i $SshKeyPath $remoteTarget $remoteCommand
if ($LASTEXITCODE -ne 0) {
    throw "Remote release step failed with exit code $LASTEXITCODE"
}
Write-Step "Remote release step completed."

Write-Host "Deployment update completed in mode: $Mode"
