param(
    [string]$ComposeFile,
    [switch]$SkipBuild,
    [switch]$SkipPull
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ComposeFile)) {
    $ComposeFile = Join-Path $PSScriptRoot 'infra\docker\docker-compose.prod.yml'
}

function Import-EnvFile {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Environment file not found: $Path"
    }

    Get-Content $Path | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') {
            return
        }
        $parts = $_ -split '=', 2
        if ($parts.Count -eq 2) {
            [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
        }
    }
}

function Invoke-Checked {
    param(
        [scriptblock]$Command,
        [string]$FailureMessage
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw $FailureMessage
    }
}

Import-EnvFile (Join-Path $PSScriptRoot '.env.local')

Write-Host 'Running SaaS production readiness gate...'
Invoke-Checked { & (Join-Path $PSScriptRoot 'check-saas-readiness.ps1') } 'SaaS production readiness gate failed.'

Write-Host 'Running commercial readiness gate...'
Invoke-Checked { & (Join-Path $PSScriptRoot 'check-commercial-readiness.ps1') } 'Commercial readiness gate failed.'

if (-not $SkipBuild) {
    Write-Host 'Running backend tests...'
    Invoke-Checked { mvn test } 'Backend tests failed.'

    Write-Host 'Building frontend...'
    Invoke-Checked { npm --prefix (Join-Path $PSScriptRoot 'frontend\angular-app') run build } 'Frontend build failed.'
}

$dockerCompose = Get-Command docker-compose -ErrorAction SilentlyContinue
$composeCommand = if ($dockerCompose) {
    @('docker-compose', '-f', $ComposeFile)
} else {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $docker) {
        throw 'Neither docker-compose nor docker is available in PATH.'
    }
    @('docker', 'compose', '-f', $ComposeFile)
}

$command = $composeCommand[0]
$baseArgs = $composeCommand[1..($composeCommand.Length - 1)]

if (-not $SkipPull) {
    Write-Host 'Pulling production images...'
    Invoke-Checked { & $command @baseArgs pull } 'Unable to pull production images.'
}

Write-Host 'Starting production stack...'
Invoke-Checked { & $command @baseArgs up -d } 'Unable to start production stack.'

Write-Host 'Running smoke tests...'
Invoke-Checked { & (Join-Path $PSScriptRoot 'smoke-test-workrh.ps1') -BaseUrl $env:WORKRH_PUBLIC_BASE_URL } 'Smoke tests failed.'

Write-Host 'Production deployment finished.'
