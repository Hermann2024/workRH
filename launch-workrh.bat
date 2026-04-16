@echo off
REM ============================================
REM WorkRH - Full launch script
REM Starts backend services and frontend
REM ============================================

setlocal enabledelayedexpansion

set GREEN=[92m
set YELLOW=[93m
set RED=[91m
set BLUE=[94m
set RESET=[0m

echo.
echo %BLUE%==================================================%RESET%
echo %BLUE%  WorkRH - Full system launch%RESET%
echo %BLUE%==================================================%RESET%
echo.

echo %BLUE%[1/5]%RESET% Checking Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo %RED%[ERROR] Docker is not installed or not available%RESET%
    pause
    exit /b 1
)
echo %GREEN%[OK] Docker found%RESET%

echo.
echo %BLUE%[2/5]%RESET% Checking Maven...
mvn --version >nul 2>&1
if errorlevel 1 (
    echo %RED%[ERROR] Maven is not installed or not available%RESET%
    pause
    exit /b 1
)
echo %GREEN%[OK] Maven found%RESET%

echo.
echo %BLUE%[3/5]%RESET% Checking Node.js...
node --version >nul 2>&1
if errorlevel 1 (
    echo %RED%[ERROR] Node.js is not installed or not available%RESET%
    pause
    exit /b 1
)
echo %GREEN%[OK] Node.js found%RESET%

echo.
echo %BLUE%[4/5]%RESET% Starting Docker infrastructure...
cd /d "%~dp0"
set "DOCKER_COMPOSE_CMD="
docker-compose --version >nul 2>&1
if not errorlevel 1 (
    set "DOCKER_COMPOSE_CMD=docker-compose"
) else (
    docker compose version >nul 2>&1
    if not errorlevel 1 (
        set "DOCKER_COMPOSE_CMD=docker compose"
    )
)

if not defined DOCKER_COMPOSE_CMD (
    echo %RED%[ERROR] Docker Compose is not available%RESET%
    pause
    exit /b 1
)

%DOCKER_COMPOSE_CMD% -f infra\docker\docker-compose.yml up -d
if errorlevel 1 (
    echo %RED%[ERROR] Failed to start Docker infrastructure%RESET%
    pause
    exit /b 1
)
echo %GREEN%[OK] Docker infrastructure started%RESET%
timeout /t 5 /nobreak

echo.
echo %BLUE%[5/5]%RESET% Building backend and installing reactor artifacts...
call mvn clean install -DskipTests -T1C
if errorlevel 1 (
    echo %RED%[ERROR] Maven build failed%RESET%
    pause
    exit /b 1
)
echo %GREEN%[OK] Backend build completed%RESET%

echo.
echo %GREEN%=================================================%RESET%
echo %GREEN%  Starting services in new windows%RESET%
echo %GREEN%=================================================%RESET%
echo.

echo %BLUE%[INFO]%RESET% Starting Config Server...
start "WorkRH - Config Server (Port 9888)" cmd /k "cd /d %~dp0 && mvn -pl platform/config-server spring-boot:run"
timeout /t 6 /nobreak

echo %BLUE%[INFO]%RESET% Starting Discovery Service...
start "WorkRH - Discovery Service (Port 9761)" cmd /k "cd /d %~dp0 && mvn -pl platform/discovery-service spring-boot:run"
timeout /t 6 /nobreak

echo %BLUE%[INFO]%RESET% Starting API Gateway...
start "WorkRH - API Gateway (Port 9080)" cmd /k "cd /d %~dp0 && mvn -pl platform/api-gateway spring-boot:run"
timeout /t 6 /nobreak

echo %BLUE%[INFO]%RESET% Starting business services...
start "WorkRH - User Service" cmd /k "cd /d %~dp0 && mvn -pl services/user-service spring-boot:run"
timeout /t 3 /nobreak

start "WorkRH - Leave Service" cmd /k "cd /d %~dp0 && mvn -pl services/leave-service spring-boot:run"
timeout /t 3 /nobreak

start "WorkRH - Sickness Service" cmd /k "cd /d %~dp0 && mvn -pl services/sickness-service spring-boot:run"
timeout /t 3 /nobreak

start "WorkRH - Telework Service" cmd /k "cd /d %~dp0 && mvn -pl services/telework-service spring-boot:run"
timeout /t 3 /nobreak

start "WorkRH - Notification Service" cmd /k "cd /d %~dp0 && mvn -pl services/notification-service spring-boot:run"
timeout /t 3 /nobreak

start "WorkRH - Reporting Service" cmd /k "cd /d %~dp0 && mvn -pl services/reporting-service spring-boot:run"
timeout /t 3 /nobreak

start "WorkRH - Subscription Service" cmd /k "cd /d %~dp0 && mvn -pl services/subscription-service spring-boot:run"
timeout /t 3 /nobreak

echo.
echo %BLUE%[INFO]%RESET% Starting Angular frontend...
start "WorkRH - Frontend Angular (Port 4200)" cmd /k "cd /d %~dp0frontend\angular-app && npm install && npm start"

echo.
echo %GREEN%=================================================%RESET%
echo %GREEN%  Launch in progress%RESET%
echo %GREEN%=================================================%RESET%
echo.
echo %BLUE%Application URLs:%RESET%
echo   - Frontend:         http://localhost:4200
echo   - API Gateway:      http://localhost:9080
echo   - Eureka Dashboard: http://localhost:9761
echo   - Config Server:    http://localhost:9888
echo.
echo %YELLOW%Demo credentials:%RESET%
echo   Email:    rh@company.com
echo   Password: secret
echo   Tenant:   demo-lu
echo.
echo %BLUE%Check each service window for live logs.%RESET%
echo.
pause
