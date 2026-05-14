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
$backupScriptReady = Test-Path (Join-Path $PSScriptRoot 'backup-workrh.ps1')
$restoreScriptReady = Test-Path (Join-Path $PSScriptRoot 'restore-workrh.ps1')
$smokeScriptReady = Test-Path (Join-Path $PSScriptRoot 'smoke-test-workrh.ps1')
$prodComposeReady = Test-Path (Join-Path $PSScriptRoot 'infra\docker\docker-compose.prod.yml')
$productionReadinessDocReady = Test-Path (Join-Path $PSScriptRoot 'docs\production-readiness.md')
$incidentRunbookReady = Test-Path (Join-Path $PSScriptRoot 'docs\incident-runbook.md')

$checks = @(
    [pscustomobject]@{ Area = 'Security'; Name = 'JWT signing secret'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_JWT_SECRET') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Demo auth disabled'; Required = $true; Ready = ($envValues['WORKRH_DEMO_AUTHENTICATION_ENABLED'] -ne 'true') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Connector secret encryption key'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_CONNECTOR_SECRET_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Subscription bootstrap key'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_SUBSCRIPTION_BOOTSTRAP_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Workspace internal key'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_WORKSPACE_INTERNAL_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Notification internal key'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_NOTIFICATION_INTERNAL_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Frontend origin configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_FRONTEND_ORIGIN') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Hibernate schema validation'; Required = $true; Ready = ($envValues['WORKRH_HIBERNATE_DDL_AUTO'] -eq 'validate') },
    [pscustomobject]@{ Area = 'Deployment'; Name = 'Public API base URL'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_PUBLIC_BASE_URL') },
    [pscustomobject]@{ Area = 'Deployment'; Name = 'Image registry configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_IMAGE_REGISTRY') },
    [pscustomobject]@{ Area = 'Deployment'; Name = 'Image tag configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_IMAGE_TAG') },
    [pscustomobject]@{ Area = 'Deployment'; Name = 'Kafka bootstrap configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_KAFKA_BOOTSTRAP_SERVERS') },
    [pscustomobject]@{ Area = 'Deployment'; Name = 'Production compose file'; Required = $true; Ready = $prodComposeReady },
    [pscustomobject]@{ Area = 'Database'; Name = 'Database username'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_DATABASE_USERNAME') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Database password'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_DATABASE_PASSWORD') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Users database URL'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_USERS_DATABASE_URL') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Leaves database URL'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_LEAVES_DATABASE_URL') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Sickness database URL'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_SICKNESS_DATABASE_URL') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Telework database URL'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_TELEWORK_DATABASE_URL') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Notifications database URL'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_NOTIFICATIONS_DATABASE_URL') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Reporting database URL'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_REPORTING_DATABASE_URL') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Subscriptions database URL'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_SUBSCRIPTIONS_DATABASE_URL') },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Backup directory configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_BACKUP_DIR') },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Incident contact configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_INCIDENT_CONTACT') },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Backup script present'; Required = $true; Ready = $backupScriptReady },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Restore script present'; Required = $true; Ready = $restoreScriptReady },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Smoke test script present'; Required = $true; Ready = $smokeScriptReady },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Production readiness doc'; Required = $true; Ready = $productionReadinessDocReady },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Incident runbook'; Required = $true; Ready = $incidentRunbookReady },
    [pscustomobject]@{ Area = 'Stripe'; Name = 'Secret key'; Required = $true; Ready = (Has-Value $envValues 'STRIPE_SECRET_KEY') },
    [pscustomobject]@{ Area = 'Stripe'; Name = 'Webhook secret'; Required = $true; Ready = (Has-Value $envValues 'STRIPE_WEBHOOK_SECRET') },
    [pscustomobject]@{ Area = 'Stripe'; Name = 'Starter price'; Required = $true; Ready = (Has-Value $envValues 'STRIPE_PRICE_STARTER') },
    [pscustomobject]@{ Area = 'Stripe'; Name = 'Pro price'; Required = $true; Ready = (Has-Value $envValues 'STRIPE_PRICE_PRO') },
    [pscustomobject]@{ Area = 'Stripe'; Name = 'Premium price'; Required = $true; Ready = (Has-Value $envValues 'STRIPE_PRICE_PREMIUM') },
    [pscustomobject]@{ Area = 'Support'; Name = 'SMTP host'; Required = $true; Ready = (Has-Value $envValues 'SMTP_HOST') },
    [pscustomobject]@{ Area = 'SMS'; Name = 'SMS enabled'; Required = $false; Ready = ($envValues['WORKRH_SMS_ENABLED'] -eq 'true') },
    [pscustomobject]@{ Area = 'SMS'; Name = 'SMS webhook'; Required = $false; Ready = (Has-Value $envValues 'WORKRH_SMS_WEBHOOK_URL') },
    [pscustomobject]@{ Area = 'Enterprise'; Name = 'Enterprise enabled'; Required = $false; Ready = ($envValues['WORKRH_ENTERPRISE_ENABLED'] -eq 'true') }
)

$checks | Format-Table -AutoSize

$blocking = $checks | Where-Object { $_.Required -and -not $_.Ready }
$optional = $checks | Where-Object { -not $_.Required -and -not $_.Ready }
Write-Host ''
if ($blocking.Count -eq 0) {
    Write-Host 'Commercial readiness: OK for self-service Starter/Pro/Premium'
    if ($optional.Count -gt 0) {
        Write-Host 'Optional capabilities not enabled:'
        $optional | ForEach-Object {
            Write-Host ("- [{0}] {1}" -f $_.Area, $_.Name)
        }
    }
    exit 0
}

Write-Host 'Commercial readiness: BLOCKED'
$blocking | ForEach-Object {
    Write-Host ("- [{0}] {1}" -f $_.Area, $_.Name)
}
exit 1
