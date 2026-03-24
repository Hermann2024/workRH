# Run from repository root. This script opens new PowerShell windows to run each service (requires pwsh)
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $repoRoot

Write-Host "Starting infrastructure: Postgres, Kafka, Zookeeper (docker-compose up -d)"
docker-compose up -d postgres kafka zookeeper

Write-Host "Building project (skip tests)"
mvn -T1C -DskipTests clean package

Write-Host "Starting Config Server"
Start-Process -FilePath pwsh -ArgumentList "-NoExit","-Command","mvn -pl platform/config-server spring-boot:run"
Start-Sleep -Seconds 6

Write-Host "Starting Discovery (Eureka)"
Start-Process -FilePath pwsh -ArgumentList "-NoExit","-Command","mvn -pl platform/discovery-service spring-boot:run"
Start-Sleep -Seconds 6

Write-Host "Starting API Gateway"
Start-Process -FilePath pwsh -ArgumentList "-NoExit","-Command","mvn -pl platform/api-gateway spring-boot:run"
Start-Sleep -Seconds 6

Write-Host "Starting microservices"
$services = @("services/user-service","services/leave-service","services/sickness-service","services/telework-service","services/reporting-service","services/notification-service","services/subscription-service")
foreach ($s in $services) {
    Write-Host "Starting $s"
    Start-Process -FilePath pwsh -ArgumentList "-NoExit","-Command","mvn -pl $s spring-boot:run"
    Start-Sleep -Seconds 2
}

Write-Host "All start commands issued. Check consoles for logs."
