param(
    [Parameter(Mandatory = $true)]
    [string]$BackupDirectory,

    [string]$ComposeFile,

    [string]$ConfirmRestore
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ComposeFile)) {
    $ComposeFile = Join-Path $PSScriptRoot 'infra\docker\docker-compose.yml'
}

if ($ConfirmRestore -ne 'RESTORE_WORKRH_DATABASES') {
    throw "Restore is destructive. Re-run with -ConfirmRestore RESTORE_WORKRH_DATABASES."
}

if (-not (Test-Path $BackupDirectory)) {
    throw "Backup directory not found: $BackupDirectory"
}

if (-not (Test-Path (Join-Path $BackupDirectory 'manifest.json'))) {
    throw "Missing manifest.json in backup directory: $BackupDirectory"
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
    $source = Join-Path $BackupDirectory ($database.Database + '.dump')
    if (-not (Test-Path $source)) {
        throw "Missing dump file: $source"
    }

    Write-Host ("Restoring {0}..." -f $database.Database)
    $containerId = & $dockerCommand @dockerBaseArgs ps -q $database.Service
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "Container is not running for service $($database.Service)"
    }

    $containerDumpPath = "/tmp/$($database.Database).dump"
    docker cp $source "${containerId}:$containerDumpPath"
    & $dockerCommand @dockerBaseArgs exec -T $database.Service psql -U workrh -d $database.Database -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
    & $dockerCommand @dockerBaseArgs exec -T $database.Service pg_restore -U workrh -d $database.Database --clean --if-exists $containerDumpPath
    & $dockerCommand @dockerBaseArgs exec -T $database.Service rm -f $containerDumpPath
}

Write-Host "Restore complete from: $BackupDirectory"
