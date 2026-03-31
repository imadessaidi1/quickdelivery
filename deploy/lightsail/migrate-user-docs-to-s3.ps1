param(
    [string]$Vm3Ip = "15.224.39.34",
    [string]$Vm4Ip = "13.36.9.2",
    [string]$SshUser = "ubuntu",
    [string]$SshKeyPath = "C:\Users\imess\.ssh\lightsail.pem",
    [string]$EnvFile = "deploy/lightsail/vm3-app.env"
)

$ErrorActionPreference = "Stop"

function Load-EnvFile {
    param([string]$Path)
    $values = @{}
    Get-Content $Path | ForEach-Object {
        if ([string]::IsNullOrWhiteSpace($_) -or $_.Trim().StartsWith("#")) {
            return
        }
        $parts = $_ -split "=", 2
        if ($parts.Count -eq 2) {
            $values[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
    return $values
}

function Fetch-UsersDocsTar {
    param(
        [string]$HostIp,
        [string]$DestinationTar,
        [string]$SshUser,
        [string]$SshKeyPath
    )

    $sshCommand = "ssh -o StrictHostKeyChecking=no -i `"$SshKeyPath`" $SshUser@$HostIp sudo docker exec quickdelivery-users-service tar -C /data -cf - users-docs > `"$DestinationTar`""
    cmd /c $sshCommand | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to export users-docs tar from $HostIp"
    }
}

$envValues = Load-EnvFile -Path $EnvFile
$bucket = $envValues["AWS_S3_BUCKET"]
$region = $envValues["AWS_REGION"]
$accessKey = $envValues["AWS_ACCESS_KEY_ID"]
$secretKey = $envValues["AWS_SECRET_ACCESS_KEY"]

if ([string]::IsNullOrWhiteSpace($bucket) -or [string]::IsNullOrWhiteSpace($region) -or
    [string]::IsNullOrWhiteSpace($accessKey) -or [string]::IsNullOrWhiteSpace($secretKey)) {
    throw "AWS S3 settings are missing in $EnvFile"
}

$tempRoot = Join-Path $env:TEMP ("qd-users-docs-migration-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

try {
    $vm3Tar = Join-Path $tempRoot "vm3-users-docs.tar"
    $vm4Tar = Join-Path $tempRoot "vm4-users-docs.tar"
    $vm3Extract = Join-Path $tempRoot "vm3"
    $vm4Extract = Join-Path $tempRoot "vm4"
    New-Item -ItemType Directory -Force -Path $vm3Extract, $vm4Extract | Out-Null

    Fetch-UsersDocsTar -HostIp $Vm3Ip -DestinationTar $vm3Tar -SshUser $SshUser -SshKeyPath $SshKeyPath
    Fetch-UsersDocsTar -HostIp $Vm4Ip -DestinationTar $vm4Tar -SshUser $SshUser -SshKeyPath $SshKeyPath

    tar -xf $vm3Tar -C $vm3Extract
    tar -xf $vm4Tar -C $vm4Extract

    $pythonScript = @'
import os
from pathlib import Path

import boto3

bucket = os.environ["QD_S3_BUCKET"]
region = os.environ["QD_AWS_REGION"]
access_key = os.environ["QD_AWS_ACCESS_KEY_ID"]
secret_key = os.environ["QD_AWS_SECRET_ACCESS_KEY"]
root = Path(os.environ["QD_MIGRATION_ROOT"])

s3 = boto3.client(
    "s3",
    region_name=region,
    aws_access_key_id=access_key,
    aws_secret_access_key=secret_key,
)

uploaded = 0
for file_path in sorted(root.rglob("*")):
    if not file_path.is_file():
        continue
    parts = list(file_path.parts)
    if "users-docs" not in parts:
        continue
    users_docs_index = parts.index("users-docs")
    key = "/".join(["data"] + parts[users_docs_index:])
    s3.upload_file(str(file_path), bucket, key)
    uploaded += 1

print(uploaded)
'@

    $env:QD_S3_BUCKET = $bucket
    $env:QD_AWS_REGION = $region
    $env:QD_AWS_ACCESS_KEY_ID = $accessKey
    $env:QD_AWS_SECRET_ACCESS_KEY = $secretKey
    $env:QD_MIGRATION_ROOT = $tempRoot

    python -m pip install --user boto3 | Out-Null
    $uploaded = $pythonScript | python -
    Write-Host "Uploaded $uploaded user document files to s3://$bucket"
}
finally {
    if (Test-Path $tempRoot) {
        Remove-Item -Recurse -Force $tempRoot
    }
}
