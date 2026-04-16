#!/usr/bin/env pwsh
# ============================================
# WorkRH - Full launch script (PowerShell)
# Starts backend services and frontend
# ============================================

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $repoRoot

$shellCommand = Get-Command pwsh -ErrorAction SilentlyContinue
if (-not $shellCommand) {
    $shellCommand = Get-Command powershell -ErrorAction Stop
}

$colors = @{
    Green  = "`e[92m"
    Yellow = "`e[93m"
    Red    = "`e[91m"
    Blue   = "`e[94m"
    Reset  = "`e[0m"
}

function Write-Status([string]$message, [string]$type = "INFO") {
    $timestamp = Get-Date -Format "HH:mm:ss"
    switch ($type) {
        "OK" { Write-Host "$($colors.Green)[$timestamp] OK  $message$($colors.Reset)" }
        "ERROR" { Write-Host "$($colors.Red)[$timestamp] ERR $message$($colors.Reset)" }
        "WARNING" { Write-Host "$($colors.Yellow)[$timestamp] WARN $message$($colors.Reset)" }
        default { Write-Host "$($colors.Blue)[$timestamp] INF $message$($colors.Reset)" }
    }
}

Write-Host ""
Write-Host "$($colors.Blue)=================================================$($colors.Reset)"
Write-Host "$($colors.Blue)  WorkRH - Full system launch$($colors.Reset)"
Write-Host "$($colors.Blue)=================================================$($colors.Reset)"
Write-Host ""

Write-Status "Checking prerequisites..."
$checks = @{
    "docker" = "Docker"
    "mvn" = "Maven"
    "node" = "Node.js"
    "npm" = "npm"
}

foreach ($cmd in $checks.Keys) {
    if (Get-Command $cmd -ErrorAction SilentlyContinue) {
        $version = & $cmd --version 2>$null | Select-Object -First 1
        Write-Status "$($checks[$cmd]): $version" "OK"
    } else {
        Write-Status "$($checks[$cmd]) is not installed or not available in PATH" "ERROR"
        exit 1
    }
}

if (Test-Path ".env.local") {
    Write-Status "Loading environment variables from .env.local..."
    Get-Content ".env.local" | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') {
            return
        }

        $parts = $_ -split '=', 2
        if ($parts.Count -eq 2) {
            [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
        }
    }
}

Write-Host ""
Write-Status "Starting Docker infrastructure..."
$composeFile = Join-Path $repoRoot "infra\docker\docker-compose.yml"
$infraServices = @(
    "postgres-users",
    "postgres-leaves",
    "postgres-sickness",
    "postgres-telework",
    "postgres-notifications",
    "postgres-reporting",
    "postgres-subscriptions",
    "kafka",
    "zookeeper"
)
$dockerComposeCommand = Get-Command docker-compose -ErrorAction SilentlyContinue
if ($dockerComposeCommand) {
    & $dockerComposeCommand.Source -f $composeFile up -d @infraServices
} else {
    $dockerCommand = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $dockerCommand) {
        Write-Status "Docker is not available in PATH" "ERROR"
        exit 1
    }

    & $dockerCommand.Source compose -f $composeFile up -d @infraServices
}

if ($LASTEXITCODE -eq 0) {
    Write-Status "Docker infrastructure started" "OK"
    Start-Sleep -Seconds 5
} else {
    Write-Status "Failed to start Docker infrastructure" "ERROR"
    exit 1
}

Write-Host ""
Write-Status "Building backend and installing reactor artifacts locally..."
mvn clean install -Dmaven.test.skip=true -T1C

if ($LASTEXITCODE -eq 0) {
    Write-Status "Backend build completed" "OK"
} else {
    Write-Status "Backend build failed" "ERROR"
    exit 1
}

Write-Host ""
Write-Host "$($colors.Green)=================================================$($colors.Reset)"
Write-Host "$($colors.Green)  Starting services$($colors.Reset)"
Write-Host "$($colors.Green)=================================================$($colors.Reset)"
Write-Host ""

$services = @(
    @{ name = "Config Server"; path = "platform/config-server"; port = "9888" },
    @{ name = "Discovery Service"; path = "platform/discovery-service"; port = "9761" },
    @{ name = "API Gateway"; path = "platform/api-gateway"; port = "9080" },
    @{ name = "User Service"; path = "services/user-service"; port = "9081" },
    @{ name = "Leave Service"; path = "services/leave-service"; port = "9082" },
    @{ name = "Sickness Service"; path = "services/sickness-service"; port = "9083" },
    @{ name = "Telework Service"; path = "services/telework-service"; port = "9084" },
    @{ name = "Notification Service"; path = "services/notification-service"; port = "9085" },
    @{ name = "Reporting Service"; path = "services/reporting-service"; port = "9086" },
    @{ name = "Subscription Service"; path = "services/subscription-service"; port = "9087" }
)

$windowsOpened = 0

function Wait-ForPort([string]$serviceName, [int]$port, [int]$timeoutSeconds = 120) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $listener = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
        if ($listener) {
            Write-Status "$serviceName is listening on port $port" "OK"
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "$serviceName did not open port $port within $timeoutSeconds seconds."
}

foreach ($service in $services) {
    Write-Status "Starting $($service.name) on port $($service.port)..."
    $command = "Set-Location '$repoRoot'; mvn -pl '$($service.path)' spring-boot:run"
    Start-Process -FilePath $shellCommand.Source -ArgumentList "-NoExit", "-Command", $command -WindowStyle Normal
    $windowsOpened++
    Wait-ForPort $service.name ([int]$service.port)
}

Write-Host ""
Write-Status "Starting Angular frontend on port 4200..."
$frontendCmd = "Set-Location '$repoRoot\frontend\angular-app'; npm install; npm start"
Start-Process -FilePath $shellCommand.Source -ArgumentList "-NoExit", "-Command", $frontendCmd -WindowStyle Normal

Write-Host ""
Write-Host "$($colors.Green)=================================================$($colors.Reset)"
Write-Host "$($colors.Green)  Launch in progress$($colors.Reset)"
Write-Host "$($colors.Green)=================================================$($colors.Reset)"
Write-Host ""
Write-Host "$($colors.Blue)Application URLs:$($colors.Reset)"
Write-Host "  - Frontend:         http://localhost:4200"
Write-Host "  - API Gateway:      http://localhost:9080"
Write-Host "  - Eureka Dashboard: http://localhost:9761"
Write-Host "  - Config Server:    http://localhost:9888"
Write-Host ""
Write-Host "$($colors.Yellow)Demo credentials:$($colors.Reset)"
Write-Host "  Email:    rh@company.com"
Write-Host "  Password: secret"
Write-Host "  Tenant:   demo-lu"
Write-Host ""
Write-Host "$($colors.Blue)$windowsOpened service windows were opened.$($colors.Reset)"
Write-Host "$($colors.Blue)Check those windows for live logs.$($colors.Reset)"
