param(
    [string]$BaseUrl = "https://api.quickdelivery.fr",
    [string]$Locale = "fr",
    [string]$SshKeyPath = "C:\Users\imess\.ssh\lightsail.pem",
    [string]$MysqlVmPublicIp = "15.188.208.12",
    [string]$MysqlSshUser = "ubuntu",
    [ValidateSet("RealRouteOnly", "HomepageFull")]
    [string]$SeedDataset = "RealRouteOnly",
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

function New-RadiusAddress {
    param(
        [hashtable]$Base,
        [string]$Type,
        [string]$Label,
        [int]$Index,
        [double]$RadiusKm,
        [double]$BearingDegrees
    )

    $bearingRadians = $BearingDegrees * [math]::PI / 180
    $latitudeOffsetKm = $RadiusKm * [math]::Cos($bearingRadians)
    $longitudeOffsetKm = $RadiusKm * [math]::Sin($bearingRadians)
    $latitudeOffset = $latitudeOffsetKm / 110.574
    $longitudeOffset = $longitudeOffsetKm / (111.320 * [math]::Cos($Base.Latitude * [math]::PI / 180))

    return New-CoordinateAddress `
        -Type $Type `
        -Line1 ("{0} Seed {1}" -f (10 + $Index), $Label) `
        -Town $Base.Town `
        -ZipCode $Base.ZipCode `
        -Latitude ($Base.Latitude + $latitudeOffset) `
        -Longitude ($Base.Longitude + $longitudeOffset) `
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

    if ($Progress -lt 0.25) {
        $town = "Limeil-Brevannes"
        $zipCode = "94450"
    } elseif ($Progress -lt 0.55) {
        $town = "Valenton"
        $zipCode = "94460"
    } elseif ($Progress -lt 0.75) {
        $town = "Villeneuve-Saint-Georges"
        $zipCode = "94190"
    } else {
        $town = "Orly"
        $zipCode = "94310"
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

function New-RealSeedAddress {
    param(
        [string]$Type,
        [hashtable]$Address,
        [string]$Label
    )

    return New-CoordinateAddress `
        -Type $Type `
        -Line1 $Address.Line1 `
        -Town $Address.Town `
        -ZipCode $Address.ZipCode `
        -Latitude $Address.Latitude `
        -Longitude $Address.Longitude `
        -Label $Label
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
    param(
        [ValidateSet("RealRouteOnly", "HomepageFull")]
        [string]$SeedDataset = "RealRouteOnly"
    )

    $seedCases = New-Object 'System.Collections.Generic.List[object]'
    $runSuffix = Get-Date -Format "yyyyMMddHHmmss"

    $limeil = @{
        Line1 = "3 Rue Pasteur"
        Town = "Limeil-Brevannes"
        ZipCode = "94450"
        Latitude = 48.744134
        Longitude = 2.474711
    }
    $orlyAnatoleFrance = @{
        Line1 = "2 Rue Anatole France"
        Town = "Orly"
        ZipCode = "94310"
        Latitude = 48.744035
        Longitude = 2.403361
    }
    $originAddress = New-CoordinateAddress -Type DEPARTURE -Line1 $limeil.Line1 -Town $limeil.Town -ZipCode $limeil.ZipCode -Latitude $limeil.Latitude -Longitude $limeil.Longitude -Label "origin"
    $directDestinationAddress = New-CoordinateAddress -Type ARRIVAL -Line1 $orlyAnatoleFrance.Line1 -Town $orlyAnatoleFrance.Town -ZipCode $orlyAnatoleFrance.ZipCode -Latitude $orlyAnatoleFrance.Latitude -Longitude $orlyAnatoleFrance.Longitude -Label "destination"

    $realLocalRouteCases = @(
        @{
            Pickup = @{ Line1 = "3 Rue Pasteur"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.744134; Longitude = 2.474711 }
            Drop   = @{ Line1 = "2 Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744035; Longitude = 2.403361 }
        },
        @{
            Pickup = @{ Line1 = "1 Place de l'Eglise"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.743971; Longitude = 2.474735 }
            Drop   = @{ Line1 = "2bis Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744196; Longitude = 2.403544 }
        },
        @{
            Pickup = @{ Line1 = "6 Rue Pasteur"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.744049; Longitude = 2.474403 }
            Drop   = @{ Line1 = "4 Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744216; Longitude = 2.403238 }
        },
        @{
            Pickup = @{ Line1 = "8 Rue Pasteur"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.744206; Longitude = 2.475071 }
            Drop   = @{ Line1 = "10 Rue du Verger"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744053; Longitude = 2.402943 }
        },
        @{
            Pickup = @{ Line1 = "2 Place de l'Eglise"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.743803; Longitude = 2.47473 }
            Drop   = @{ Line1 = "33j Avenue Adrien Raynal"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744024; Longitude = 2.403809 }
        },
        @{
            Pickup = @{ Line1 = "10 Rue Pasteur"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.74428; Longitude = 2.475289 }
            Drop   = @{ Line1 = "6 Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744317; Longitude = 2.403169 }
        },
        @{
            Pickup = @{ Line1 = "4 Rue Pasteur"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.744021; Longitude = 2.474125 }
            Drop   = @{ Line1 = "29 Avenue Adrien Raynal"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.743713; Longitude = 2.403714 }
        },
        @{
            Pickup = @{ Line1 = "6 Ruelle de l'Eglise"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.743712; Longitude = 2.474922 }
            Drop   = @{ Line1 = "31 Avenue Adrien Raynal"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.743753; Longitude = 2.403843 }
        },
        @{
            Pickup = @{ Line1 = "5 Place des Tilleuls"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.744421; Longitude = 2.474151 }
            Drop   = @{ Line1 = "4 Rue du Verger"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.743676; Longitude = 2.403014 }
        },
        @{
            Pickup = @{ Line1 = "7 Ruelle de Paris"; Town = "Limeil-Brevannes"; ZipCode = "94450"; Latitude = 48.744557; Longitude = 2.474324 }
            Drop   = @{ Line1 = "8 Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744424; Longitude = 2.403095 }
        }
    )

    $realCorridorRouteCases = @(
        @{
            Pickup = @{ Line1 = "38 Rue Louise Michel"; Town = "Valenton"; ZipCode = "94460"; Latitude = 48.744378; Longitude = 2.465898 }
            Drop   = @{ Line1 = "2 Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744035; Longitude = 2.403361 }
        },
        @{
            Pickup = @{ Line1 = "5 Rue Louis Pergaud"; Town = "Valenton"; ZipCode = "94460"; Latitude = 48.744594; Longitude = 2.459759 }
            Drop   = @{ Line1 = "2bis Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744196; Longitude = 2.403544 }
        },
        @{
            Pickup = @{ Line1 = "236 Avenue du General Leclerc"; Town = "Valenton"; ZipCode = "94460"; Latitude = 48.744256; Longitude = 2.459454 }
            Drop   = @{ Line1 = "4 Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744216; Longitude = 2.403238 }
        },
        @{
            Pickup = @{ Line1 = "87 Avenue Anatole France"; Town = "Villeneuve-Saint-Georges"; ZipCode = "94190"; Latitude = 48.744485; Longitude = 2.452224 }
            Drop   = @{ Line1 = "10 Rue du Verger"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744053; Longitude = 2.402943 }
        },
        @{
            Pickup = @{ Line1 = "60 Rue Danton"; Town = "Villeneuve-Saint-Georges"; ZipCode = "94190"; Latitude = 48.744231; Longitude = 2.45183 }
            Drop   = @{ Line1 = "33j Avenue Adrien Raynal"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744024; Longitude = 2.403809 }
        },
        @{
            Pickup = @{ Line1 = "264 Rue de Paris"; Town = "Villeneuve-Saint-Georges"; ZipCode = "94190"; Latitude = 48.744486; Longitude = 2.446706 }
            Drop   = @{ Line1 = "6 Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744317; Longitude = 2.403169 }
        },
        @{
            Pickup = @{ Line1 = "6 Rue du Tgv"; Town = "Villeneuve-Saint-Georges"; ZipCode = "94190"; Latitude = 48.744841; Longitude = 2.438911 }
            Drop   = @{ Line1 = "29 Avenue Adrien Raynal"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.743713; Longitude = 2.403714 }
        },
        @{
            Pickup = @{ Line1 = "5 Avenue du Front de Seine"; Town = "Villeneuve-le-Roi"; ZipCode = "94290"; Latitude = 48.743102; Longitude = 2.43685 }
            Drop   = @{ Line1 = "31 Avenue Adrien Raynal"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.743753; Longitude = 2.403843 }
        },
        @{
            Pickup = @{ Line1 = "12 Avenue du Marechal de Turenne"; Town = "Villeneuve-le-Roi"; ZipCode = "94290"; Latitude = 48.743251; Longitude = 2.431649 }
            Drop   = @{ Line1 = "4 Rue du Verger"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.743676; Longitude = 2.403014 }
        },
        @{
            Pickup = @{ Line1 = "3 Rue Edmond Rostand"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744256; Longitude = 2.404113 }
            Drop   = @{ Line1 = "8 Rue Anatole France"; Town = "Orly"; ZipCode = "94310"; Latitude = 48.744424; Longitude = 2.403095 }
        }
    )

    $categories = @("SMALL", "MEDIUM", "LARGE", "EXTRA_LARGE")
    for ($index = 0; $index -lt $realLocalRouteCases.Count; $index += 1) {
        $case = $realLocalRouteCases[$index]
        Add-SeedCase -Target $seedCases -Group "real_limeil_orly" `
            -Reference ("TEST-REAL-LIMEIL-ORLY-{0:D2}-{1}" -f ($index + 1), $runSuffix) `
            -Category $categories[$index % $categories.Count] `
            -WeightKg (2 + ($index % 5) * 2) `
            -DeliverySpeed @("STANDARD", "EXPRESS", "SAME_DAY")[$index % 3] `
            -InsuranceSelected (($index % 2) -eq 0) `
            -DeclaredValue $(if (($index % 2) -eq 0) { 100 } else { 0 }) `
            -Departure (New-RealSeedAddress -Type DEPARTURE -Address $case.Pickup -Label ("real-local-pickup-{0}" -f $index)) `
            -Arrival (New-RealSeedAddress -Type ARRIVAL -Address $case.Drop -Label ("real-local-drop-{0}" -f $index)) `
            -Expectation "Real addresses around 3 Rue Pasteur to test route ordering toward 2 Rue Anatole France"
    }

    for ($index = 0; $index -lt $realCorridorRouteCases.Count; $index += 1) {
        $case = $realCorridorRouteCases[$index]
        Add-SeedCase -Target $seedCases -Group "real_corridor" `
            -Reference ("TEST-REAL-CORRIDOR-{0:D2}-{1}" -f ($index + 1), $runSuffix) `
            -Category $categories[$index % $categories.Count] `
            -WeightKg (3 + ($index % 4) * 3) `
            -DeliverySpeed "STANDARD" `
            -InsuranceSelected (($index % 3) -eq 0) `
            -DeclaredValue $(if (($index % 3) -eq 0) { 150 } else { 0 }) `
            -Departure (New-RealSeedAddress -Type DEPARTURE -Address $case.Pickup -Label ("real-corridor-pickup-{0}" -f $index)) `
            -Arrival (New-RealSeedAddress -Type ARRIVAL -Address $case.Drop -Label ("real-corridor-drop-{0}" -f $index)) `
            -Expectation "Real addresses on the Limeil-Brevannes to Orly corridor for on-my-way routing"
    }

    if ($SeedDataset -eq "RealRouteOnly") {
        return @{
            RunSuffix  = $runSuffix
            SeedDataset = $SeedDataset
            SeedCases  = $seedCases
            DriverOrigin = $originAddress.addressAuto
            DirectDestination = $directDestinationAddress.addressAuto
        }
    }

    $pricingDestinations = @(
        @{ Key = "corridor-25"; Address = (New-InterpolatedAddress -Start $limeil -End $orlyAnatoleFrance -Type ARRIVAL -Label "price-corridor" -Index 1 -Progress 0.25 -LatitudeOffset 0 -LongitudeOffset 0) },
        @{ Key = "corridor-45"; Address = (New-InterpolatedAddress -Start $limeil -End $orlyAnatoleFrance -Type ARRIVAL -Label "price-corridor" -Index 2 -Progress 0.45 -LatitudeOffset 0 -LongitudeOffset 0) },
        @{ Key = "corridor-65"; Address = (New-InterpolatedAddress -Start $limeil -End $orlyAnatoleFrance -Type ARRIVAL -Label "price-corridor" -Index 3 -Progress 0.65 -LatitudeOffset 0 -LongitudeOffset 0) },
        @{ Key = "corridor-85"; Address = (New-InterpolatedAddress -Start $limeil -End $orlyAnatoleFrance -Type ARRIVAL -Label "price-corridor" -Index 4 -Progress 0.85 -LatitudeOffset 0 -LongitudeOffset 0) },
        @{ Key = "orly-anatole"; Address = $directDestinationAddress }
    )

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
        $pickup = New-RadiusAddress -Base $limeil -Type DEPARTURE -Label "around-me-pickup" -Index $index -RadiusKm (0.4 + (($index % 6) * 0.4)) -BearingDegrees (($index * 47) % 360)
        $drop = New-RadiusAddress -Base $orlyAnatoleFrance -Type ARRIVAL -Label "around-me-drop" -Index $index -RadiusKm (0.3 + (($index % 5) * 0.3)) -BearingDegrees ((180 + ($index * 53)) % 360)
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
        $pickup = New-RadiusAddress -Base $limeil -Type DEPARTURE -Label "direct-pickup" -Index $index -RadiusKm (0.5 + (($index % 6) * 0.42)) -BearingDegrees (($index * 31) % 360)
        $drop = New-RadiusAddress -Base $orlyAnatoleFrance -Type ARRIVAL -Label "direct-drop" -Index $index -RadiusKm (0.25 + (($index % 7) * 0.25)) -BearingDegrees ((90 + ($index * 29)) % 360)
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
        $progressPickup = 0.06 + ($index * 0.025)
        $progressDrop = [math]::Min(0.98, $progressPickup + 0.14)
        $pickup = New-InterpolatedAddress -Start $limeil -End $orlyAnatoleFrance -Type DEPARTURE -Label "tournee-pickup" -Index $index -Progress $progressPickup -LatitudeOffset (0.0010 * (($index % 3) - 1)) -LongitudeOffset (0.0008 * (($index % 4) - 1.5))
        $drop = New-InterpolatedAddress -Start $limeil -End $orlyAnatoleFrance -Type ARRIVAL -Label "tournee-drop" -Index $index -Progress $progressDrop -LatitudeOffset (0.0009 * (($index % 4) - 1.5)) -LongitudeOffset (0.0007 * (($index % 3) - 1))
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
            -Expectation "Visible on personal route search between Limeil and Orly"
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
            -Expectation "Should not appear on homepage searches around Limeil/Orly"
    }

    return @{
        RunSuffix  = $runSuffix
        SeedDataset = $SeedDataset
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
    Write-Host ("Dataset: {0}" -f $Plan.SeedDataset)
    $summary | ForEach-Object {
        Write-Host (" - {0}: {1}" -f $_.group, $_.count)
    }
    Write-Host ("Driver origin: {0}" -f $Plan.DriverOrigin)
    Write-Host ("Direct/Tournee destination: {0}" -f $Plan.DirectDestination)
}

function Assert-SeedPlanMatchesDataset {
    param([hashtable]$Plan)

    if ($Plan.SeedDataset -ne "RealRouteOnly") {
        return
    }

    $allowedGroups = @("real_limeil_orly", "real_corridor")
    $unexpectedGroups = $Plan.SeedCases |
        Where-Object { $allowedGroups -notcontains $_.group } |
        Select-Object -ExpandProperty group -Unique

    if ($unexpectedGroups.Count -gt 0) {
        throw ("RealRouteOnly seed contains unexpected groups: {0}" -f ($unexpectedGroups -join ", "))
    }

    if ($Plan.SeedCases.Count -ne 20) {
        throw ("RealRouteOnly seed must create exactly 20 packages, found {0}." -f $Plan.SeedCases.Count)
    }

    $groupCounts = @{}
    $Plan.SeedCases | Group-Object { $_.group } | ForEach-Object {
        $groupCounts[$_.Name] = $_.Count
    }

    foreach ($groupName in $allowedGroups) {
        if (-not $groupCounts.ContainsKey($groupName) -or $groupCounts[$groupName] -ne 10) {
            throw ("RealRouteOnly seed must create exactly 10 packages for {0}." -f $groupName)
        }
    }
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

$seedPlan = Get-SeedPlan -SeedDataset $SeedDataset
Write-SeedPlanSummary -Plan $seedPlan
Assert-SeedPlanMatchesDataset -Plan $seedPlan

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
    seedDataset = $seedPlan.SeedDataset
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
