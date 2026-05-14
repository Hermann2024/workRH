$ErrorActionPreference = 'Stop'

function Read-EnvFile {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path $Path)) {
        return $values
    }

    Get-Content $Path | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') {
            return
        }

        $parts = $_ -split '=', 2
        if ($parts.Count -eq 2) {
            $values[$parts[0].Trim()] = $parts[1].Trim()
        }
    }

    return $values
}

function Has-Value {
    param([hashtable]$EnvValues, [string]$Name)

    return $EnvValues.ContainsKey($Name) -and -not [string]::IsNullOrWhiteSpace($EnvValues[$Name])
}

$envValues = Read-EnvFile (Join-Path $PSScriptRoot '.env.local')

$checks = @(
    [pscustomobject]@{ Area = 'Security'; Name = 'JWT signing secret'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_JWT_SECRET') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Connector secret encryption key'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_CONNECTOR_SECRET_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Subscription bootstrap key'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_SUBSCRIPTION_BOOTSTRAP_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Workspace internal key'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_WORKSPACE_INTERNAL_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Notification internal key'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_NOTIFICATION_INTERNAL_KEY') },
    [pscustomobject]@{ Area = 'Local'; Name = 'Local launch script'; Required = $true; Ready = (Test-Path (Join-Path $PSScriptRoot 'launch-workrh.ps1')) },
    [pscustomobject]@{ Area = 'Local'; Name = 'Local Docker compose'; Required = $true; Ready = (Test-Path (Join-Path $PSScriptRoot 'infra\docker\docker-compose.yml')) },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Backup script present'; Required = $true; Ready = (Test-Path (Join-Path $PSScriptRoot 'backup-workrh.ps1')) },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Restore script present'; Required = $true; Ready = (Test-Path (Join-Path $PSScriptRoot 'restore-workrh.ps1')) },
    [pscustomobject]@{ Area = 'Docs'; Name = 'Pilot guide'; Required = $true; Ready = (Test-Path (Join-Path $PSScriptRoot 'docs\pilot-readiness.md')) },
    [pscustomobject]@{ Area = 'Stripe'; Name = 'Stripe configured'; Required = $false; Ready = ((Has-Value $envValues 'STRIPE_SECRET_KEY') -and (Has-Value $envValues 'STRIPE_PRICE_STARTER') -and (Has-Value $envValues 'STRIPE_PRICE_PRO') -and (Has-Value $envValues 'STRIPE_PRICE_PREMIUM')) },
    [pscustomobject]@{ Area = 'Support'; Name = 'SMTP configured'; Required = $false; Ready = (Has-Value $envValues 'SMTP_HOST') }
)

$checks | Format-Table -AutoSize

$blocking = $checks | Where-Object { $_.Required -and -not $_.Ready }
$optional = $checks | Where-Object { -not $_.Required -and -not $_.Ready }
Write-Host ''
if ($blocking.Count -eq 0) {
    Write-Host 'Pilot readiness: OK for demo / assisted pilot without paid hosting'
    if ($optional.Count -gt 0) {
        Write-Host 'Optional capabilities not enabled for the pilot:'
        $optional | ForEach-Object {
            Write-Host ("- [{0}] {1}" -f $_.Area, $_.Name)
        }
    }
    exit 0
}

Write-Host 'Pilot readiness: BLOCKED'
$blocking | ForEach-Object {
    Write-Host ("- [{0}] {1}" -f $_.Area, $_.Name)
}
exit 1
