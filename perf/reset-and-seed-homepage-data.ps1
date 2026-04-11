param(
    [string]$BaseUrl = "https://api.quickdelivery.fr",
    [string]$Locale = "fr",
    [string]$SshKeyPath = "C:\Users\imess\.ssh\lightsail.pem",
    [string]$MysqlVmPublicIp = "15.188.208.12",
    [string]$MysqlSshUser = "ubuntu",
    [switch]$Execute,
    [string]$ConfirmationPhrase = ""
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function Resolve-RepoRoot {
    if ($PSScriptRoot) {
        return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
    }
    throw "Unable to resolve repository root."
}

function Load-KeyValueFile {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path $Path)) {
        return $values
    }

    Get-Content -Path $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) {
            return
        }
        $separator = $line.IndexOf('=')
        if ($separator -lt 1) {
            return
        }
        $name = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1).Trim().Trim('"').Trim("'")
        $values[$name] = $value
    }

    return $values
}

function Invoke-RemoteMysql {
    param(
        [string]$Sql
    )

    $localTempFile = [System.IO.Path]::GetTempFileName()
    $remoteTempFile = "/tmp/qd-homepage-seed-$([guid]::NewGuid().ToString('N')).sql"

    try {
        Set-Content -Path $localTempFile -Value $Sql -Encoding UTF8 -NoNewline
        & scp -i $SshKeyPath $localTempFile "${MysqlSshUser}@${MysqlVmPublicIp}:$remoteTempFile" | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to copy SQL file to MySQL VM."
        }

        $command = "sudo docker exec -i quickdelivery-mysql mysql -u root -p`"$script:MysqlRootPassword`" -N -B < $remoteTempFile; rm -f $remoteTempFile"
        & ssh -i $SshKeyPath "$MysqlSshUser@$MysqlVmPublicIp" $command
        if ($LASTEXITCODE -ne 0) {
            throw "Remote MySQL execution failed."
        }
    } finally {
        Remove-Item -Path $localTempFile -Force -ErrorAction SilentlyContinue
    }
}

function Require-ExecutionConfirmation {
    if (-not $Execute) {
        Write-Host "Dry-run only. No database cleanup or API seeding will be executed."
        Write-Host "To execute: .\perf\reset-and-seed-homepage-data.ps1 -Execute -ConfirmationPhrase RESET_QUICKDELIVERY_ALL_PACKAGES"
        return $false
    }

    if ($ConfirmationPhrase -ne 'RESET_QUICKDELIVERY_ALL_PACKAGES') {
        throw "Refusing to execute reset: invalid confirmation phrase."
    }

    return $true
}

function New-FutureIso {
    param([int]$MinutesAhead)

    return (Get-Date).AddMinutes($MinutesAhead).ToUniversalTime().ToString("o")
}

function New-CoordinateAddress {
    param(
        [string]$Type,
        [string]$Line1,
        [string]$Town,
        [string]$ZipCode,
        [double]$Latitude,
        [double]$Longitude,
        [string]$Label,
        [int]$Floor = 0,
        [bool]$HasElevator = $false
    )

    $addressAuto = "$Line1, $ZipCode $Town, France"
    $emailTag = ($Label.ToLowerInvariant() -replace '[^a-z0-9]+', '.').Trim('.')

    return [ordered]@{
        type        = $Type
        firstName   = "Test"
        lastName    = "User"
        floor       = $Floor
        hasElevator = $HasElevator
        dateTime    = if ($Type -eq 'DEPARTURE') { New-FutureIso 90 } else { New-FutureIso 180 }
        email       = "$emailTag@example.com"
        phone       = "0600000000"
        line1       = $Line1
        line2       = ""
        town        = $Town
        zipCode     = $ZipCode
        country     = "France"
        latitude    = [math]::Round($Latitude, 6)
        longitude   = [math]::Round($Longitude, 6)
        addressAuto = $addressAuto
    }
}

function New-OffsetAddress {
    param(
        [hashtable]$Base,
        [string]$Type,
        [string]$Label,
        [int]$Index,
        [double]$LatitudeOffset,
        [double]$LongitudeOffset
    )

    $line1 = "{0} Test {1}" -f (10 + $Index), $Label
    return New-CoordinateAddress `
        -Type $Type `
        -Line1 $line1 `
        -Town $Base.Town `
        -ZipCode $Base.ZipCode `
        -Latitude ($Base.Latitude + $LatitudeOffset) `
        -Longitude ($Base.Longitude + $LongitudeOffset) `
        -Label ("{0}-{1}" -f $Label, $Index)
}

function New-InterpolatedAddress {
    param(
        [hashtable]$Start,
        [hashtable]$End,
        [string]$Type,
        [string]$Label,
        [int]$Index,
        [double]$Progress,
        [double]$LatitudeOffset,
        [double]$LongitudeOffset
    )

    $latitude = $Start.Latitude + (($End.Latitude - $Start.Latitude) * $Progress) + $LatitudeOffset
    $longitude = $Start.Longitude + (($End.Longitude - $Start.Longitude) * $Progress) + $LongitudeOffset

    if ($Progress -lt 0.2) {
        $town = "Limeil-Brevannes"
        $zipCode = "94450"
    } elseif ($Progress -lt 0.4) {
        $town = "Creteil"
        $zipCode = "94000"
    } elseif ($Progress -lt 0.7) {
        $town = "Paris"
        $zipCode = "75012"
    } else {
        $town = "Paris"
        $zipCode = "75018"
    }

    return New-CoordinateAddress `
        -Type $Type `
        -Line1 ("{0} Corridor {1}" -f (100 + $Index), $Label) `
        -Town $town `
        -ZipCode $zipCode `
        -Latitude $latitude `
        -Longitude $longitude `
        -Label ("{0}-{1}" -f $Label, $Index)
}

function New-PackagePayload {
    param(
        [string]$Reference,
        [string]$PackageSizeCategory,
        [double]$WeightKg,
        [string]$DeliverySpeed,
        [bool]$InsuranceSelected,
        [double]$DeclaredValue,
        [hashtable]$Departure,
        [hashtable]$Arrival
    )

    $dimensionsByCategory = @{
        SMALL       = @{ Height = 20; Width = 20; Depth = 20; DefaultWeight = 2 }
        MEDIUM      = @{ Height = 40; Width = 30; Depth = 30; DefaultWeight = 10 }
        LARGE       = @{ Height = 60; Width = 40; Depth = 40; DefaultWeight = 25 }
        EXTRA_LARGE = @{ Height = 80; Width = 60; Depth = 60; DefaultWeight = 50 }
    }

    if (-not $dimensionsByCategory.ContainsKey($PackageSizeCategory)) {
        throw "Unsupported package category: $PackageSizeCategory"
    }

    $dimensions = $dimensionsByCategory[$PackageSizeCategory]

    return [ordered]@{
        reference            = $Reference
        height               = $dimensions.Height
        width                = $dimensions.Width
        depth                = $dimensions.Depth
        weight               = $WeightKg
        packageSizeCategory  = $PackageSizeCategory
        deliverySpeed        = $DeliverySpeed
        insuranceSelected    = $InsuranceSelected
        declaredValue        = if ($InsuranceSelected) { $DeclaredValue } else { $null }
        guestMode            = $true
        status               = "PAYMENTPENDING"
        addresses            = @($Departure, $Arrival)
    }
}

function Invoke-PackageCreate {
    param(
        [hashtable]$Payload,
        [string]$GroupName
    )

    $createResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$($BaseUrl.TrimEnd('/'))/packages/v1/create" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            packageDTO = ($Payload | ConvertTo-Json -Depth 8 -Compress)
            locale     = $Locale
        }

    if (-not $createResponse.id -or -not $createResponse.guestAccessToken) {
        throw "Package creation did not return an id/guestAccessToken for $($Payload.reference)."
    }

    Invoke-RestMethod `
        -Method Put `
        -Uri "$($BaseUrl.TrimEnd('/'))/packages/v1/confirm-guest-payment?packageID=$($createResponse.id)&guestAccessToken=$([uri]::EscapeDataString($createResponse.guestAccessToken))"

    return [ordered]@{
        id                           = $createResponse.id
        reference                    = $createResponse.reference
        group                        = $GroupName
        packageSizeCategory          = $Payload.packageSizeCategory
        weightKg                     = $Payload.weight
        deliverySpeed                = $Payload.deliverySpeed
        insuranceSelected            = $Payload.insuranceSelected
        declaredValue                = $Payload.declaredValue
        deliveryPrice                = $createResponse.deliveryPrice
        customerTotalPrice           = $createResponse.customerTotalPrice
        deliveryBaseAmount           = $createResponse.deliveryBaseAmount
        insuranceFee                 = $createResponse.insuranceFee
        platformServiceFee           = $createResponse.platformServiceFee
        deliveryRevenueExcludingServiceFee = $createResponse.deliveryRevenueExcludingServiceFee
        platformCommissionRate       = $createResponse.platformCommissionRate
        platformCommissionAmount     = $createResponse.platformCommissionAmount
        courierShareRate             = $createResponse.courierShareRate
        courierPayoutAmount          = $createResponse.courierPayoutAmount
        pricingVersion               = $createResponse.pricingVersion
        departure                    = $Payload.addresses[0].addressAuto
        arrival                      = $Payload.addresses[1].addressAuto
    }
}

function Add-SeedCase {
    param(
        [System.Collections.Generic.List[object]]$Target,
        [string]$Group,
        [string]$Reference,
        [string]$Category,
        [double]$WeightKg,
        [string]$DeliverySpeed,
        [bool]$InsuranceSelected,
        [double]$DeclaredValue,
        [hashtable]$Departure,
        [hashtable]$Arrival,
        [string]$Expectation
    )

    $Target.Add([ordered]@{
        group        = $Group
        expectation  = $Expectation
        payload      = (New-PackagePayload `
            -Reference $Reference `
            -PackageSizeCategory $Category `
            -WeightKg $WeightKg `
            -DeliverySpeed $DeliverySpeed `
            -InsuranceSelected $InsuranceSelected `
            -DeclaredValue $DeclaredValue `
            -Departure $Departure `
            -Arrival $Arrival)
    }) | Out-Null
}

function Get-SeedPlan {
    $seedCases = New-Object 'System.Collections.Generic.List[object]'
    $runSuffix = Get-Date -Format "yyyyMMddHHmmss"

    $limeil = @{
        Line1 = "3 Rue Pasteur"
        Town = "Limeil-Brevannes"
        ZipCode = "94450"
        Latitude = 48.74845
        Longitude = 2.48856
    }
    $bernardDimey = @{
        Line1 = "22 Rue Bernard Dimey"
        Town = "Paris"
        ZipCode = "75018"
        Latitude = 48.89510
        Longitude = 2.34360
    }
    $creteil = @{
        Line1 = "2 Avenue du General de Gaulle"
        Town = "Creteil"
        ZipCode = "94000"
        Latitude = 48.78120
        Longitude = 2.45450
    }
    $paris12 = @{
        Line1 = "14 Cours de Vincennes"
        Town = "Paris"
        ZipCode = "75012"
        Latitude = 48.84470
        Longitude = 2.40840
    }
    $chartres = @{
        Line1 = "8 Place Pierre Semard"
        Town = "Chartres"
        ZipCode = "28000"
        Latitude = 48.44720
        Longitude = 1.48910
    }
    $lille = @{
        Line1 = "1 Boulevard de Turin"
        Town = "Lille"
        ZipCode = "59800"
        Latitude = 50.63940
        Longitude = 3.07540
    }
    $marseille = @{
        Line1 = "Square Narvik"
        Town = "Marseille"
        ZipCode = "13001"
        Latitude = 43.30280
        Longitude = 5.38020
    }

    $originAddress = New-CoordinateAddress -Type DEPARTURE -Line1 $limeil.Line1 -Town $limeil.Town -ZipCode $limeil.ZipCode -Latitude $limeil.Latitude -Longitude $limeil.Longitude -Label "origin"
    $directDestinationAddress = New-CoordinateAddress -Type ARRIVAL -Line1 $bernardDimey.Line1 -Town $bernardDimey.Town -ZipCode $bernardDimey.ZipCode -Latitude $bernardDimey.Latitude -Longitude $bernardDimey.Longitude -Label "destination"

    $pricingDestinations = @(
        @{ Key = "band-10"; Address = (New-CoordinateAddress -Type ARRIVAL -Line1 $creteil.Line1 -Town $creteil.Town -ZipCode $creteil.ZipCode -Latitude $creteil.Latitude -Longitude $creteil.Longitude -Label "price-band-10") },
        @{ Key = "band-30"; Address = (New-CoordinateAddress -Type ARRIVAL -Line1 $paris12.Line1 -Town $paris12.Town -ZipCode $paris12.ZipCode -Latitude $paris12.Latitude -Longitude $paris12.Longitude -Label "price-band-30") },
        @{ Key = "band-100"; Address = (New-CoordinateAddress -Type ARRIVAL -Line1 $chartres.Line1 -Town $chartres.Town -ZipCode $chartres.ZipCode -Latitude $chartres.Latitude -Longitude $chartres.Longitude -Label "price-band-100") },
        @{ Key = "band-long"; Address = (New-CoordinateAddress -Type ARRIVAL -Line1 $lille.Line1 -Town $lille.Town -ZipCode $lille.ZipCode -Latitude $lille.Latitude -Longitude $lille.Longitude -Label "price-band-long") },
        @{ Key = "band-long-premium"; Address = (New-CoordinateAddress -Type ARRIVAL -Line1 $marseille.Line1 -Town $marseille.Town -ZipCode $marseille.ZipCode -Latitude $marseille.Latitude -Longitude $marseille.Longitude -Label "price-band-long-premium") }
    )

    $categories = @("SMALL", "MEDIUM", "LARGE", "EXTRA_LARGE")
    for ($index = 0; $index -lt $categories.Count; $index += 1) {
        Add-SeedCase -Target $seedCases -Group "pricing" `
            -Reference ("TEST-PRICE-CATEGORY-{0}-{1}" -f $categories[$index], $runSuffix) `
            -Category $categories[$index] `
            -WeightKg (2 + ($index * 3)) `
            -DeliverySpeed "STANDARD" `
            -InsuranceSelected $false `
            -DeclaredValue 0 `
            -Departure $originAddress `
            -Arrival $pricingDestinations[1].Address `
            -Expectation "Category coverage"
    }

    $weightBands = @(
        @{ Label = "W0-2"; Weight = 2.0 },
        @{ Label = "W2-5"; Weight = 4.0 },
        @{ Label = "W5-10"; Weight = 8.0 },
        @{ Label = "W10-15"; Weight = 12.0 },
        @{ Label = "W15PLUS"; Weight = 18.0 }
    )
    foreach ($weightBand in $weightBands) {
        Add-SeedCase -Target $seedCases -Group "pricing" `
            -Reference ("TEST-PRICE-WEIGHT-{0}-{1}" -f $weightBand.Label, $runSuffix) `
            -Category "MEDIUM" `
            -WeightKg $weightBand.Weight `
            -DeliverySpeed "STANDARD" `
            -InsuranceSelected $false `
            -DeclaredValue 0 `
            -Departure $originAddress `
            -Arrival $pricingDestinations[0].Address `
            -Expectation "Weight band coverage"
    }

    $distanceCases = @(
        @{ Label = "SHORT"; Arrival = $pricingDestinations[0].Address; Speed = "STANDARD"; Insurance = $false; DeclaredValue = 0 },
        @{ Label = "MID"; Arrival = $pricingDestinations[1].Address; Speed = "STANDARD"; Insurance = $false; DeclaredValue = 0 },
        @{ Label = "UPTO100"; Arrival = $pricingDestinations[2].Address; Speed = "STANDARD"; Insurance = $true; DeclaredValue = 50 },
        @{ Label = "LONG"; Arrival = $pricingDestinations[3].Address; Speed = "STANDARD"; Insurance = $true; DeclaredValue = 50 }
    )
    foreach ($distanceCase in $distanceCases) {
        Add-SeedCase -Target $seedCases -Group "pricing" `
            -Reference ("TEST-PRICE-DISTANCE-{0}-{1}" -f $distanceCase.Label, $runSuffix) `
            -Category "MEDIUM" `
            -WeightKg 10 `
            -DeliverySpeed $distanceCase.Speed `
            -InsuranceSelected $distanceCase.Insurance `
            -DeclaredValue $distanceCase.DeclaredValue `
            -Departure $originAddress `
            -Arrival $distanceCase.Arrival `
            -Expectation "Distance regime coverage"
    }

    foreach ($speed in @("STANDARD", "EXPRESS", "SAME_DAY")) {
        Add-SeedCase -Target $seedCases -Group "pricing" `
            -Reference ("TEST-PRICE-MODE-{0}-{1}" -f $speed, $runSuffix) `
            -Category "MEDIUM" `
            -WeightKg 10 `
            -DeliverySpeed $speed `
            -InsuranceSelected $true `
            -DeclaredValue 50 `
            -Departure $originAddress `
            -Arrival $pricingDestinations[4].Address `
            -Expectation "Delivery mode coverage"
    }

    Add-SeedCase -Target $seedCases -Group "pricing" `
        -Reference ("TEST-PRICE-INSURANCE-OFF-{0}" -f $runSuffix) `
        -Category "MEDIUM" `
        -WeightKg 10 `
        -DeliverySpeed "STANDARD" `
        -InsuranceSelected $false `
        -DeclaredValue 0 `
        -Departure $originAddress `
        -Arrival $pricingDestinations[1].Address `
        -Expectation "Insurance disabled"

    Add-SeedCase -Target $seedCases -Group "pricing" `
        -Reference ("TEST-PRICE-INSURANCE-MIN-{0}" -f $runSuffix) `
        -Category "MEDIUM" `
        -WeightKg 10 `
        -DeliverySpeed "STANDARD" `
        -InsuranceSelected $true `
        -DeclaredValue 50 `
        -Departure $originAddress `
        -Arrival $pricingDestinations[1].Address `
        -Expectation "Insurance minimum fee"

    Add-SeedCase -Target $seedCases -Group "pricing" `
        -Reference ("TEST-PRICE-INSURANCE-PROP-{0}" -f $runSuffix) `
        -Category "MEDIUM" `
        -WeightKg 10 `
        -DeliverySpeed "STANDARD" `
        -InsuranceSelected $true `
        -DeclaredValue 500 `
        -Departure $originAddress `
        -Arrival $pricingDestinations[1].Address `
        -Expectation "Insurance proportional fee"

    for ($index = 0; $index -lt 8; $index += 1) {
        $pickup = New-OffsetAddress -Base $limeil -Type DEPARTURE -Label "around-me-pickup" -Index $index -LatitudeOffset (0.002 * (($index % 4) - 1.5)) -LongitudeOffset (0.002 * ([math]::Floor($index / 4) - 0.5))
        $drop = New-OffsetAddress -Base $creteil -Type ARRIVAL -Label "around-me-drop" -Index $index -LatitudeOffset (0.002 * (($index % 4) - 1.5)) -LongitudeOffset (0.002 * ([math]::Floor($index / 4) - 0.5))
        Add-SeedCase -Target $seedCases -Group "around_me" `
            -Reference ("TEST-AROUNDME-{0:D2}-{1}" -f ($index + 1), $runSuffix) `
            -Category $categories[$index % $categories.Count] `
            -WeightKg (2 + $index) `
            -DeliverySpeed "STANDARD" `
            -InsuranceSelected $false `
            -DeclaredValue 0 `
            -Departure $pickup `
            -Arrival $drop `
            -Expectation "Visible on around me"
    }

    for ($index = 0; $index -lt 30; $index += 1) {
        $pickup = New-OffsetAddress -Base $limeil -Type DEPARTURE -Label "direct-pickup" -Index $index -LatitudeOffset (0.0014 * (($index % 5) - 2)) -LongitudeOffset (0.0014 * ([math]::Floor($index / 5) - 2.5))
        $drop = New-OffsetAddress -Base $bernardDimey -Type ARRIVAL -Label "direct-drop" -Index $index -LatitudeOffset (0.0012 * (($index % 5) - 2)) -LongitudeOffset (0.0012 * ([math]::Floor($index / 5) - 2.5))
        $directInsuranceSelected = (($index % 2) -eq 0)
        $directDeclaredValue = if ($directInsuranceSelected) { 100 } else { 0 }
        Add-SeedCase -Target $seedCases -Group "direct" `
            -Reference ("TEST-DIRECT-{0:D2}-{1}" -f ($index + 1), $runSuffix) `
            -Category $categories[$index % $categories.Count] `
            -WeightKg (2 + ($index % 5) * 2) `
            -DeliverySpeed @("STANDARD", "EXPRESS", "SAME_DAY")[$index % 3] `
            -InsuranceSelected $directInsuranceSelected `
            -DeclaredValue $directDeclaredValue `
            -Departure $pickup `
            -Arrival $drop `
            -Expectation "Visible on direct mode and capped by vehicle capacity"
    }

    for ($index = 0; $index -lt 30; $index += 1) {
        $progressPickup = 0.05 + ($index * 0.02)
        $progressDrop = [math]::Min(0.95, $progressPickup + 0.12)
        $pickup = New-InterpolatedAddress -Start $limeil -End $bernardDimey -Type DEPARTURE -Label "tournee-pickup" -Index $index -Progress $progressPickup -LatitudeOffset (0.0012 * (($index % 3) - 1)) -LongitudeOffset (0.0009 * (($index % 4) - 1.5))
        $drop = New-InterpolatedAddress -Start $limeil -End $bernardDimey -Type ARRIVAL -Label "tournee-drop" -Index $index -Progress $progressDrop -LatitudeOffset (0.0010 * (($index % 4) - 1.5)) -LongitudeOffset (0.0008 * (($index % 3) - 1))
        $tourneeInsuranceSelected = (($index % 3) -eq 0)
        $tourneeDeclaredValue = if ($tourneeInsuranceSelected) { 150 } else { 0 }
        Add-SeedCase -Target $seedCases -Group "tournee" `
            -Reference ("TEST-TOURNEE-{0:D2}-{1}" -f ($index + 1), $runSuffix) `
            -Category $categories[$index % $categories.Count] `
            -WeightKg (2 + ($index % 4) * 3) `
            -DeliverySpeed "STANDARD" `
            -InsuranceSelected $tourneeInsuranceSelected `
            -DeclaredValue $tourneeDeclaredValue `
            -Departure $pickup `
            -Arrival $drop `
            -Expectation "Visible on personal route search"
    }

    for ($index = 0; $index -lt 6; $index += 1) {
        $pickup = New-CoordinateAddress -Type DEPARTURE -Line1 ("{0} Place de la Gare" -f ($index + 1)) -Town "Lille" -ZipCode "59800" -Latitude (50.6394 + ($index * 0.001)) -Longitude (3.0754 + ($index * 0.001)) -Label ("noise-pickup-{0}" -f $index)
        $drop = New-CoordinateAddress -Type ARRIVAL -Line1 ("{0} Place Bellecour" -f ($index + 1)) -Town "Lyon" -ZipCode "69002" -Latitude (45.7578 + ($index * 0.001)) -Longitude (4.8320 + ($index * 0.001)) -Label ("noise-drop-{0}" -f $index)
        Add-SeedCase -Target $seedCases -Group "noise" `
            -Reference ("TEST-NOISE-{0:D2}-{1}" -f ($index + 1), $runSuffix) `
            -Category "MEDIUM" `
            -WeightKg 10 `
            -DeliverySpeed "STANDARD" `
            -InsuranceSelected $false `
            -DeclaredValue 0 `
            -Departure $pickup `
            -Arrival $drop `
            -Expectation "Should not appear on homepage searches around Limeil/Paris"
    }

    return @{
        RunSuffix  = $runSuffix
        SeedCases  = $seedCases
        DriverOrigin = $originAddress.addressAuto
        DirectDestination = $directDestinationAddress.addressAuto
    }
}

function Write-SeedPlanSummary {
    param([hashtable]$Plan)

    $summary = $Plan.SeedCases | Group-Object { $_.group } | Sort-Object Name | ForEach-Object {
        [ordered]@{
            group = $_.Name
            count = $_.Count
        }
    }

    Write-Host "Seed plan summary:"
    $summary | ForEach-Object {
        Write-Host (" - {0}: {1}" -f $_.group, $_.count)
    }
    Write-Host ("Driver origin: {0}" -f $Plan.DriverOrigin)
    Write-Host ("Direct/Tournee destination: {0}" -f $Plan.DirectDestination)
}

$repoRoot = Resolve-RepoRoot
$vm5Env = Load-KeyValueFile -Path (Join-Path $repoRoot 'deploy\lightsail\vm5-mysql.env')
$script:MysqlRootPassword = $vm5Env['MYSQL_ROOT_PASSWORD']
$quickDeliveryDb = if ($vm5Env.ContainsKey('QUICKDELIVERY_DB_NAME')) { $vm5Env['QUICKDELIVERY_DB_NAME'] } elseif ($vm5Env.ContainsKey('MYSQL_DATABASE')) { $vm5Env['MYSQL_DATABASE'] } else { '' }

if (-not $script:MysqlRootPassword) {
    throw "MYSQL_ROOT_PASSWORD not found in deploy/lightsail/vm5-mysql.env"
}
if (-not $quickDeliveryDb) {
    throw "MYSQL_DATABASE not found in deploy/lightsail/vm5-mysql.env"
}

$seedPlan = Get-SeedPlan
Write-SeedPlanSummary -Plan $seedPlan

$executeResetAndSeed = Require-ExecutionConfirmation
if (-not $executeResetAndSeed) {
    return
}

$summarySql = @'
SELECT 'package', COUNT(*) FROM {0}.package
UNION ALL
SELECT 'address', COUNT(*) FROM {0}.address
UNION ALL
SELECT 'package_reservation', COUNT(*) FROM {0}.package_reservation;
'@ -f $quickDeliveryDb

Write-Host "Current package-related row counts:"
Invoke-RemoteMysql -Sql $summarySql

$cleanupSql = @'
SET FOREIGN_KEY_CHECKS=0;
USE {0};
CREATE TEMPORARY TABLE qd_all_package_ids AS SELECT id FROM {0}.package;

DELETE FROM {0}.courier_payout WHERE package_id IN (SELECT id FROM qd_all_package_ids);
DELETE FROM {0}.package_settlement WHERE package_id IN (SELECT id FROM qd_all_package_ids);
DELETE FROM {0}.package_reservation WHERE package_id IN (SELECT id FROM qd_all_package_ids);
DELETE FROM {0}.document WHERE package_id IN (SELECT id FROM qd_all_package_ids);
DELETE FROM {0}.address WHERE package_id IN (SELECT id FROM qd_all_package_ids);
DELETE FROM {0}.package WHERE id IN (SELECT id FROM qd_all_package_ids);

SET FOREIGN_KEY_CHECKS=1;
'@ -f $quickDeliveryDb

Write-Host "Deleting all packages, addresses, reservations and package-linked rows..."
Invoke-RemoteMysql -Sql $cleanupSql

$createdPackages = New-Object 'System.Collections.Generic.List[object]'

foreach ($seedCase in $seedPlan.SeedCases) {
    Write-Host ("Creating {0} [{1}]" -f $seedCase.payload.reference, $seedCase.group)
    $created = Invoke-PackageCreate -Payload $seedCase.payload -GroupName $seedCase.group
    $createdWithExpectation = [ordered]@{
        id                  = $created.id
        reference           = $created.reference
        group               = $created.group
        expectation         = $seedCase.expectation
        packageSizeCategory = $created.packageSizeCategory
        weightKg            = $created.weightKg
        deliverySpeed       = $created.deliverySpeed
        insuranceSelected   = $created.insuranceSelected
        declaredValue       = $created.declaredValue
        deliveryPrice       = $created.deliveryPrice
        customerTotalPrice  = $created.customerTotalPrice
        deliveryBaseAmount  = $created.deliveryBaseAmount
        insuranceFee        = $created.insuranceFee
        platformServiceFee  = $created.platformServiceFee
        deliveryRevenueExcludingServiceFee = $created.deliveryRevenueExcludingServiceFee
        platformCommissionRate = $created.platformCommissionRate
        platformCommissionAmount = $created.platformCommissionAmount
        courierShareRate    = $created.courierShareRate
        courierPayoutAmount = $created.courierPayoutAmount
        pricingVersion      = $created.pricingVersion
        departure           = $created.departure
        arrival             = $created.arrival
    }
    $createdPackages.Add($createdWithExpectation) | Out-Null
}

$reportDir = Join-Path $repoRoot 'perf\generated'
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
}

$reportPath = Join-Path $reportDir ("homepage-seed-report-{0}.json" -f $seedPlan.RunSuffix)
$reportPayload = [ordered]@{
    createdAt = (Get-Date).ToUniversalTime().ToString("o")
    baseUrl = $BaseUrl
    driverOrigin = $seedPlan.DriverOrigin
    directDestination = $seedPlan.DirectDestination
    counts = ($createdPackages | Group-Object { $_.group } | Sort-Object Name | ForEach-Object {
        [ordered]@{
            group = $_.Name
            count = $_.Count
        }
    })
    packages = $createdPackages
}

$reportPayload | ConvertTo-Json -Depth 8 | Set-Content -Path $reportPath -Encoding UTF8

Write-Host "Reset and seed completed."
Write-Host ("Report written to {0}" -f $reportPath)
