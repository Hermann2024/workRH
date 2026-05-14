$ErrorActionPreference = "Stop"

$pptxPath = Join-Path $PSScriptRoot "workrh-demo-presentation.pptx"
$videoPath = Join-Path $PSScriptRoot "workrh-demo-presentation.mp4"

if (!(Test-Path $pptxPath)) {
    throw "Missing PPTX: $pptxPath"
}
if (Test-Path $videoPath) {
    Remove-Item -LiteralPath $videoPath -Force
}

$powerPoint = New-Object -ComObject PowerPoint.Application
$powerPoint.Visible = 1
$presentation = $powerPoint.Presentations.Open($pptxPath, $false, $false, $false)

foreach ($slide in $presentation.Slides) {
    $slide.SlideShowTransition.AdvanceOnTime = -1
    $slide.SlideShowTransition.AdvanceTime = 6
}

$presentation.CreateVideo($videoPath, $true, 6, 720, 24, 85)

$deadline = (Get-Date).AddMinutes(15)
while (($presentation.CreateVideoStatus -eq 1 -or $presentation.CreateVideoStatus -eq 2) -and (Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
}

$status = $presentation.CreateVideoStatus
$presentation.Close()
$powerPoint.Quit()

[System.Runtime.InteropServices.Marshal]::ReleaseComObject($presentation) | Out-Null
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($powerPoint) | Out-Null

if ($status -ne 3 -or !(Test-Path $videoPath)) {
    throw "PowerPoint video export did not complete successfully. Status: $status"
}

Write-Output "Created: $videoPath"
