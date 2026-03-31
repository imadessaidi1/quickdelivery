param(
    [string]$SshKeyPath = "C:\Users\imess\.ssh\lightsail.pem",
    [string]$MysqlVmPublicIp = "15.188.208.12",
    [string]$MysqlSshUser = "ubuntu",
    [string]$BucketName = "",
    [string[]]$ProtectedEmails = @("admin.test@quickdelivery.local", "im.essaidi@gmail.com", "nezha.kemrach@gmail.com"),
    [switch]$Execute,
    [string]$ConfirmationPhrase = ""
)

$ErrorActionPreference = "Stop"

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
    $remoteTempFile = "/tmp/qd-perf-cleanup-$([guid]::NewGuid().ToString('N')).sql"
    try {
        Set-Content -Path $localTempFile -Value $Sql -Encoding UTF8 -NoNewline
        & scp -i $SshKeyPath $localTempFile "${MysqlSshUser}@${MysqlVmPublicIp}:$remoteTempFile" | Out-Null
        $command = "sudo docker exec -i quickdelivery-mysql mysql -u root -p`"$script:MysqlRootPassword`" -N -B < $remoteTempFile; rm -f $remoteTempFile"
        & ssh -i $SshKeyPath "$MysqlSshUser@$MysqlVmPublicIp" $command
    } finally {
        Remove-Item -Path $localTempFile -Force -ErrorAction SilentlyContinue
    }
}

function Require-ExecutionConfirmation {
    if (-not $Execute) {
        Write-Host "Dry-run only. No data will be deleted."
        Write-Host "To execute the cleanup, rerun with -Execute -ConfirmationPhrase DELETE_QUICKDELIVERY_PERF_DATA"
        return $false
    }

    if ($ConfirmationPhrase -ne 'DELETE_QUICKDELIVERY_PERF_DATA') {
        throw "Refusing to execute cleanup: invalid confirmation phrase."
    }

    return $true
}

function Build-ProtectedEmailSqlClause {
    param([string[]]$Emails)

    $normalized = @($Emails | Where-Object { $_ -and $_.Trim() } | ForEach-Object { $_.Trim().ToLowerInvariant() } | Select-Object -Unique)
    if ($normalized.Count -eq 0) {
        return "''"
    }

    return ($normalized | ForEach-Object { "'{0}'" -f ($_.Replace("'", "''")) }) -join ', '
}

$repoRoot = Resolve-RepoRoot
$vm5Env = Load-KeyValueFile -Path (Join-Path $repoRoot 'deploy\lightsail\vm5-mysql.env')
$vm3Env = Load-KeyValueFile -Path (Join-Path $repoRoot 'deploy\lightsail\vm3-app.env')

$script:MysqlRootPassword = $vm5Env['MYSQL_ROOT_PASSWORD']
$quickDeliveryDb = if ($vm5Env.ContainsKey('QUICKDELIVERY_DB_NAME')) { $vm5Env['QUICKDELIVERY_DB_NAME'] } elseif ($vm5Env.ContainsKey('MYSQL_DATABASE')) { $vm5Env['MYSQL_DATABASE'] } else { '' }
$keycloakDb = if ($vm5Env.ContainsKey('KEYCLOAK_DB_NAME')) { $vm5Env['KEYCLOAK_DB_NAME'] } elseif ($vm5Env.ContainsKey('KEYCLOAK_DATABASE')) { $vm5Env['KEYCLOAK_DATABASE'] } else { 'KeycloakDB' }
$bucket = if ($BucketName) { $BucketName } elseif ($vm3Env.ContainsKey('AWS_S3_BUCKET')) { $vm3Env['AWS_S3_BUCKET'] } else { '' }
$protectedEmailClause = Build-ProtectedEmailSqlClause -Emails $ProtectedEmails

if (-not $script:MysqlRootPassword) {
    throw "MYSQL_ROOT_PASSWORD not found in deploy/lightsail/vm5-mysql.env"
}
if (-not $quickDeliveryDb) {
    throw "MYSQL_DATABASE not found in deploy/lightsail/vm5-mysql.env"
}

$summarySql = (@'
SELECT 'quickdelivery_users', COUNT(*) FROM {0}.`user`
WHERE email_address LIKE 'perf.%@example.com'
  AND LOWER(email_address) NOT IN ({3})
UNION ALL
SELECT 'quickdelivery_packages', COUNT(*) FROM {0}.`package` p
WHERE p.reference LIKE 'PERF-%'
   OR p.sender_id IN (
       SELECT id
       FROM {0}.`user`
       WHERE email_address LIKE 'perf.%@example.com'
         AND LOWER(email_address) NOT IN ({3})
   )
   OR EXISTS (
       SELECT 1 FROM {0}.`address` a
       WHERE a.package_id = p.id
         AND (a.email LIKE 'perf.%@example.com' OR a.first_name = 'Perf')
   )
UNION ALL
SELECT 'quickdelivery_documents', COUNT(*) FROM {0}.`document` WHERE docurl LIKE '%perf.%@example.com%' OR docurl LIKE '%PERF-%' OR docurl LIKE 's3://{1}/data/users-docs/perf.%' OR docurl LIKE 's3://{1}/data/package-docs/PERF-%'
UNION ALL
SELECT 'keycloak_users', COUNT(*) FROM {2}.USER_ENTITY
WHERE (EMAIL LIKE 'perf.%@example.com' OR USERNAME LIKE 'perf.%@example.com')
  AND LOWER(COALESCE(EMAIL, USERNAME)) NOT IN ({3});
'@) -f $quickDeliveryDb, $bucket, $keycloakDb, $protectedEmailClause

Write-Host "Cleanup summary for perf data:"
Invoke-RemoteMysql -Sql $summarySql

$executeCleanup = Require-ExecutionConfirmation
if (-not $executeCleanup) {
    return
}

$quickDeliveryCleanupSql = (@'
SET FOREIGN_KEY_CHECKS=0;
USE {0};
CREATE TEMPORARY TABLE perf_package_ids AS
SELECT p.id
FROM {0}.`package` p
WHERE p.reference LIKE 'PERF-%'
   OR p.sender_id IN (
       SELECT id
       FROM {0}.`user`
       WHERE email_address LIKE 'perf.%@example.com'
         AND LOWER(email_address) NOT IN ({1})
   )
   OR EXISTS (
       SELECT 1
       FROM {0}.`address` a
       WHERE a.package_id = p.id
         AND (a.email LIKE 'perf.%@example.com' OR a.first_name = 'Perf')
   );
CREATE TEMPORARY TABLE perf_user_ids AS
SELECT id
FROM {0}.`user`
WHERE email_address LIKE 'perf.%@example.com'
  AND LOWER(email_address) NOT IN ({1});
CREATE TEMPORARY TABLE perf_vehicle_ids AS SELECT id FROM {0}.vehicle WHERE user_id IN (SELECT id FROM perf_user_ids);

DELETE FROM {0}.courier_payout WHERE package_id IN (SELECT id FROM perf_package_ids) OR delivery_person_id IN (SELECT id FROM perf_user_ids);
DELETE FROM {0}.package_settlement WHERE package_id IN (SELECT id FROM perf_package_ids);
DELETE FROM {0}.package_reservation WHERE package_id IN (SELECT id FROM perf_package_ids) OR delivery_person_id IN (SELECT id FROM perf_user_ids);
DELETE FROM {0}.`document` WHERE package_id IN (SELECT id FROM perf_package_ids);
DELETE FROM {0}.`address` WHERE package_id IN (SELECT id FROM perf_package_ids);
DELETE FROM {0}.`package` WHERE id IN (SELECT id FROM perf_package_ids) OR sender_id IN (SELECT id FROM perf_user_ids);

DELETE FROM {0}.`document` WHERE vehicle_id IN (SELECT id FROM perf_vehicle_ids) OR user_id IN (SELECT id FROM perf_user_ids);
DELETE FROM {0}.vehicle WHERE id IN (SELECT id FROM perf_vehicle_ids);
DELETE FROM {0}.payment WHERE holder_in_app_id IN (SELECT id FROM perf_user_ids);
DELETE FROM {0}.`address` WHERE residents_user_id IN (SELECT id FROM perf_user_ids);
DELETE FROM {0}.`user` WHERE id IN (SELECT id FROM perf_user_ids);
SET FOREIGN_KEY_CHECKS=1;
'@) -f $quickDeliveryDb, $protectedEmailClause

$keycloakCleanupSql = @"
SET FOREIGN_KEY_CHECKS=0;
USE $keycloakDb;
CREATE TEMPORARY TABLE perf_keycloak_user_ids AS
SELECT ID
FROM $keycloakDb.USER_ENTITY
WHERE (EMAIL LIKE 'perf.%@example.com' OR USERNAME LIKE 'perf.%@example.com')
  AND LOWER(COALESCE(EMAIL, USERNAME)) NOT IN ($protectedEmailClause);

DELETE FROM $keycloakDb.USER_ROLE_MAPPING WHERE USER_ID IN (SELECT ID FROM perf_keycloak_user_ids);
DELETE FROM $keycloakDb.CREDENTIAL WHERE USER_ID IN (SELECT ID FROM perf_keycloak_user_ids);
DELETE FROM $keycloakDb.USER_ATTRIBUTE WHERE USER_ID IN (SELECT ID FROM perf_keycloak_user_ids);
DELETE FROM $keycloakDb.USER_REQUIRED_ACTION WHERE USER_ID IN (SELECT ID FROM perf_keycloak_user_ids);
DELETE FROM $keycloakDb.FEDERATED_IDENTITY WHERE USER_ID IN (SELECT ID FROM perf_keycloak_user_ids);
DELETE FROM $keycloakDb.USER_GROUP_MEMBERSHIP WHERE USER_ID IN (SELECT ID FROM perf_keycloak_user_ids);
DELETE FROM $keycloakDb.USER_CONSENT_CLIENT_SCOPE WHERE USER_CONSENT_ID IN (SELECT ID FROM $keycloakDb.USER_CONSENT WHERE USER_ID IN (SELECT ID FROM perf_keycloak_user_ids));
DELETE FROM $keycloakDb.USER_CONSENT WHERE USER_ID IN (SELECT ID FROM perf_keycloak_user_ids);
DELETE FROM $keycloakDb.USER_ENTITY WHERE ID IN (SELECT ID FROM perf_keycloak_user_ids);
SET FOREIGN_KEY_CHECKS=1;
"@

Write-Host "Deleting perf users/packages from MySQL..."
Invoke-RemoteMysql -Sql $quickDeliveryCleanupSql
Invoke-RemoteMysql -Sql $keycloakCleanupSql

if ($bucket) {
    $awsCommand = Get-Command aws -ErrorAction SilentlyContinue
    if ($awsCommand) {
        & $awsCommand.Source s3 rm "s3://$bucket/data/users-docs/" --recursive --exclude "*" --include "perf.*"
        & $awsCommand.Source s3 rm "s3://$bucket/data/package-docs/" --recursive --exclude "*" --include "PERF-*"
    } else {
        $pythonCommand = Get-Command python -ErrorAction SilentlyContinue
        if ($pythonCommand -and $vm3Env['AWS_ACCESS_KEY_ID'] -and $vm3Env['AWS_SECRET_ACCESS_KEY'] -and $vm3Env['AWS_REGION']) {
            $cleanupPython = @'
import boto3
import os

bucket = os.environ["QD_BUCKET"]
region = os.environ["QD_AWS_REGION"]
access_key = os.environ["QD_AWS_ACCESS_KEY_ID"]
secret_key = os.environ["QD_AWS_SECRET_ACCESS_KEY"]

s3 = boto3.client(
    "s3",
    region_name=region,
    aws_access_key_id=access_key,
    aws_secret_access_key=secret_key,
)

prefixes = ["data/users-docs/perf.", "data/package-docs/PERF-"]
deleted = 0
for prefix in prefixes:
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

print(f"s3-deleted={deleted}")
'@
            $env:QD_BUCKET = $bucket
            $env:QD_AWS_REGION = $vm3Env['AWS_REGION']
            $env:QD_AWS_ACCESS_KEY_ID = $vm3Env['AWS_ACCESS_KEY_ID']
            $env:QD_AWS_SECRET_ACCESS_KEY = $vm3Env['AWS_SECRET_ACCESS_KEY']
            $cleanupPython | & $pythonCommand.Source -
            Remove-Item Env:QD_BUCKET, Env:QD_AWS_REGION, Env:QD_AWS_ACCESS_KEY_ID, Env:QD_AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
        } else {
            Write-Warning "AWS CLI not found locally. MySQL cleanup completed, but S3 objects were not deleted."
        }
    }
}

Write-Host "Perf data cleanup executed."
