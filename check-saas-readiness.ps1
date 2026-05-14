param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '.env.local'),
    [switch]$StrictGit
)

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
    param([hashtable]$Values, [string]$Name)
    return $Values.ContainsKey($Name) -and -not [string]::IsNullOrWhiteSpace($Values[$Name])
}

function Is-Base64-Secret32 {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }
    try {
        return ([Convert]::FromBase64String($Value).Length -ge 32)
    } catch {
        return $false
    }
}

function Is-Https-Origin {
    param([string]$Value)
    return -not [string]::IsNullOrWhiteSpace($Value) `
        -and $Value.StartsWith('https://') `
        -and -not $Value.Contains('localhost') `
        -and -not $Value.Contains('127.0.0.1') `
        -and $Value.Trim() -ne '*'
}

function File-Exists {
    param([string]$RelativePath)
    return Test-Path (Join-Path $PSScriptRoot $RelativePath)
}

$envValues = Read-EnvFile $EnvFile
$gitDirty = $false
try {
    $gitStatus = git -C $PSScriptRoot status --porcelain 2>$null
    $gitDirty = -not [string]::IsNullOrWhiteSpace(($gitStatus -join ''))
} catch {
    $gitDirty = $true
}

$checks = @(
    [pscustomobject]@{ Area = 'Security'; Name = 'JWT secret is Base64 and >= 32 bytes'; Required = $true; Ready = (Is-Base64-Secret32 $envValues['WORKRH_JWT_SECRET']) },
    [pscustomobject]@{ Area = 'Security'; Name = 'Demo authentication disabled'; Required = $true; Ready = ($envValues['WORKRH_DEMO_AUTHENTICATION_ENABLED'] -ne 'true') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Production guard enabled'; Required = $true; Ready = ($envValues['WORKRH_PRODUCTION_READINESS_ENFORCED'] -eq 'true') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Connector encryption key configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_CONNECTOR_SECRET_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Subscription bootstrap key configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_SUBSCRIPTION_BOOTSTRAP_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Workspace internal key configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_WORKSPACE_INTERNAL_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Notification internal key configured'; Required = $true; Ready = (Has-Value $envValues 'WORKRH_NOTIFICATION_INTERNAL_KEY') },
    [pscustomobject]@{ Area = 'Security'; Name = 'Frontend origin is public HTTPS'; Required = $true; Ready = (Is-Https-Origin $envValues['WORKRH_FRONTEND_ORIGIN']) },
    [pscustomobject]@{ Area = 'Database'; Name = 'Hibernate validates schema'; Required = $true; Ready = ($envValues['WORKRH_HIBERNATE_DDL_AUTO'] -eq 'validate') },
    [pscustomobject]@{ Area = 'Database'; Name = 'Database credentials configured'; Required = $true; Ready = ((Has-Value $envValues 'WORKRH_DATABASE_USERNAME') -and (Has-Value $envValues 'WORKRH_DATABASE_PASSWORD')) },
    [pscustomobject]@{ Area = 'Database'; Name = 'All service database URLs configured'; Required = $true; Ready = ((Has-Value $envValues 'WORKRH_USERS_DATABASE_URL') -and (Has-Value $envValues 'WORKRH_LEAVES_DATABASE_URL') -and (Has-Value $envValues 'WORKRH_SICKNESS_DATABASE_URL') -and (Has-Value $envValues 'WORKRH_TELEWORK_DATABASE_URL') -and (Has-Value $envValues 'WORKRH_NOTIFICATIONS_DATABASE_URL') -and (Has-Value $envValues 'WORKRH_REPORTING_DATABASE_URL') -and (Has-Value $envValues 'WORKRH_SUBSCRIPTIONS_DATABASE_URL')) },
    [pscustomobject]@{ Area = 'Billing'; Name = 'Stripe live configuration present'; Required = $true; Ready = ((Has-Value $envValues 'STRIPE_SECRET_KEY') -and (Has-Value $envValues 'STRIPE_WEBHOOK_SECRET') -and (Has-Value $envValues 'STRIPE_PRICE_STARTER') -and (Has-Value $envValues 'STRIPE_PRICE_PRO') -and (Has-Value $envValues 'STRIPE_PRICE_PREMIUM')) },
    [pscustomobject]@{ Area = 'Email'; Name = 'SMTP configured'; Required = $true; Ready = ((Has-Value $envValues 'SMTP_HOST') -and (Has-Value $envValues 'SMTP_USERNAME') -and (Has-Value $envValues 'SMTP_PASSWORD')) },
    [pscustomobject]@{ Area = 'Deployment'; Name = 'Production compose exists'; Required = $true; Ready = (File-Exists 'infra\docker\docker-compose.prod.yml') },
    [pscustomobject]@{ Area = 'Deployment'; Name = 'Image registry and tag configured'; Required = $true; Ready = ((Has-Value $envValues 'WORKRH_IMAGE_REGISTRY') -and (Has-Value $envValues 'WORKRH_IMAGE_TAG')) },
    [pscustomobject]@{ Area = 'Deployment'; Name = 'Public API base URL configured'; Required = $true; Ready = (Is-Https-Origin $envValues['WORKRH_PUBLIC_BASE_URL']) },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Backup, restore and smoke scripts exist'; Required = $true; Ready = ((File-Exists 'backup-workrh.ps1') -and (File-Exists 'restore-workrh.ps1') -and (File-Exists 'smoke-test-workrh.ps1')) },
    [pscustomobject]@{ Area = 'Operations'; Name = 'Incident runbook exists'; Required = $true; Ready = (File-Exists 'docs\incident-runbook.md') },
    [pscustomobject]@{ Area = 'Legal'; Name = 'Privacy notice exists'; Required = $true; Ready = (File-Exists 'docs\legal\privacy-notice.md') },
    [pscustomobject]@{ Area = 'Legal'; Name = 'Terms of service exist'; Required = $true; Ready = (File-Exists 'docs\legal\terms-of-service.md') },
    [pscustomobject]@{ Area = 'Legal'; Name = 'DPA exists'; Required = $true; Ready = (File-Exists 'docs\legal\data-processing-addendum.md') },
    [pscustomobject]@{ Area = 'Quality'; Name = 'Git working tree clean'; Required = [bool]$StrictGit; Ready = (-not $gitDirty) }
)

$checks | Sort-Object Area, Name | Format-Table -AutoSize

$blocking = $checks | Where-Object { $_.Required -and -not $_.Ready }
Write-Host ''
if ($blocking.Count -eq 0) {
    Write-Host 'SaaS readiness: OK for production go-live checks'
    exit 0
}

Write-Host 'SaaS readiness: BLOCKED'
$blocking | ForEach-Object {
    Write-Host ("- [{0}] {1}" -f $_.Area, $_.Name)
}
exit 1
