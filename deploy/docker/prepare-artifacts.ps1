param(
    [string[]]$RequestedModules = @()
)

$ErrorActionPreference = "Stop"

if ($RequestedModules.Count -eq 1 -and $RequestedModules[0] -match ",") {
    $RequestedModules = $RequestedModules[0].Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
}

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$artifactsDir = Join-Path $root "artifacts"

$allModules = @(
    [pscustomobject]@{ Key = "config-server"; Name = "quickdemivery-config-server"; Artifact = "quickdelivery-config-server.jar" }
    [pscustomobject]@{ Key = "discovery-server"; Name = "quickdelivery-registy-server"; Artifact = "quickdelivery-registry-server.jar" }
    [pscustomobject]@{ Key = "oauth"; Name = "oauth-authorization-server"; Artifact = "oauth-authorization-server.jar" }
    [pscustomobject]@{ Key = "api-gateway"; Name = "quickdelivery-api-gateway"; Artifact = "quickdelivery-api-gateway.jar" }
    [pscustomobject]@{ Key = "users"; Name = "quickdelivery-users"; Artifact = "quickdelivery-users.jar" }
    [pscustomobject]@{ Key = "packages"; Name = "quickdelivery-packages"; Artifact = "quickdelivery-packages.jar" }
)

if ($RequestedModules.Count -gt 0) {
    $requested = [System.Collections.Generic.List[object]]::new()
    foreach ($requestedModule in $RequestedModules) {
        $match = $allModules | Where-Object {
            $_.Key -eq $requestedModule -or $_.Name -eq $requestedModule -or $_.Artifact -eq $requestedModule
        } | Select-Object -First 1

        if (-not $match) {
            $valid = ($allModules | ForEach-Object { $_.Key }) -join ", "
            throw "Unknown module '$requestedModule'. Valid values: $valid"
        }

        if (-not ($requested | Where-Object { $_.Name -eq $match.Name })) {
            $requested.Add($match)
        }
    }
    $selectedModules = @($requested)
}
else {
    $selectedModules = @($allModules)
}

New-Item -ItemType Directory -Force -Path $artifactsDir | Out-Null

Push-Location $root
try {
    $projectList = ($selectedModules | ForEach-Object { $_.Name }) -join ","
    if ([string]::IsNullOrWhiteSpace($projectList)) {
        throw "No Maven projects resolved for requested modules."
    }

    & mvn "-pl" $projectList "-am" "clean" "package" "-DskipTests"
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }

    foreach ($module in $selectedModules) {
        $targetDir = Join-Path $root "$($module.Name)\target"
        if (-not (Test-Path $targetDir)) {
            throw "Target directory not found for module $($module.Name): $targetDir"
        }
        $jar = Get-ChildItem -Path $targetDir -Filter *.jar |
            Where-Object { $_.Name -notlike "*.jar.original" } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1

        if (-not $jar) {
            throw "No jar found for module $($module.Name) in $targetDir"
        }

        Copy-Item $jar.FullName (Join-Path $artifactsDir $module.Artifact) -Force
    }
}
finally {
    Pop-Location
}

Write-Host "Artifacts prepared in $artifactsDir"
