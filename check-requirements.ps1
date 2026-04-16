#!/usr/bin/env pwsh
# ============================================
# WorkRH - Vérification des prérequis
# ============================================

$ErrorActionPreference = "SilentlyContinue"

# Couleurs
$colors = @{
    Green  = "`e[92m"
    Yellow = "`e[93m"
    Red    = "`e[91m"
    Blue   = "`e[94m"
    Reset  = "`e[0m"
}

function Check-Command([string]$cmd, [string]$name, [string]$url = "") {
    if (Get-Command $cmd -ErrorAction SilentlyContinue) {
        $version = & $cmd --version 2>$null | Select-Object -First 1
        Write-Host "$($colors.Green)  ✓ $name$($colors.Reset): $version"
        return $true
    } else {
        Write-Host "$($colors.Red)  ✗ $name n'est pas installé$($colors.Reset)"
        if ($url) {
            Write-Host "$($colors.Yellow)  → Installer depuis: $url$($colors.Reset)"
        }
        return $false
    }
}

function Check-File([string]$path, [string]$name) {
    if (Test-Path $path) {
        Write-Host "$($colors.Green)  ✓ $name trouvé$($colors.Reset)"
        return $true
    } else {
        Write-Host "$($colors.Red)  ✗ $name manquant$($colors.Reset)"
        return $false
    }
}

# Header
Write-Host ""
Write-Host "$($colors.Blue)=================================================$($colors.Reset)"
Write-Host "$($colors.Blue)  WorkRH - Vérification des prérequis$($colors.Reset)"
Write-Host "$($colors.Blue)=================================================$($colors.Reset)"
Write-Host ""

$results = @()

# 1. Docker
Write-Host "$($colors.Blue)[1/5]$($colors.Reset) Vérification Docker..."
$results += Check-Command "docker" "Docker" "https://www.docker.com/products/docker-desktop"

# 2. Docker Compose
Write-Host ""
Write-Host "$($colors.Blue)[2/5]$($colors.Reset) Vérification Docker Compose..."
$results += Check-Command "docker-compose" "Docker Compose" "https://docs.docker.com/compose/install/"

# 3. Java/Maven
Write-Host ""
Write-Host "$($colors.Blue)[3/5]$($colors.Reset) Vérification Java et Maven..."
$results += Check-Command "java" "Java 17+" "https://adoptium.net/"
$results += Check-Command "mvn" "Maven" "https://maven.apache.org/"

# 4. Node.js/npm
Write-Host ""
Write-Host "$($colors.Blue)[4/5]$($colors.Reset) Vérification Node.js et npm..."
$results += Check-Command "node" "Node.js 18+" "https://nodejs.org/"
$results += Check-Command "npm" "npm"

# 5. Fichiers nécessaires
Write-Host ""
Write-Host "$($colors.Blue)[5/5]$($colors.Reset) Vérification des fichiers de configuration..."
$results += Check-File "infra\docker\docker-compose.yml" "docker-compose.yml"
$results += Check-File "pom.xml" "pom.xml"
$results += Check-File "frontend\angular-app\package.json" "package.json"

# Résumé
Write-Host ""
Write-Host "$($colors.Blue)=================================================$($colors.Reset)"

$passed = @($results | Where-Object { $_ -eq $true }).Count
$total = $results.Count

if ($passed -eq $total) {
    Write-Host "$($colors.Green)  ✓ Tous les prérequis sont ok !$($colors.Reset)"
    Write-Host "$($colors.Green)=================================================$($colors.Reset)"
    Write-Host ""
    Write-Host "$($colors.Green)Tu peux maintenant lancer:$($colors.Reset)"
    Write-Host ""
    Write-Host "  .\launch-workrh.ps1"
    Write-Host ""
} else {
    Write-Host "$($colors.Red)  ✗ $passed/$total vérifications ok$($colors.Reset)"
    Write-Host "$($colors.Red)=================================================$($colors.Reset)"
    Write-Host ""
    Write-Host "$($colors.Yellow)Veuillez installer les outils manquants avant de continuer$($colors.Reset)"
    Write-Host ""
}
