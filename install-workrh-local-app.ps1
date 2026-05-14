#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$vbsLauncher = Join-Path $repoRoot "start-workrh-app.vbs"
$sourcePng = Join-Path $repoRoot "frontend\angular-app\src\assets\icons\icon-512x512.png"
$iconPath = Join-Path $repoRoot "frontend\angular-app\src\assets\icons\workrh.ico"
$desktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "WorkRH.lnk"
$startMenuDir = Join-Path ([Environment]::GetFolderPath("Programs")) "WorkRH"
$startMenuShortcut = Join-Path $startMenuDir "WorkRH.lnk"

if (-not (Test-Path $vbsLauncher)) {
    throw "Lanceur introuvable: $vbsLauncher"
}

if (-not (Test-Path $sourcePng)) {
    throw "Logo introuvable: $sourcePng"
}

function Convert-PngToIco([string]$pngPath, [string]$icoPath) {
    $pngBytes = [System.IO.File]::ReadAllBytes($pngPath)
    $iconDir = New-Object byte[] 6
    $iconDir[2] = 1
    $iconDir[4] = 1

    $entry = New-Object byte[] 16
    $entry[0] = 0
    $entry[1] = 0
    $entry[2] = 0
    $entry[3] = 0
    [BitConverter]::GetBytes([UInt32]$pngBytes.Length).CopyTo($entry, 8)
    [BitConverter]::GetBytes([UInt32]22).CopyTo($entry, 12)

    $output = New-Object byte[] (22 + $pngBytes.Length)
    $iconDir.CopyTo($output, 0)
    $entry.CopyTo($output, 6)
    $pngBytes.CopyTo($output, 22)
    [System.IO.File]::WriteAllBytes($icoPath, $output)
}

function New-WorkRhShortcut([string]$shortcutPath) {
    $wshShell = New-Object -ComObject WScript.Shell
    $shortcut = $wshShell.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = "$env:WINDIR\System32\wscript.exe"
    $shortcut.Arguments = "`"$vbsLauncher`""
    $shortcut.WorkingDirectory = $repoRoot
    $shortcut.IconLocation = $iconPath
    $shortcut.Description = "Ouvrir WorkRH"
    $shortcut.Save()
}

Convert-PngToIco $sourcePng $iconPath
New-Item -ItemType Directory -Force -Path $startMenuDir | Out-Null

New-WorkRhShortcut $desktopShortcut
New-WorkRhShortcut $startMenuShortcut

Write-Host "WorkRH est installe comme application locale."
Write-Host "Bureau: $desktopShortcut"
Write-Host "Menu Demarrer: $startMenuShortcut"
