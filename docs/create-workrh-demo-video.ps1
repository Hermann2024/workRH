$ErrorActionPreference = "Stop"

$pptxPath = Join-Path $PSScriptRoot "workrh-demo-presentation.pptx"
$videoPath = Join-Path $PSScriptRoot "workrh-demo-presentation.mp4"

function Rgb([int]$r, [int]$g, [int]$b) {
    return $r + ($g * 256) + ($b * 65536)
}

function Add-Text($slide, [string]$text, [single]$left, [single]$top, [single]$width, [single]$height, [int]$size, [int]$color, [bool]$bold = $false) {
    $shape = $slide.Shapes.AddTextbox(1, $left, $top, $width, $height)
    $shape.TextFrame.TextRange.InsertAfter($text) | Out-Null
    $shape.TextFrame.TextRange.Font.Name = "Aptos"
    $shape.TextFrame.TextRange.Font.Size = $size
    $shape.TextFrame.TextRange.Font.Color.RGB = $color
    $shape.TextFrame.TextRange.Font.Bold = [int]$bold
    $shape.TextFrame.MarginLeft = 0
    $shape.TextFrame.MarginRight = 0
    $shape.TextFrame.MarginTop = 0
    $shape.TextFrame.MarginBottom = 0
    return $shape
}

function Add-Pill($slide, [string]$text, [single]$left, [single]$top, [single]$width, [int]$fillColor, [int]$textColor) {
    $pill = $slide.Shapes.AddShape(5, $left, $top, $width, 30)
    $pill.Fill.ForeColor.RGB = $fillColor
    $pill.Line.Visible = 0
    $pill.TextFrame.TextRange.InsertAfter($text) | Out-Null
    $pill.TextFrame.TextRange.Font.Name = "Aptos"
    $pill.TextFrame.TextRange.Font.Size = 12
    $pill.TextFrame.TextRange.Font.Bold = 1
    $pill.TextFrame.TextRange.Font.Color.RGB = $textColor
    $pill.TextFrame.VerticalAnchor = 3
    return $pill
}

function Add-Card($slide, [string]$title, [string]$body, [single]$left, [single]$top, [single]$width, [single]$height) {
    $card = $slide.Shapes.AddShape(5, $left, $top, $width, $height)
    $card.Fill.ForeColor.RGB = Rgb 255 255 255
    $card.Line.ForeColor.RGB = Rgb 220 226 235
    $card.Line.Weight = 1
    Add-Text $slide $title ($left + 22) ($top + 18) ($width - 44) 34 18 (Rgb 22 40 70) $true | Out-Null
    Add-Text $slide $body ($left + 22) ($top + 58) ($width - 44) ($height - 70) 12 (Rgb 78 92 112) $false | Out-Null
    return $card
}

function Add-Slide($presentation, [string]$eyebrow, [string]$title, [string]$subtitle, [string[]]$cards) {
    $slide = $presentation.Slides.Add($presentation.Slides.Count + 1, 12)
    $slide.FollowMasterBackground = $false
    $slide.Background.Fill.ForeColor.RGB = Rgb 247 250 252

    $bar = $slide.Shapes.AddShape(1, 0, 0, 1280, 88)
    $bar.Fill.ForeColor.RGB = Rgb 20 83 120
    $bar.Line.Visible = 0
    Add-Text $slide "WorkRH" 52 25 240 38 24 (Rgb 255 255 255) $true | Out-Null
    Add-Pill $slide $eyebrow 955 28 260 (Rgb 232 246 252) (Rgb 20 83 120) | Out-Null

    Add-Text $slide $title 70 130 820 95 35 (Rgb 18 34 58) $true | Out-Null
    Add-Text $slide $subtitle 72 242 780 92 17 (Rgb 83 98 121) $false | Out-Null

    if ($cards.Count -gt 0) {
        $x = 70
        foreach ($card in $cards) {
            $parts = $card.Split("|", 2)
            Add-Card $slide $parts[0] $parts[1] $x 380 340 205 | Out-Null
            $x += 380
        }
    }

    $footer = $slide.Shapes.AddShape(1, 0, 690, 1280, 30)
    $footer.Fill.ForeColor.RGB = Rgb 234 240 247
    $footer.Line.Visible = 0
    Add-Text $slide "Présentation produit - WorkRH" 70 696 420 18 10 (Rgb 95 110 132) $false | Out-Null
    return $slide
}

$powerPoint = New-Object -ComObject PowerPoint.Application
$powerPoint.Visible = 1
$presentation = $powerPoint.Presentations.Add()
$presentation.PageSetup.SlideWidth = 1280
$presentation.PageSetup.SlideHeight = 720

Add-Slide $presentation "Démo produit" "WorkRH" "Une plateforme RH pensée pour centraliser les absences, les justificatifs, les arrêts maladie et le suivi des salariés frontaliers au Luxembourg." @(
    "Pour les RH|Un tableau de bord unique pour suivre les demandes, repérer les dossiers incomplets et prendre une décision rapidement.",
    "Pour les salariés|Un espace simple pour déclarer une absence, déposer un justificatif et suivre le statut de la demande.",
    "Pour l'entreprise|Moins d'e-mails dispersés, moins d'oublis de documents et une meilleure traçabilité des décisions RH."
) | Out-Null

Add-Slide $presentation "Constat terrain" "Les équipes RH perdent du temps sur des tâches répétitives" "Dans beaucoup d'entreprises, les absences et justificatifs circulent encore par e-mail, tableur ou dossier partagé. Cela crée des relances manuelles et des risques d'erreur." @(
    "Justificatifs dispersés|Un certificat médical ou un document familial peut rester dans une boîte mail, sans lien clair avec la demande d'absence.",
    "Suivi difficile|La RH doit vérifier à la main qui a envoyé quoi, quel dossier est complet et quelle demande peut être validée.",
    "Risque de validation|Une absence sensible peut être approuvée alors que le justificatif obligatoire n'a pas encore été reçu."
) | Out-Null

Add-Slide $presentation "Parcours salarié" "Déclarer une absence en quelques étapes" "Le salarié choisit le type d'absence, renseigne la période, ajoute un commentaire si nécessaire et dépose le justificatif lorsque WorkRH l'exige." @(
    "Absences concernées|Mariage, naissance, adoption, deuil, rendez-vous médical, formation, démarche administrative ou assistance familiale.",
    "Pièce jointe guidée|Le salarié voit clairement si le justificatif est obligatoire et peut déposer un PDF, JPG ou PNG.",
    "Statut visible|Une fois la demande envoyée, l'employé retrouve son historique, le statut et le document rattaché."
) | Out-Null

Add-Slide $presentation "Arrêts maladie" "Le justificatif médical est intégré au workflow" "Pour une absence maladie, WorkRH demande directement le justificatif médical au moment de la déclaration afin que le dossier arrive complet côté RH." @(
    "Déclaration simple|Le salarié indique la date de début, la date de fin et ajoute son certificat ou son document médical.",
    "Historique salarié|Le justificatif peut être consulté, remplacé ou ajouté depuis l'espace personnel.",
    "Vue RH|La RH voit immédiatement si le justificatif est présent ou manquant et peut le télécharger depuis le tableau."
) | Out-Null

Add-Slide $presentation "Vue RH" "Un tableau de validation clair" "Côté RH, chaque demande affiche le salarié, le type d'absence, la période, le statut, le commentaire et l'état du justificatif." @(
    "Dossier complet|Si le justificatif est présent, la RH peut le télécharger directement depuis la ligne de la demande.",
    "Dossier incomplet|Si le justificatif manque, WorkRH affiche un statut clair pour éviter les validations prématurées.",
    "Décision sécurisée|Pour les absences qui l'exigent, l'approbation est bloquée tant que le justificatif n'est pas déposé."
) | Out-Null

Add-Slide $presentation "Frontaliers Luxembourg" "Suivre le télétravail et les seuils sensibles" "WorkRH aide aussi les équipes RH à suivre les jours travaillés hors Luxembourg, un sujet important pour les salariés frontaliers." @(
    "Déclarations structurées|Pays, date, minutes travaillées, télétravail à résidence ou activité dans un autre pays.",
    "Alertes et seuils|Le tableau RH met en évidence les situations qui demandent une attention particulière.",
    "Traçabilité|Les historiques facilitent les contrôles internes, les échanges avec la paie et la préparation des dossiers."
) | Out-Null

Add-Slide $presentation "Bénéfices" "Moins d'e-mails, moins d'oublis, plus de visibilité" "WorkRH ne cherche pas à remplacer toute la fonction RH. Le produit sécurise les workflows quotidiens qui créent le plus de friction." @(
    "Gain de temps|Réduction des relances manuelles et des recherches de documents dans les e-mails.",
    "Meilleur contrôle|Les dossiers incomplets sont visibles immédiatement, avant validation.",
    "Expérience simple|Les salariés disposent d'un espace clair et les RH gardent une vue consolidée."
) | Out-Null

Add-Slide $presentation "Mise en place" "Un onboarding progressif pour limiter le risque" "Le déploiement peut commencer avec une équipe pilote, quelques salariés et les cas RH les plus fréquents." @(
    "1. Cadrage|Identifier les types d'absence, les justificatifs obligatoires, les rôles RH et les salariés pilotes.",
    "2. Paramétrage|Créer le tenant, les comptes, les accès et les premières données salariés.",
    "3. Pilote|Tester pendant 30 jours, mesurer les gains, corriger les irritants puis élargir le périmètre."
) | Out-Null

Add-Slide $presentation "Prochaine étape" "Proposer un pilote de 30 jours" "La meilleure manière de vendre WorkRH est de faire tester le workflow sur des cas réels : une absence, un arrêt maladie, un justificatif et une validation RH." @(
    "Démo courte|Montrer le parcours salarié, le justificatif, le dashboard RH et le téléchargement du document.",
    "Pilote ciblé|Démarrer avec 5 à 20 salariés et une personne RH référente.",
    "Décision factuelle|Si le produit réduit les relances et clarifie les dossiers, le passage payant devient naturel."
) | Out-Null

foreach ($slide in $presentation.Slides) {
    $slide.SlideShowTransition.AdvanceOnTime = -1
    $slide.SlideShowTransition.AdvanceTime = 7
}

if (Test-Path $pptxPath) {
    Remove-Item -LiteralPath $pptxPath -Force
}
if (Test-Path $videoPath) {
    Remove-Item -LiteralPath $videoPath -Force
}

$presentation.SaveAs($pptxPath, 24)
$presentation.CreateVideo($videoPath, $true, 7, 720, 24, 85)

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

Write-Output "Created: $pptxPath"
Write-Output "Created: $videoPath"
