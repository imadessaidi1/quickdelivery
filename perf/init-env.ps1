param(
    [string]$EnvFile = "",
    [string]$BaseUrl = "",
    [string]$AuthBaseUrl = "",
    [string]$AdminBearerToken = "",
    [string]$AdminUsername = "",
    [string]$AdminPassword = "",
    [string]$CourierBearerToken = "",
    [string]$CourierUsername = "",
    [string]$CourierPassword = "",
    [string]$CourierEmail = "",
    [string]$OAuthClientId = "",
    [string]$OAuthClientSecret = "",
    [string]$OAuthRealm = ""
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

function Resolve-EnvFilePath {
    param(
        [string]$RequestedFile,
        [string]$PerfRoot
    )

    if ($RequestedFile) {
        return (Resolve-Path $RequestedFile).Path
    }

    $defaultPath = Join-Path $PerfRoot ".env.local"
    if (Test-Path $defaultPath) {
        return (Resolve-Path $defaultPath).Path
    }

    return $defaultPath
}

function Read-EnvFile {
    param([string]$Path)

    $values = [ordered]@{}
    if (-not (Test-Path $Path)) {
        return $values
    }

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
        $value = $line.Substring($separatorIndex + 1)
        $values[$name] = $value
    }

    return $values
}

function Write-EnvFile {
    param(
        [string]$Path,
        [hashtable]$Values
    )

    $lines = @()
    foreach ($entry in $Values.GetEnumerator()) {
        $lines += "$($entry.Key)=$($entry.Value)"
    }
    $directory = Split-Path -Parent $Path
    if ($directory -and -not (Test-Path $directory)) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }
    $tempPath = "$Path.tmp"
    Set-Content -Path $tempPath -Value $lines -Encoding UTF8
    Move-Item -Path $tempPath -Destination $Path -Force
}

function Decode-JwtPayload {
    param([string]$Token)

    if (-not $Token) {
        return $null
    }

    try {
        $parts = $Token.Split(".")
        if ($parts.Length -lt 2) {
            return $null
        }

        $payload = $parts[1].Replace("-", "+").Replace("_", "/")
        switch ($payload.Length % 4) {
            2 { $payload += "==" }
            3 { $payload += "=" }
        }

        $json = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload))
        return $json | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Invoke-JsonGet {
    param(
        [string]$Url,
        [string]$Token
    )

    $headers = @{
        Authorization = "Bearer $Token"
    }

    return Invoke-RestMethod -Method Get -Uri $Url -Headers $headers
}

function Prompt-IfEmpty {
    param(
        [string]$Value,
        [string]$Prompt
    )

    if ($Value) {
        return $Value
    }

    return Read-Host -Prompt $Prompt
}

function Resolve-AuthBaseUrl {
    param(
        [string]$RequestedAuthBaseUrl,
        [hashtable]$EnvValues,
        [string]$ResolvedBaseUrl
    )

    if ($RequestedAuthBaseUrl) {
        return $RequestedAuthBaseUrl.TrimEnd("/")
    }

    if ($EnvValues["AUTH_BASE_URL"]) {
        return $EnvValues["AUTH_BASE_URL"].TrimEnd("/")
    }

    if ($ResolvedBaseUrl -match '^https://api\.quickdelivery\.fr/?$') {
        return "https://auth.quickdelivery.fr/auth"
    }

    return "https://localhost:18443/auth"
}

function Request-AccessToken {
    param(
        [string]$AuthUrl,
        [string]$Realm,
        [string]$ClientId,
        [string]$ClientSecret,
        [string]$Username,
        [string]$Password
    )

    if (-not $Username -or -not $Password) {
        return ""
    }

    $tokenEndpoint = "$AuthUrl/realms/$Realm/protocol/openid-connect/token"
    $form = @{
        grant_type = "password"
        client_id = $ClientId
        username = $Username
        password = $Password
        scope = "email profile"
    }

    if ($ClientSecret) {
        $form["client_secret"] = $ClientSecret
    }

    try {
        $response = Invoke-RestMethod -Method Post -Uri $tokenEndpoint -ContentType "application/x-www-form-urlencoded" -Body $form
        if ($response.access_token) {
            return [string]$response.access_token
        }
    } catch {
        if ($Username -cne $Username.ToLowerInvariant()) {
            $form["username"] = $Username.ToLowerInvariant()
            $response = Invoke-RestMethod -Method Post -Uri $tokenEndpoint -ContentType "application/x-www-form-urlencoded" -Body $form
            if ($response.access_token) {
                return [string]$response.access_token
            }
        }
        throw
    }

    return ""
}

Enter-PerfEnvMutex
try {
    $perfRoot = Resolve-PerfRoot
    $envPath = Resolve-EnvFilePath -RequestedFile $EnvFile -PerfRoot $perfRoot
    $envValues = Read-EnvFile -Path $envPath

    $baseUrl = if ($BaseUrl) { $BaseUrl } elseif ($envValues["BASE_URL"]) { $envValues["BASE_URL"] } else { "https://api.quickdelivery.fr" }
    $authBaseUrl = Resolve-AuthBaseUrl -RequestedAuthBaseUrl $AuthBaseUrl -EnvValues $envValues -ResolvedBaseUrl $baseUrl
    $wsUrl = if ($envValues["WS_URL"]) { $envValues["WS_URL"] } else { ($baseUrl -replace '^http', 'ws') + "/ws" }
    $defaultLocale = if ($envValues["DEFAULT_LOCALE"]) { $envValues["DEFAULT_LOCALE"] } else { "fr" }
    $insecureSkipTls = if ($envValues["INSECURE_SKIP_TLS_VERIFY"]) { $envValues["INSECURE_SKIP_TLS_VERIFY"] } else { "false" }
    $oauthClientId = if ($OAuthClientId) { $OAuthClientId } elseif ($envValues["OAUTH_CLIENT_ID"]) { $envValues["OAUTH_CLIENT_ID"] } else { "quickdelivery-postman" }
    $oauthClientSecret = if ($OAuthClientSecret) { $OAuthClientSecret } elseif ($envValues["OAUTH_CLIENT_SECRET"]) { $envValues["OAUTH_CLIENT_SECRET"] } else { "" }
    $oauthRealm = if ($OAuthRealm) { $OAuthRealm } elseif ($envValues["OAUTH_REALM"]) { $envValues["OAUTH_REALM"] } else { "quickdelivery" }

    $forceAdminLogin = ($AdminUsername -and $AdminPassword)
    $forceCourierLogin = ($CourierUsername -and $CourierPassword)

    $adminToken = if ($AdminBearerToken) {
        $AdminBearerToken
    } elseif (-not $forceAdminLogin) {
        $envValues["ADMIN_BEARER_TOKEN"]
    } else {
        ""
    }

    $courierToken = if ($CourierBearerToken) {
        $CourierBearerToken
    } elseif (-not $forceCourierLogin) {
        $envValues["COURIER_BEARER_TOKEN"]
    } else {
        ""
    }

    if (-not $adminToken) {
        $resolvedAdminUsername = if ($AdminUsername) { $AdminUsername } elseif ($envValues["ADMIN_USERNAME"]) { $envValues["ADMIN_USERNAME"] } else { "" }
        $resolvedAdminPassword = if ($AdminPassword) { $AdminPassword } elseif ($envValues["ADMIN_PASSWORD"]) { $envValues["ADMIN_PASSWORD"] } else { "" }
        $resolvedAdminUsername = Prompt-IfEmpty -Value $resolvedAdminUsername -Prompt "Admin username"
        $resolvedAdminPassword = Prompt-IfEmpty -Value $resolvedAdminPassword -Prompt "Admin password"
        $adminToken = Request-AccessToken -AuthUrl $authBaseUrl -Realm $oauthRealm -ClientId $oauthClientId -ClientSecret $oauthClientSecret -Username $resolvedAdminUsername -Password $resolvedAdminPassword
    }

    if (-not $courierToken) {
        $resolvedCourierUsername = if ($CourierUsername) { $CourierUsername } elseif ($envValues["COURIER_USERNAME"]) { $envValues["COURIER_USERNAME"] } else { "" }
        $resolvedCourierPassword = if ($CourierPassword) { $CourierPassword } elseif ($envValues["COURIER_PASSWORD"]) { $envValues["COURIER_PASSWORD"] } else { "" }
        $resolvedCourierUsername = Prompt-IfEmpty -Value $resolvedCourierUsername -Prompt "Courier username"
        $resolvedCourierPassword = Prompt-IfEmpty -Value $resolvedCourierPassword -Prompt "Courier password"
        $courierToken = Request-AccessToken -AuthUrl $authBaseUrl -Realm $oauthRealm -ClientId $oauthClientId -ClientSecret $oauthClientSecret -Username $resolvedCourierUsername -Password $resolvedCourierPassword
    }

    if (-not $adminToken) {
        $adminToken = Prompt-IfEmpty -Value $adminToken -Prompt "Admin access token"
    }

    if (-not $courierToken) {
        $courierToken = Prompt-IfEmpty -Value $courierToken -Prompt "Courier access token"
    }

    $courierJwt = Decode-JwtPayload -Token $courierToken
    $courierEmail = $CourierEmail
    if ($courierJwt) {
        if (-not $courierEmail) {
            $courierEmail = [string]$courierJwt.email
        }
    }
    if (-not $courierEmail) {
        $courierEmail = Read-Host -Prompt "Courier email"
    }
    if (-not $courierEmail) {
        throw "Unable to resolve courier email. Provide a valid courier token or enter the courier email when prompted."
    }

    Write-Host "Resolving courier by email: $courierEmail"
    $courierUser = Invoke-JsonGet -Url "$baseUrl/users/v1/userByEmail?email=$([uri]::EscapeDataString($courierEmail))" -Token $adminToken
    $courierId = [string]$courierUser.id

    Write-Host "Fetching first NEW package for courier lifecycle"
    $newPackages = Invoke-JsonGet -Url "$baseUrl/packages/v1/package-by-status?status=NEW" -Token $adminToken
    $courierPackageId = ""
    if ($newPackages -and $newPackages.Count -gt 0) {
        $courierPackageId = [string]$newPackages[0].id
    }

    Write-Host "Fetching packages already attached to courier for tracking"
    $courierPackages = Invoke-JsonGet -Url "$baseUrl/packages/v1/getPackagesByDeliveryPerson?deliveryPersonID=$courierId" -Token $adminToken
    $trackingPackageReference = ""
    if ($courierPackages.PICKEDUP -and $courierPackages.PICKEDUP.Count -gt 0) {
        $trackingPackageReference = [string]$courierPackages.PICKEDUP[0].reference
    } elseif ($courierPackages.INDELIVERY -and $courierPackages.INDELIVERY.Count -gt 0) {
        $trackingPackageReference = [string]$courierPackages.INDELIVERY[0].reference
    }

    $orderedValues = [ordered]@{
    BASE_URL = $baseUrl
    AUTH_BASE_URL = $authBaseUrl
    WS_URL = $wsUrl
    DEFAULT_LOCALE = $defaultLocale
    INSECURE_SKIP_TLS_VERIFY = $insecureSkipTls
    OAUTH_CLIENT_ID = $oauthClientId
    OAUTH_CLIENT_SECRET = $oauthClientSecret
    OAUTH_REALM = $oauthRealm
    ADMIN_USERNAME = $(if ($AdminUsername) { $AdminUsername } elseif ($envValues["ADMIN_USERNAME"]) { $envValues["ADMIN_USERNAME"] } else { "" })
    ADMIN_PASSWORD = $(if ($AdminPassword) { $AdminPassword } elseif ($envValues["ADMIN_PASSWORD"]) { $envValues["ADMIN_PASSWORD"] } else { "" })
    ADMIN_BEARER_TOKEN = $adminToken
    COURIER_USERNAME = $(if ($CourierUsername) { $CourierUsername } elseif ($envValues["COURIER_USERNAME"]) { $envValues["COURIER_USERNAME"] } else { "" })
    COURIER_PASSWORD = $(if ($CourierPassword) { $CourierPassword } elseif ($envValues["COURIER_PASSWORD"]) { $envValues["COURIER_PASSWORD"] } else { "" })
    COURIER_BEARER_TOKEN = $courierToken
    COURIER_ID = $courierId
    COURIER_PACKAGE_ID = $courierPackageId
    COURIER_PICKUP_OTP = ""
    COURIER_DELIVERY_OTP = ""
    TRACKING_BEARER_TOKEN = $courierToken
    TRACKING_DELIVERY_PERSON_ID = $courierId
    TRACKING_PACKAGE_REFERENCE = $trackingPackageReference
    TRACKING_GUEST_ACCESS_TOKEN = ""
    GUEST_ESTIMATE_P95_MS = $(if ($envValues["GUEST_ESTIMATE_P95_MS"]) { $envValues["GUEST_ESTIMATE_P95_MS"] } else { "800" })
    GUEST_CREATE_P95_MS = $(if ($envValues["GUEST_CREATE_P95_MS"]) { $envValues["GUEST_CREATE_P95_MS"] } else { "2500" })
    GUEST_PAYMENT_P95_MS = $(if ($envValues["GUEST_PAYMENT_P95_MS"]) { $envValues["GUEST_PAYMENT_P95_MS"] } else { "1500" })
    GUEST_CONSULT_P95_MS = $(if ($envValues["GUEST_CONSULT_P95_MS"]) { $envValues["GUEST_CONSULT_P95_MS"] } else { "800" })
    ADMIN_DASHBOARD_P95_MS = $(if ($envValues["ADMIN_DASHBOARD_P95_MS"]) { $envValues["ADMIN_DASHBOARD_P95_MS"] } else { "2000" })
    TRACKING_HTTP_P95_MS = $(if ($envValues["TRACKING_HTTP_P95_MS"]) { $envValues["TRACKING_HTTP_P95_MS"] } else { "1200" })
    TRACKING_PROPAGATION_P95_MS = $(if ($envValues["TRACKING_PROPAGATION_P95_MS"]) { $envValues["TRACKING_PROPAGATION_P95_MS"] } else { "2000" })
    }

    Write-EnvFile -Path $envPath -Values $orderedValues

    Write-Host ""
    Write-Host "perf env initialized: $envPath"
    Write-Host "COURIER_ID=$courierId"
    Write-Host "COURIER_PACKAGE_ID=$courierPackageId"
    Write-Host "TRACKING_PACKAGE_REFERENCE=$trackingPackageReference"
    if (-not $courierPackageId) {
        Write-Warning "No NEW package found for the courier lifecycle scenario."
    }
    if (-not $trackingPackageReference) {
        Write-Warning "No PICKEDUP package found for the tracking scenario."
    }
}
finally {
    Exit-PerfEnvMutex
}
