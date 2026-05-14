param(
    [string]$BaseUrl = $env:WORKRH_PUBLIC_BASE_URL,
    [int]$TimeoutSeconds = 20
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = 'http://localhost:9080'
}

$BaseUrl = $BaseUrl.TrimEnd('/')

function Invoke-SmokeRequest {
    param(
        [string]$Name,
        [string]$Path,
        [int[]]$AllowedStatusCodes = @(200)
    )

    $uri = $BaseUrl + $Path
    try {
        $response = Invoke-WebRequest -Uri $uri -Method GET -TimeoutSec $TimeoutSeconds -UseBasicParsing
        if ($AllowedStatusCodes -notcontains [int]$response.StatusCode) {
            throw "$Name returned unexpected status $($response.StatusCode)"
        }
        Write-Host ("[OK] {0} {1}" -f $Name, $response.StatusCode)
    } catch {
        throw "[FAIL] $Name at $uri - $($_.Exception.Message)"
    }
}

Invoke-SmokeRequest -Name 'Gateway health' -Path '/actuator/health'
Invoke-SmokeRequest -Name 'Subscription plans' -Path '/api/subscriptions/plans'
Invoke-SmokeRequest -Name 'Subscription catalog readiness' -Path '/api/subscriptions/catalog/readiness'

Write-Host "Smoke tests passed for $BaseUrl"
