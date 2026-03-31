param(
    [ValidateSet("full", "backend", "frontend", "module")]
    [string]$Mode = "full",
    [ValidateSet("vm1-gateway", "vm2-platform", "vm3-app", "vm4-app", "vm5-mysql")]
    [string]$Role,
    [string]$ServerIp,
    [string]$SshUser = "ubuntu",
    [string]$SshKeyPath = "C:\Users\imess\.ssh\lightsail.pem",
    [string]$RemoteRoot = "/opt/quickdelivery",
    [string]$EnvFile,
    [string[]]$Modules = @()
)

$ErrorActionPreference = "Stop"

if (-not $Role) {
    throw "-Role is required."
}

if (-not $ServerIp) {
    throw "-ServerIp is required."
}

if ($Modules.Count -eq 1 -and $Modules[0] -match ",") {
    $Modules = $Modules[0].Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
}

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$packageScript = Join-Path $projectRoot "deploy\release\package-and-upload-5vm.ps1"
$remoteTarget = "$SshUser@$ServerIp"

function Write-Step([string]$Message) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp] $Message"
}

$packageArgs = @(
    "-ExecutionPolicy", "Bypass",
    "-File", $packageScript,
    "-Role", $Role,
    "-ServerIp", $ServerIp,
    "-SshUser", $SshUser,
    "-SshKeyPath", $SshKeyPath,
    "-RemoteRoot", $RemoteRoot
)

if ($EnvFile) {
    $packageArgs += "-EnvFile"
    $packageArgs += $EnvFile
}

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

Write-Step "Starting local package/upload step for $Role in mode $Mode"
& powershell @packageArgs
if ($LASTEXITCODE -ne 0) {
    throw "Local package/upload step failed with exit code $LASTEXITCODE"
}

$remoteCommand = "sudo RELEASE_ROLE=$Role RELEASE_MODE=$Mode"
if ($Mode -eq "module") {
    $remoteCommand += " RELEASE_MODULES=$($Modules -join ',')"
}
$remoteCommand += " bash $RemoteRoot/deploy/release/release-on-vm-5vm.sh"

Write-Step "Starting remote release step for $Role"
& ssh -i $SshKeyPath $remoteTarget $remoteCommand
if ($LASTEXITCODE -ne 0) {
    throw "Remote release step failed with exit code $LASTEXITCODE"
}

Write-Step "Remote release step completed for $Role"
