@echo off
REM ============================================
REM WorkRH - Arrêt propre de tous les services
REM ============================================

setlocal enabledelayedexpansion

REM Couleurs
set GREEN=[92m
set YELLOW=[93m
set RED=[91m
set BLUE=[94m
set RESET=[0m

echo.
echo %BLUE%==================================================%RESET%
echo %BLUE%  WorkRH - Arrêt des services%RESET%
echo %BLUE%==================================================%RESET%
echo.

REM Arrêter les processus Java
echo %BLUE%[INFO]%RESET% Arrêt des services Java (Maven/Spring Boot)...
taskkill /IM java.exe /F /T 2>nul
if errorlevel 1 (
    echo %YELLOW%[ATTENTION] Aucun processus Java n'a été trouvé%RESET%
) else (
    echo %GREEN%[OK] Processus Java arrêtés%RESET%
)

REM Arrêter Node.js
echo.
echo %BLUE%[INFO]%RESET% Arrêt du serveur Node.js (Frontend)...
taskkill /IM node.exe /F /T 2>nul
if errorlevel 1 (
    echo %YELLOW%[ATTENTION] Aucun processus Node.js n'a été trouvé%RESET%
) else (
    echo %GREEN%[OK] Processus Node.js arrêtés%RESET%
)

REM Arrêter Docker Compose
echo.
echo %BLUE%[INFO]%RESET% Arrêt de l'infrastructure Docker...
cd /d "%~dp0"
docker-compose -f infra\docker\docker-compose.yml down
if errorlevel 1 (
    echo %RED%[ERREUR] Impossible d'arrêter Docker Compose%RESET%
) else (
    echo %GREEN%[OK] Infrastructure Docker arrêtée%RESET%
)

echo.
echo %GREEN%==================================================%RESET%
echo %GREEN%  ✓ Tous les services ont été arrêtés%RESET%
echo %GREEN%==================================================%RESET%
echo.

pause
