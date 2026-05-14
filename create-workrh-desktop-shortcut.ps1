#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$launcher = Join-Path $repoRoot "launch-workrh.ps1"
$sourcePng = Join-Path $repoRoot "frontend\angular-app\src\assets\icons\icon-512x512.png"
$iconPath = Join-Path $repoRoot "frontend\angular-app\src\assets\icons\workrh.ico"
$desktopPath = [Environment]::GetFolderPath("Desktop")
$shortcutPath = Join-Path $desktopPath "WorkRH.lnk"

if (-not (Test-Path $launcher)) {
    throw "Launcher introuvable: $launcher"
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

Convert-PngToIco $sourcePng $iconPath

$pwshCommand = Get-Command pwsh -ErrorAction SilentlyContinue
if ($pwshCommand) {
    $powershell = $pwshCommand.Source
} else {
    $powershell = (Get-Command powershell -ErrorAction Stop).Source
}

$wshShell = New-Object -ComObject WScript.Shell
$shortcut = $wshShell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $powershell
$shortcut.Arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$launcher`""
$shortcut.WorkingDirectory = $repoRoot
$shortcut.IconLocation = $iconPath
$shortcut.Description = "Lancer WorkRH en local"
$shortcut.Save()

Write-Host "Raccourci cree: $shortcutPath"
Write-Host "Icone utilisee: $iconPath"
