param(
    [string]$SummaryPath = "perf/generated/crash-summary.json",
    [int]$TopFailures = 20
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $SummaryPath)) {
    throw "Summary file not found: $SummaryPath"
}

$summary = Get-Content -Path $SummaryPath -Raw | ConvertFrom-Json

function Get-MetricValue {
    param(
        [string]$Name,
        [string]$Field
    )

    if ($summary.metrics.PSObject.Properties.Name -notcontains $Name) {
        return $null
    }

    return $summary.metrics.$Name.$Field
}

function Add-CheckFailures {
    param(
        [object]$Group,
        [System.Collections.Generic.List[object]]$Rows
    )

    if ($Group.checks) {
        foreach ($check in $Group.checks.PSObject.Properties) {
            $value = $check.Value
            if ([int]$value.fails -gt 0) {
                $Rows.Add([pscustomobject]@{
                    Name = $value.name
                    Passes = [int]$value.passes
                    Fails = [int]$value.fails
                    Path = $value.path
                }) | Out-Null
            }
        }
    }

    if ($Group.groups) {
        foreach ($child in $Group.groups.PSObject.Properties) {
            Add-CheckFailures -Group $child.Value -Rows $Rows
        }
    }
}

$checksRate = Get-MetricValue -Name "checks" -Field "value"
$httpFailedRate = Get-MetricValue -Name "http_req_failed" -Field "value"

[pscustomobject]@{
    SummaryPath = (Resolve-Path $SummaryPath).Path
    ChecksRatePercent = if ($checksRate -ne $null) { [math]::Round($checksRate * 100, 2) } else { $null }
    ChecksPasses = Get-MetricValue -Name "checks" -Field "passes"
    ChecksFails = Get-MetricValue -Name "checks" -Field "fails"
    HttpFailedRatePercent = if ($httpFailedRate -ne $null) { [math]::Round($httpFailedRate * 100, 2) } else { $null }
    HttpFailedCount = Get-MetricValue -Name "http_req_failed" -Field "passes"
    HttpRequests = Get-MetricValue -Name "http_reqs" -Field "count"
    Iterations = Get-MetricValue -Name "iterations" -Field "count"
    DroppedIterations = Get-MetricValue -Name "dropped_iterations" -Field "count"
    HttpP95Ms = Get-MetricValue -Name "http_req_duration" -Field "p(95)"
    HttpP99Ms = Get-MetricValue -Name "http_req_duration" -Field "p(99)"
    TrackingPropagationP95Ms = Get-MetricValue -Name "tracking_propagation_ms" -Field "p(95)"
} | Format-List

$failures = New-Object System.Collections.Generic.List[object]
Add-CheckFailures -Group $summary.root_group -Rows $failures

if ($failures.Count -gt 0) {
    "Top failing checks:"
    $failures |
        Sort-Object Fails -Descending |
        Select-Object -First $TopFailures Name, Passes, Fails, Path |
        Format-Table -AutoSize
}
else {
    "No failed checks found."
}
