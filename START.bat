

@echo off
REM ============================================
REM Quick launcher - double-clic et c'est parti !
REM ============================================

cls
echo.
echo     WorkRH Platform - Lancement rapide
echo     =================================
echo.
echo     Lancement en cours...
echo.

REM Vérifier que nous sommes dans le bon répertoire
if not exist "launch-workrh.bat" (
    echo ERREUR: Ce fichier doit être dans le répertoire racine de WorkRH
    pause
    exit /b 1
)

REM Lancer le menu
call workrh-menu.bat
