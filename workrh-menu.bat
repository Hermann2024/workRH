@echo off
REM ============================================
REM WorkRH - Menu interactif de lancement
REM ============================================

setlocal enabledelayedexpansion

REM Couleurs
set GREEN=[92m
set YELLOW=[93m
set RED=[91m
set BLUE=[94m
set RESET=[0m

:menu
cls
echo.
echo %BLUE%╔════════════════════════════════════════════════╗%RESET%
echo %BLUE%║                  WorkRH Platform               ║%RESET%
echo %BLUE%║     SaaS HR Management with Telework Ctrl     ║%RESET%
echo %BLUE%╚════════════════════════════════════════════════╝%RESET%
echo.
echo %GREEN%Version: 1.0.0 - Build Date: 28 mars 2026%RESET%
echo.
echo.
echo   %BLUE%█%RESET% MENU PRINCIPAL
echo.
echo   1) %GREEN%▶ Lancer l'application complète%RESET%
echo      (Backend + Frontend)
echo.
echo   2) %YELLOW%⚙ Vérifier les prérequis%RESET%
echo      (Docker, Maven, Node.js, etc.)
echo.
echo   3) %RED%■ Arrêter tous les services%RESET%
echo      (Shutdown propre)
echo.
echo   4) 📊 Voir le statut des services
echo      (Eureka Dashboard)
echo.
echo   5) 📝 Consulter la documentation
echo      (Guides et dépannage)
echo.
echo   6) 🔧 Commandes avancées
echo      (Options de développement)
echo.
echo   0) %RED%✕ Quitter%RESET%
echo.
echo.
set /p choice="   Choisir une option [0-6]: "

if "!choice!"=="1" goto launch_app
if "!choice!"=="2" goto check_req
if "!choice!"=="3" goto stop_app
if "!choice!"=="4" goto eureka
if "!choice!"=="5" goto docs
if "!choice!"=="6" goto advanced
if "!choice!"=="0" goto quit

echo %RED%Option invalide !%RESET%
timeout /t 2 /nobreak
goto menu

REM ============================================
REM 1. LANCER L'APPLICATION
REM ============================================
:launch_app
cls
echo.
echo %BLUE%╔════════════════════════════════════════════════╗%RESET%
echo %BLUE%║        Lancement de WorkRH Platform            ║%RESET%
echo %BLUE%╚════════════════════════════════════════════════╝%RESET%
echo.
echo %YELLOW%Ceci va lancer:%RESET%
echo   • Infrastructure Docker
echo   • Config Server
echo   • Eureka Discovery
echo   • API Gateway
echo   • 7 Microservices
echo   • Frontend Angular
echo.
echo %YELLOW%Assurez-vous que Docker Desktop est lancé.%RESET%
echo.
echo Appuyez sur une touche pour commencer ou [Q] pour annuler...
set /p confirm=
if /i "!confirm!"=="Q" goto menu

call launch-workrh.bat
goto menu

REM ============================================
REM 2. VÉRIFIER LES PRÉREQUIS
REM ============================================
:check_req
cls
call check-requirements.bat
goto menu

REM ============================================
REM 3. ARRÊTER L'APPLICATION
REM ============================================
:stop_app
cls
echo.
echo %RED%╔════════════════════════════════════════════════╗%RESET%
echo %RED%║        Arrêt de WorkRH Platform                ║%RESET%
echo %RED%╚════════════════════════════════════════════════╝%RESET%
echo.
echo %YELLOW%Ceci va arrêter:%RESET%
echo   • Tous les services Java
echo   • Serveur Node.js (Frontend)
echo   • Infrastructure Docker
echo.
set /p confirm="Continuer ? [Y/n]: "
if /i "!confirm!"=="N" goto menu
if /i "!confirm!"=="n" goto menu

call stop-workrh.bat
goto menu

REM ============================================
REM 4. EUREKA DASHBOARD
REM ============================================
:eureka
cls
echo.
echo %BLUE%Ouverture du Eureka Dashboard...%RESET%
timeout /t 1 /nobreak
start "" http://localhost:8761
goto menu

REM ============================================
REM 5. DOCUMENTATION
REM ============================================
:docs
:doc_menu
cls
echo.
echo %BLUE%╔════════════════════════════════════════════════╗%RESET%
echo %BLUE%║          Documentation WorkRH                  ║%RESET%
echo %BLUE%╚════════════════════════════════════════════════╝%RESET%
echo.
echo   1) Guide de lancement (LAUNCH_GUIDE.md)
echo   2) Guide de dépannage (TROUBLESHOOTING.md)
echo   3) Configuration (LAUNCH_CONFIG.md)
echo   4) Suite des scripts (LAUNCH_SUITE.md)
echo   5) README principal
echo.
echo   0) Retour au menu
echo.
set /p doc_choice="Choisir un document [0-5]: "

if "!doc_choice!"=="1" start "" LAUNCH_GUIDE.md
if "!doc_choice!"=="2" start "" TROUBLESHOOTING.md
if "!doc_choice!"=="3" start "" LAUNCH_CONFIG.md
if "!doc_choice!"=="4" start "" LAUNCH_SUITE.md
if "!doc_choice!"=="5" start "" README.md

if not "!doc_choice!"=="0" (
    timeout /t 1 /nobreak
    goto doc_menu
)

goto menu

REM ============================================
REM 6. COMMANDES AVANCÉES
REM ============================================
:advanced
:advanced_menu
cls
echo.
echo %BLUE%╔════════════════════════════════════════════════╗%RESET%
echo %BLUE%║       Commandes avancées                       ║%RESET%
echo %BLUE%╚════════════════════════════════════════════════╝%RESET%
echo.
echo   1) Compiler le backend uniquement
echo      (mvn clean package -DskipTests)
echo.
echo   2) Lancer le frontend uniquement
echo      (npm install + npm start)
echo.
echo   3) Afficher les logs Docker
echo      (docker-compose logs -f)
echo.
echo   4) Voir l'état de tous les conteneurs
echo      (docker ps -a)
echo.
echo   5) Réinitialiser les bases de données
echo      (reset complet)
echo.
echo   6) Nettoyer les caches
echo      (Maven, npm, Docker)
echo.
echo   0) Retour au menu
echo.
set /p adv_choice="Choisir une commande [0-6]: "

if "!adv_choice!"=="1" (
    cls
    echo %BLUE%Compilation en cours...%RESET%
    mvn clean package -DskipTests -T1C
    pause
)

if "!adv_choice!"=="2" (
    cls
    echo %BLUE%Lancement du frontend...%RESET%
    cd frontend\angular-app
    call npm install
    call npm start
    pause
)

if "!adv_choice!"=="3" (
    cls
    docker-compose logs -f
)

if "!adv_choice!"=="4" (
    cls
    docker ps -a
    pause
)

if "!adv_choice!"=="5" (
    cls
    echo %RED%Réinitialisation des bases de données...%RESET%
    docker-compose down
    echo %YELLOW%Les données ont été perdues. À relancer: launch-workrh.bat%RESET%
    pause
)

if "!adv_choice!"=="6" (
    cls
    echo %YELLOW%Nettoyage des caches...%RESET%
    npm cache clean --force
    call mvn clean
    docker system prune -a -f
    echo %GREEN%Caches nettoyés !%RESET%
    pause
)

if not "!adv_choice!"=="0" goto advanced_menu

goto menu

REM ============================================
REM QUITTER
REM ============================================
:quit
cls
echo.
echo %GREEN%Merci d'avoir utilisé WorkRH !%RESET%
echo.
echo Au revoir ! 👋
echo.
timeout /t 2 /nobreak
exit /b 0
