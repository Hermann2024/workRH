#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$appUrl = "http://localhost:4200"
$sourcePng = Join-Path $repoRoot "frontend\angular-app\src\assets\icons\icon-512x512.png"
$iconPath = Join-Path $repoRoot "frontend\angular-app\src\assets\icons\workrh.ico"
$desktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "WorkRH.lnk"
$startMenuDir = Join-Path ([Environment]::GetFolderPath("Programs")) "WorkRH"
$startMenuShortcut = Join-Path $startMenuDir "WorkRH.lnk"

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

function Resolve-Browser {
    $edge = Get-Command msedge -ErrorAction SilentlyContinue
    if ($edge) {
        return @{ Path = $edge.Source; Arguments = "--app=$appUrl" }
    }

    $edgePaths = @(
        "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
        "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
        "$env:LOCALAPPDATA\Microsoft\Edge\Application\msedge.exe"
    )
    foreach ($edgePath in $edgePaths) {
        if (Test-Path $edgePath) {
            return @{ Path = $edgePath; Arguments = "--app=$appUrl" }
        }
    }

    $chrome = Get-Command chrome -ErrorAction SilentlyContinue
    if ($chrome) {
        return @{ Path = $chrome.Source; Arguments = "--app=$appUrl" }
    }

    $chromePaths = @(
        "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
        "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
        "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
    )
    foreach ($chromePath in $chromePaths) {
        if (Test-Path $chromePath) {
            return @{ Path = $chromePath; Arguments = "--app=$appUrl" }
        }
    }

    return @{ Path = "$env:WINDIR\explorer.exe"; Arguments = $appUrl }
}

function New-DirectShortcut([string]$shortcutPath, [hashtable]$browser) {
    $wshShell = New-Object -ComObject WScript.Shell
    $shortcut = $wshShell.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = $browser.Path
    $shortcut.Arguments = $browser.Arguments
    $shortcut.WorkingDirectory = $repoRoot
    $shortcut.IconLocation = $iconPath
    $shortcut.Description = "Ouvrir WorkRH"
    $shortcut.Save()
}

Convert-PngToIco $sourcePng $iconPath
New-Item -ItemType Directory -Force -Path $startMenuDir | Out-Null

$browser = Resolve-Browser
New-DirectShortcut $desktopShortcut $browser
New-DirectShortcut $startMenuShortcut $browser

Write-Host "Icone WorkRH directe installee."
Write-Host "URL: $appUrl"
Write-Host "Bureau: $desktopShortcut"
Write-Host "Menu Demarrer: $startMenuShortcut"
