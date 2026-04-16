# Run from repository root. This script opens new PowerShell windows to run each service.
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $repoRoot

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

$shellCommand = Get-Command pwsh -ErrorAction SilentlyContinue
if (-not $shellCommand) {
    $shellCommand = Get-Command powershell -ErrorAction Stop
}

if (Test-Path ".env.local") {
    Write-Host "Loading environment variables from .env.local"
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

$dockerDesktopBin = "C:\Program Files\Docker\Docker\resources\bin"
if (-not (Get-Command docker -ErrorAction SilentlyContinue) -and (Test-Path (Join-Path $dockerDesktopBin "docker.exe"))) {
    $env:Path += ";$dockerDesktopBin"
}

$dockerComposeCommand = Get-Command docker-compose -ErrorAction SilentlyContinue
if ($dockerComposeCommand) {
    Write-Host "Starting infrastructure: Postgres, Kafka, Zookeeper (docker-compose up -d)"
    & $dockerComposeCommand.Source -f $composeFile up -d @infraServices
} else {
    $dockerCommand = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $dockerCommand) {
        throw "Neither docker-compose nor docker is available in PATH."
    }

    Write-Host "Starting infrastructure: Postgres, Kafka, Zookeeper (docker compose up -d)"
    & $dockerCommand.Source compose -f $composeFile up -d @infraServices
}

Write-Host "Building project and installing reactor artifacts locally (tests fully skipped)"
mvn -T1C -Dmaven.test.skip=true clean install
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed."
}

Write-Host "Starting Config Server"
Start-Process -FilePath $shellCommand.Source -ArgumentList "-NoExit","-Command","Set-Location '$repoRoot'; mvn -pl platform/config-server spring-boot:run"
Start-Sleep -Seconds 6

Write-Host "Starting Discovery (Eureka)"
Start-Process -FilePath $shellCommand.Source -ArgumentList "-NoExit","-Command","Set-Location '$repoRoot'; mvn -pl platform/discovery-service spring-boot:run"
Start-Sleep -Seconds 6

Write-Host "Starting API Gateway"
Start-Process -FilePath $shellCommand.Source -ArgumentList "-NoExit","-Command","Set-Location '$repoRoot'; mvn -pl platform/api-gateway spring-boot:run"
Start-Sleep -Seconds 6

Write-Host "Starting microservices"
$services = @("services/user-service","services/leave-service","services/sickness-service","services/telework-service","services/reporting-service","services/notification-service","services/subscription-service")
foreach ($s in $services) {
    Write-Host "Starting $s"
    Start-Process -FilePath $shellCommand.Source -ArgumentList "-NoExit","-Command","Set-Location '$repoRoot'; mvn -pl $s spring-boot:run"
    Start-Sleep -Seconds 2
}

Write-Host "All start commands issued. Check consoles for logs."
