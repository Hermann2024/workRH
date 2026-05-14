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
