param(
    [ValidateSet("smoke", "nominal", "stress", "crash", "vm1-frontdoor", "packages-write-heavy", "db-pressure", "public-realistic", "regression", "security-pentest", "guest-checkout", "admin-dashboards", "courier-lifecycle", "tracking-live", "customer-onboarding", "courier-onboarding", "e2e-delivery", "seed-visible-packages", "seed-sucy-creteil-package")]
    [string]$Suite = "smoke",

    [string]$EnvFile = "",

    [string[]]$ExtraArgs = @()
)

$ErrorActionPreference = "Stop"

$envMutex = $null

function Enter-PerfEnvMutex {
    $script:envMutex = New-Object System.Threading.Mutex($false, "Global\QuickDeliveryPerfEnvLock")
    if (-not $script:envMutex.WaitOne([TimeSpan]::FromSeconds(30))) {
        throw "Unable to acquire perf env lock within 30 seconds. Another perf command is probably running."
    }
}

function Exit-PerfEnvMutex {
    if ($script:envMutex) {
        try {
            $script:envMutex.ReleaseMutex() | Out-Null
        }
        catch {
        }
        finally {
            $script:envMutex.Dispose()
            $script:envMutex = $null
        }
    }
}

function Resolve-PerfRoot {
    if ($PSScriptRoot) {
        return (Resolve-Path $PSScriptRoot).Path
    }

    if ($PSCommandPath) {
        return (Resolve-Path (Split-Path -Parent $PSCommandPath)).Path
    }

    throw "Unable to resolve perf root."
}

function Load-EnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Write-Host "Loading env file: $Path"
    Get-Content -Path $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $separatorIndex = $line.IndexOf("=")
        if ($separatorIndex -lt 1) {
            return
        }

        $name = $line.Substring(0, $separatorIndex).Trim()
        $value = $line.Substring($separatorIndex + 1).Trim()

        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "Env:$name" -Value $value
    }
}

function Resolve-EnvFile {
    param(
        [string]$RequestedFile,
        [string]$PerfRoot
    )

    if ($RequestedFile) {
        return (Resolve-Path $RequestedFile).Path
    }

    $preferredFiles = @(
        (Join-Path $PerfRoot ".env.local"),
        (Join-Path $PerfRoot ".env"),
        (Join-Path $PerfRoot ".env.example")
    )

    foreach ($candidate in $preferredFiles) {
        if (Test-Path $candidate) {
            return (Resolve-Path $candidate).Path
        }
    }

    throw "No env file found in perf/. Create .env.local or provide -EnvFile."
}

function Resolve-TargetScript {
    param(
        [string]$PerfRoot,
        [string]$SelectedSuite
    )

    $mapping = @{
        "smoke" = "suites/smoke.js"
        "nominal" = "suites/nominal.js"
        "stress" = "suites/stress.js"
        "crash" = "suites/crash.js"
        "vm1-frontdoor" = "suites/vm1-frontdoor.js"
        "packages-write-heavy" = "suites/packages-write-heavy.js"
        "db-pressure" = "suites/db-pressure.js"
        "public-realistic" = "suites/public-realistic.js"
        "regression" = "suites/regression.js"
        "security-pentest" = "suites/security-pentest.js"
        "guest-checkout" = "scenarios/guest-checkout.js"
        "admin-dashboards" = "scenarios/admin-dashboards.js"
        "courier-lifecycle" = "scenarios/courier-lifecycle.js"
        "tracking-live" = "scenarios/tracking-live.js"
        "customer-onboarding" = "scenarios/customer-onboarding.js"
        "courier-onboarding" = "scenarios/courier-onboarding.js"
        "e2e-delivery" = "scenarios/e2e-delivery.js"
        "seed-visible-packages" = "scenarios/seed-visible-packages.js"
        "seed-sucy-creteil-package" = "scenarios/seed-sucy-creteil-package.js"
    }

    $relativePath = $mapping[$SelectedSuite]
    if (-not $relativePath) {
        throw "Unsupported suite: $SelectedSuite"
    }

    return Join-Path $PerfRoot $relativePath
}

function Should-RefreshAuth {
    param(
        [string]$SelectedSuite
    )

    return $SelectedSuite -ne "guest-checkout"
}

function Resolve-InitScript {
    param(
        [string]$PerfRoot
    )

    $initScript = Join-Path $PerfRoot "init-env.ps1"
    if (-not (Test-Path $initScript)) {
        throw "Unable to locate init script: $initScript"
    }

    return $initScript
}

function Refresh-EnvAuthentication {
    param(
        [string]$PerfRoot,
        [string]$EnvPath
    )

    $initScript = Resolve-InitScript -PerfRoot $PerfRoot
    $adminCredentialsAvailable = $env:ADMIN_USERNAME -and $env:ADMIN_PASSWORD
    $courierCredentialsAvailable = $env:COURIER_USERNAME -and $env:COURIER_PASSWORD

    if (-not $adminCredentialsAvailable -and -not $courierCredentialsAvailable) {
        Write-Host "Skipping token refresh: no saved admin or courier credentials found."
        return
    }

    Write-Host "Refreshing perf credentials via init-env.ps1"
    $arguments = @(
        "-ExecutionPolicy", "Bypass",
        "-File", $initScript,
        "-EnvFile", $EnvPath
    )

    if ($adminCredentialsAvailable) {
        $arguments += @(
            "-AdminUsername", $env:ADMIN_USERNAME,
            "-AdminPassword", $env:ADMIN_PASSWORD
        )
    }

    if ($courierCredentialsAvailable) {
        $arguments += @(
            "-CourierUsername", $env:COURIER_USERNAME,
            "-CourierPassword", $env:COURIER_PASSWORD
        )
    }

    & powershell @arguments
}

function Resolve-K6Command {
    $command = Get-Command k6 -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidatePaths = @(
        "C:\Program Files\k6\k6.exe",
        "C:\Program Files (x86)\k6\k6.exe",
        (Join-Path $env:LOCALAPPDATA "Programs\k6\k6.exe")
    )

    foreach ($candidate in $candidatePaths) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    return $null
}

$perfRoot = Resolve-PerfRoot
$envPath = Resolve-EnvFile -RequestedFile $EnvFile -PerfRoot $perfRoot

if (Should-RefreshAuth -SelectedSuite $Suite) {
    Enter-PerfEnvMutex
    try {
        Load-EnvFile -Path $envPath
    }
    finally {
        Exit-PerfEnvMutex
    }

    Refresh-EnvAuthentication -PerfRoot $perfRoot -EnvPath $envPath
}

Enter-PerfEnvMutex
try {
    Load-EnvFile -Path $envPath

    $k6Command = Resolve-K6Command
    if (-not $k6Command) {
        throw "k6 is not installed or not available in PATH."
    }

    $targetScript = Resolve-TargetScript -PerfRoot $perfRoot -SelectedSuite $Suite
    if (-not (Test-Path $targetScript)) {
        throw "Unable to locate k6 script: $targetScript"
    }

    $arguments = @("run", $targetScript) + $ExtraArgs

    Write-Host "Running k6 suite '$Suite' with script: $targetScript"
    & $k6Command @arguments
}
finally {
    Exit-PerfEnvMutex
}
