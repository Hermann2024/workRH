#!/usr/bin/env pwsh
# ============================================
# WorkRH - Graceful stop for local services
# ============================================

$ErrorActionPreference = "SilentlyContinue"
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $repoRoot

# Colors
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

# Header
Write-Host ""
Write-Host "$($colors.Blue)=================================================$($colors.Reset)"
Write-Host "$($colors.Blue)  WorkRH - Stop local services$($colors.Reset)"
Write-Host "$($colors.Blue)=================================================$($colors.Reset)"
Write-Host ""

# Stop Java processes
Write-Status "Stopping Java services (Maven/Spring Boot)..." "INFO"
$javaProcesses = Get-Process java -ErrorAction SilentlyContinue
if ($javaProcesses) {
    $javaProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-Status "Java processes stopped" "OK"
} else {
    Write-Status "No Java process found" "WARNING"
}

# Stop Node.js
Write-Host ""
Write-Status "Stopping Node.js frontend..." "INFO"
$nodeProcesses = Get-Process node -ErrorAction SilentlyContinue
if ($nodeProcesses) {
    $nodeProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-Status "Node.js processes stopped" "OK"
} else {
    Write-Status "No Node.js process found" "WARNING"
}

# Stop Docker infrastructure
Write-Host ""
Write-Status "Stopping Docker infrastructure..." "INFO"

$composeFile = Join-Path $repoRoot "infra\docker\docker-compose.yml"
$dockerComposeCommand = Get-Command docker-compose -ErrorAction SilentlyContinue
if ($dockerComposeCommand) {
    & $dockerComposeCommand.Source -f $composeFile down
} else {
    $dockerCommand = Get-Command docker -ErrorAction SilentlyContinue
    if ($dockerCommand) {
        & $dockerCommand.Source compose -f $composeFile down
    }
}

if ($LASTEXITCODE -eq 0) {
    Write-Status "Docker infrastructure stopped" "OK"
} else {
    Write-Status "Docker infrastructure could not be stopped or was already down" "WARNING"
}

# Final summary
Write-Host ""
Write-Host "$($colors.Green)=================================================$($colors.Reset)"
Write-Host "$($colors.Green)  All local services have been stopped$($colors.Reset)"
Write-Host "$($colors.Green)=================================================$($colors.Reset)"
Write-Host ""
