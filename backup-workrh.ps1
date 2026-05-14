param(
    [string]$OutputDirectory = $env:WORKRH_BACKUP_DIR,
    [string]$ComposeFile
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ComposeFile)) {
    $ComposeFile = Join-Path $PSScriptRoot 'infra\docker\docker-compose.yml'
}

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $PSScriptRoot 'backups'
}

if (-not (Test-Path $ComposeFile)) {
    throw "Compose file not found: $ComposeFile"
}

$dockerCompose = Get-Command docker-compose -ErrorAction SilentlyContinue
$dockerArgs = if ($dockerCompose) {
    @('docker-compose', '-f', $ComposeFile)
} else {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $docker) {
        throw 'Neither docker-compose nor docker is available in PATH.'
    }
    @('docker', 'compose', '-f', $ComposeFile)
}
$dockerCommand = $dockerArgs[0]
$dockerBaseArgs = $dockerArgs[1..($dockerArgs.Length - 1)]

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupRoot = Join-Path $OutputDirectory $timestamp
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null

$databases = @(
    @{ Service = 'postgres-users'; Database = 'workrh_users' },
    @{ Service = 'postgres-leaves'; Database = 'workrh_leaves' },
    @{ Service = 'postgres-sickness'; Database = 'workrh_sickness' },
    @{ Service = 'postgres-telework'; Database = 'workrh_telework' },
    @{ Service = 'postgres-notifications'; Database = 'workrh_notifications' },
    @{ Service = 'postgres-reporting'; Database = 'workrh_reporting' },
    @{ Service = 'postgres-subscriptions'; Database = 'workrh_subscriptions' }
)

foreach ($database in $databases) {
    $target = Join-Path $backupRoot ($database.Database + '.dump')
    Write-Host ("Backing up {0}..." -f $database.Database)
    $containerId = & $dockerCommand @dockerBaseArgs ps -q $database.Service
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "Container is not running for service $($database.Service)"
    }
    $containerDumpPath = "/tmp/$($database.Database).dump"
    & $dockerCommand @dockerBaseArgs exec -T $database.Service pg_dump -U workrh -Fc -f $containerDumpPath -d $database.Database
    docker cp "${containerId}:$containerDumpPath" $target
    & $dockerCommand @dockerBaseArgs exec -T $database.Service rm -f $containerDumpPath
    if (-not (Test-Path $target) -or (Get-Item $target).Length -eq 0) {
        throw "Backup failed for $($database.Database)"
    }
}

$manifest = [pscustomobject]@{
    createdAt = (Get-Date).ToUniversalTime().ToString('o')
    composeFile = $ComposeFile
    databases = $databases | ForEach-Object { $_.Database }
}
$manifest | ConvertTo-Json -Depth 3 | Set-Content -Encoding UTF8 -Path (Join-Path $backupRoot 'manifest.json')

Write-Host "Backup complete: $backupRoot"
