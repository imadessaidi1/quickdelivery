param(
    [string]$SshKeyPath = "C:\Users\imess\.ssh\lightsail.pem",
    [string]$MysqlVmPublicIp = "15.188.208.12",
    [string]$MysqlSshUser = "ubuntu",
    [string]$ProtectedLogin = "admin.test",
    [string]$ProtectedEmail = "admin.test@quickdelivery.local",
    [string]$BucketName = "",
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
    param([string]$Sql)

    $localTempFile = [System.IO.Path]::GetTempFileName()
    $remoteTempFile = "/tmp/qd-business-purge-$([guid]::NewGuid().ToString('N')).sql"
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
        Write-Host "Dry-run only. No data will be deleted."
        Write-Host "To execute: .\perf\purge-business-data-keep-admin.ps1 -Execute -ConfirmationPhrase PURGE_QUICKDELIVERY_ALL_DATA_KEEP_ADMIN"
        return $false
    }

    if ($ConfirmationPhrase -ne 'PURGE_QUICKDELIVERY_ALL_DATA_KEEP_ADMIN') {
        throw "Refusing to execute purge: invalid confirmation phrase."
    }

    return $true
}

function Remove-S3Prefix {
    param(
        [string]$Bucket,
        [string]$Prefix,
        [hashtable]$AwsEnv
    )

    $pythonCommand = Get-Command python -ErrorAction SilentlyContinue
    if (-not $pythonCommand) {
        throw "Python is required for S3 cleanup because AWS CLI is not available."
    }

    if (-not $AwsEnv['AWS_ACCESS_KEY_ID'] -or -not $AwsEnv['AWS_SECRET_ACCESS_KEY'] -or -not $AwsEnv['AWS_REGION']) {
        throw "AWS settings are missing in deploy/lightsail/vm3-app.env"
    }

    $cleanupPython = @'
import boto3
import os

bucket = os.environ["QD_BUCKET"]
prefix = os.environ["QD_PREFIX"]

s3 = boto3.client(
    "s3",
    region_name=os.environ["QD_AWS_REGION"],
    aws_access_key_id=os.environ["QD_AWS_ACCESS_KEY_ID"],
    aws_secret_access_key=os.environ["QD_AWS_SECRET_ACCESS_KEY"],
)

deleted = 0
continuation = None
while True:
    kwargs = {"Bucket": bucket, "Prefix": prefix}
    if continuation:
        kwargs["ContinuationToken"] = continuation
    response = s3.list_objects_v2(**kwargs)
    objects = [{"Key": item["Key"]} for item in response.get("Contents", [])]
    if objects:
        s3.delete_objects(Bucket=bucket, Delete={"Objects": objects})
        deleted += len(objects)
    if not response.get("IsTruncated"):
        break
    continuation = response.get("NextContinuationToken")

print(f"{prefix}\t{deleted}")
'@

    $env:QD_BUCKET = $Bucket
    $env:QD_PREFIX = $Prefix
    $env:QD_AWS_REGION = $AwsEnv['AWS_REGION']
    $env:QD_AWS_ACCESS_KEY_ID = $AwsEnv['AWS_ACCESS_KEY_ID']
    $env:QD_AWS_SECRET_ACCESS_KEY = $AwsEnv['AWS_SECRET_ACCESS_KEY']
    try {
        $cleanupPython | & $pythonCommand.Source -
    } finally {
        Remove-Item Env:QD_BUCKET, Env:QD_PREFIX, Env:QD_AWS_REGION, Env:QD_AWS_ACCESS_KEY_ID, Env:QD_AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
    }
}

$repoRoot = Resolve-RepoRoot
$vm5Env = Load-KeyValueFile -Path (Join-Path $repoRoot 'deploy\lightsail\vm5-mysql.env')
$vm3Env = Load-KeyValueFile -Path (Join-Path $repoRoot 'deploy\lightsail\vm3-app.env')

$script:MysqlRootPassword = $vm5Env['MYSQL_ROOT_PASSWORD']
$quickDeliveryDb = if ($vm5Env.ContainsKey('QUICKDELIVERY_DB_NAME')) { $vm5Env['QUICKDELIVERY_DB_NAME'] } elseif ($vm5Env.ContainsKey('MYSQL_DATABASE')) { $vm5Env['MYSQL_DATABASE'] } else { '' }
$keycloakDb = if ($vm5Env.ContainsKey('KEYCLOAK_DB_NAME')) { $vm5Env['KEYCLOAK_DB_NAME'] } elseif ($vm5Env.ContainsKey('KEYCLOAK_DATABASE')) { $vm5Env['KEYCLOAK_DATABASE'] } else { 'KeycloakDB' }
$bucket = if ($BucketName) { $BucketName } elseif ($vm3Env.ContainsKey('AWS_S3_BUCKET')) { $vm3Env['AWS_S3_BUCKET'] } else { '' }

if (-not $script:MysqlRootPassword) {
    throw "MYSQL_ROOT_PASSWORD not found in deploy/lightsail/vm5-mysql.env"
}
if (-not $quickDeliveryDb) {
    throw "MYSQL_DATABASE not found in deploy/lightsail/vm5-mysql.env"
}

$protectedLoginSql = $ProtectedLogin.ToLowerInvariant().Replace("'", "''")
$protectedEmailSql = $ProtectedEmail.ToLowerInvariant().Replace("'", "''")

$summarySql = @"
SELECT 'quickdelivery_keep_users', COUNT(*) FROM $quickDeliveryDb.``user``
WHERE LOWER(COALESCE(email_address, '')) IN ('$protectedLoginSql', '$protectedEmailSql')
UNION ALL
SELECT 'quickdelivery_delete_users', COUNT(*) FROM $quickDeliveryDb.``user``
WHERE NOT (
    LOWER(COALESCE(email_address, '')) IN ('$protectedLoginSql', '$protectedEmailSql')
)
UNION ALL
SELECT 'quickdelivery_packages', COUNT(*) FROM $quickDeliveryDb.``package``
UNION ALL
SELECT 'keycloak_keep_users', COUNT(*) FROM $keycloakDb.USER_ENTITY
WHERE LOWER(COALESCE(USERNAME, '')) = '$protectedLoginSql'
   OR LOWER(COALESCE(EMAIL, '')) IN ('$protectedLoginSql', '$protectedEmailSql')
UNION ALL
SELECT 'keycloak_delete_users', COUNT(*) FROM $keycloakDb.USER_ENTITY
WHERE NOT (
    LOWER(COALESCE(USERNAME, '')) = '$protectedLoginSql'
    OR LOWER(COALESCE(EMAIL, '')) IN ('$protectedLoginSql', '$protectedEmailSql')
);
"@

Write-Host "Purge summary before execution:"
Invoke-RemoteMysql -Sql $summarySql

$executePurge = Require-ExecutionConfirmation
if (-not $executePurge) {
    return
}

$quickDeliveryCleanupSql = @"
SET FOREIGN_KEY_CHECKS=0;
USE $quickDeliveryDb;

-- List of tables to truncate (all except user and sequences related to user if needed)
-- We will delete from all tables, and handle user specially.

TRUNCATE TABLE courier_payout;
TRUNCATE TABLE courier_penalty;
TRUNCATE TABLE delivery_route_stop;
TRUNCATE TABLE delivery_route;
TRUNCATE TABLE package_settlement;
TRUNCATE TABLE package_reservation;
TRUNCATE TABLE document;
TRUNCATE TABLE mobile_device;
TRUNCATE TABLE notification;
TRUNCATE TABLE address;
TRUNCATE TABLE package;
TRUNCATE TABLE vehicle;
TRUNCATE TABLE payment;
TRUNCATE TABLE user_onboarding;

-- Delete all users except admin.test
DELETE FROM ``user`` WHERE NOT (LOWER(COALESCE(email_address, '')) IN ('$protectedLoginSql', '$protectedEmailSql'));

-- Reset sequences if possible (optional but cleaner)
-- ALTER TABLE address_seq AUTO_INCREMENT = 1; -- and others if they were using sequences tables instead of native auto_increment

SET FOREIGN_KEY_CHECKS=1;
"@

$keycloakCleanupSql = @"
SET FOREIGN_KEY_CHECKS=0;
USE $keycloakDb;

CREATE TEMPORARY TABLE qd_keep_keycloak_user_ids AS
SELECT ID FROM USER_ENTITY
WHERE LOWER(COALESCE(USERNAME, '')) = '$protectedLoginSql'
   OR LOWER(COALESCE(EMAIL, '')) IN ('$protectedLoginSql', '$protectedEmailSql');

CREATE TEMPORARY TABLE qd_delete_keycloak_user_ids AS
SELECT ID FROM USER_ENTITY WHERE ID NOT IN (SELECT ID FROM qd_keep_keycloak_user_ids);

DELETE FROM USER_ROLE_MAPPING WHERE USER_ID IN (SELECT ID FROM qd_delete_keycloak_user_ids);
DELETE FROM CREDENTIAL WHERE USER_ID IN (SELECT ID FROM qd_delete_keycloak_user_ids);
DELETE FROM USER_ATTRIBUTE WHERE USER_ID IN (SELECT ID FROM qd_delete_keycloak_user_ids);
DELETE FROM USER_REQUIRED_ACTION WHERE USER_ID IN (SELECT ID FROM qd_delete_keycloak_user_ids);
DELETE FROM FEDERATED_IDENTITY WHERE USER_ID IN (SELECT ID FROM qd_delete_keycloak_user_ids);
DELETE FROM USER_GROUP_MEMBERSHIP WHERE USER_ID IN (SELECT ID FROM qd_delete_keycloak_user_ids);
DELETE FROM USER_CONSENT_CLIENT_SCOPE WHERE USER_CONSENT_ID IN (SELECT ID FROM USER_CONSENT WHERE USER_ID IN (SELECT ID FROM qd_delete_keycloak_user_ids));
DELETE FROM USER_CONSENT WHERE USER_ID IN (SELECT ID FROM qd_delete_keycloak_user_ids);
DELETE FROM USER_ENTITY WHERE ID IN (SELECT ID FROM qd_delete_keycloak_user_ids);

SET FOREIGN_KEY_CHECKS=1;
"@

Write-Host "Emptying QuickDeliveryDB (keeping only $ProtectedLogin)..."
Invoke-RemoteMysql -Sql $quickDeliveryCleanupSql

Write-Host "Cleaning KeycloakDB (keeping only $ProtectedLogin)..."
Invoke-RemoteMysql -Sql $keycloakCleanupSql

if ($bucket) {
    Write-Host "Deleting S3 document prefixes..."
    Remove-S3Prefix -Bucket $bucket -Prefix "data/users-docs/" -AwsEnv $vm3Env
    Remove-S3Prefix -Bucket $bucket -Prefix "data/package-docs/" -AwsEnv $vm3Env
}

Write-Host "Purge completed. Final summary:"
Invoke-RemoteMysql -Sql $summarySql
