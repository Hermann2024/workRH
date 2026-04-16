@echo off
REM ============================================
REM WorkRH - Checklist de prérequis
REM ============================================

setlocal enabledelayedexpansion

set /a checks_passed=0
set /a checks_total=0

REM Couleurs
set GREEN=[92m
set YELLOW=[93m
set RED=[91m
set BLUE=[94m
set RESET=[0m

echo.
echo %BLUE%==================================================%RESET%
echo %BLUE%  WorkRH - Vérification des prérequis%RESET%
echo %BLUE%==================================================%RESET%
echo.

REM 1. Docker
echo %BLUE%[1/5]%RESET% Vérification Docker...
set /a checks_total+=1
docker --version >nul 2>&1
if errorlevel 1 (
    echo %RED%  ✗ Docker n'est pas installé%RESET%
    echo %YELLOW%  → Installer depuis: https://www.docker.com/products/docker-desktop%RESET%
) else (
    for /f "tokens=*" %%i in ('docker --version') do echo %GREEN%  ✓ %%i%RESET%
    set /a checks_passed+=1
)

REM 2. Docker Compose
echo.
echo %BLUE%[2/5]%RESET% Vérification Docker Compose...
set /a checks_total+=1
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo %RED%  ✗ Docker Compose n'est pas installé%RESET%
    echo %YELLOW%  → Installer depuis: https://docs.docker.com/compose/install/%RESET%
) else (
    for /f "tokens=*" %%i in ('docker-compose --version') do echo %GREEN%  ✓ %%i%RESET%
    set /a checks_passed+=1
)

REM 3. Java/Maven
echo.
echo %BLUE%[3/5]%RESET% Vérification Java et Maven...
set /a checks_total+=1
java -version >nul 2>&1
if errorlevel 1 (
    echo %RED%  ✗ Java n'est pas installé%RESET%
    echo %YELLOW%  → Installer Java 17+ depuis: https://adoptium.net/%RESET%
) else (
    for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr /R "version"') do echo %GREEN%  ✓ %%i%RESET%
)

mvn --version >nul 2>&1
if errorlevel 1 (
    echo %RED%  ✗ Maven n'est pas installé%RESET%
    echo %YELLOW%  → Installer Maven depuis: https://maven.apache.org/%RESET%
) else (
    for /f "tokens=*" %%i in ('mvn --version 2^>^&1 ^| findstr /R "Apache"') do echo %GREEN%  ✓ %%i%RESET%
    set /a checks_passed+=1
)

REM 4. Node.js/npm
echo.
echo %BLUE%[4/5]%RESET% Vérification Node.js et npm...
set /a checks_total+=1
node --version >nul 2>&1
if errorlevel 1 (
    echo %RED%  ✗ Node.js n'est pas installé%RESET%
    echo %YELLOW%  → Installer Node.js 18+ depuis: https://nodejs.org/%RESET%
) else (
    for /f "tokens=*" %%i in ('node --version') do echo %GREEN%  ✓ Node.js %%i%RESET%
)

npm --version >nul 2>&1
if errorlevel 1 (
    echo %RED%  ✗ npm n'est pas installé%RESET%
) else (
    for /f "tokens=*" %%i in ('npm --version') do echo %GREEN%  ✓ npm %%i%RESET%
    set /a checks_passed+=1
)

REM 5. Fichiers nécessaires
echo.
echo %BLUE%[5/5]%RESET% Vérification des fichiers de configuration...
set /a checks_total+=1

if exist "infra\docker\docker-compose.yml" (
    echo %GREEN%  ✓ docker-compose.yml trouvé%RESET%
) else (
    echo %RED%  ✗ docker-compose.yml manquant%RESET%
)

if exist "pom.xml" (
    echo %GREEN%  ✓ pom.xml trouvé%RESET%
) else (
    echo %RED%  ✗ pom.xml manquant%RESET%
)

if exist "frontend\angular-app\package.json" (
    echo %GREEN%  ✓ package.json trouvé%RESET%
    set /a checks_passed+=1
) else (
    echo %RED%  ✗ package.json manquant%RESET%
)

REM Résumé
echo.
echo %BLUE%==================================================%RESET%

if !checks_passed! equ !checks_total! (
    echo %GREEN%  ✓ Tous les prérequis sont ok !%RESET%
    echo %GREEN%==================================================%RESET%
    echo.
    echo %GREEN%Tu peux maintenant lancer:%RESET%
    echo.
    echo   launch-workrh.bat
    echo.
    echo %BLUE%ou en PowerShell:%RESET%
    echo.
    echo   .\launch-workrh.ps1
    echo.
) else (
    echo %RED%  ✗ %checks_passed%/%checks_total% vérifications ok%RESET%
    echo %RED%==================================================%RESET%
    echo.
    echo %YELLOW%Veuillez installer les outils manquants avant de continuer%RESET%
    echo.
)

pause
