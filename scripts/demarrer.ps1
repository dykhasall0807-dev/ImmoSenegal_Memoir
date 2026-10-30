# Lance toute l'application sous Windows (PowerShell). Prérequis : Docker Desktop démarré.
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
$fichier = '.env.complet'
$nouveau = $false

function Nouveau-Secret([int]$octets) {
    $b = New-Object byte[] $octets
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($b)
    -join ($b | ForEach-Object { $_.ToString('x2') })
}

if (-not (Test-Path $fichier)) {
    # Secrets aléatoires propres à CET ordinateur : jamais dans Git (le fichier est ignoré).
    $texte = "DB_PASSWORD=$(Nouveau-Secret 16)`nJWT_SECRET=$(Nouveau-Secret 48)`nBOOTSTRAP_ADMIN_EMAIL=admin@immosenegal.local`nBOOTSTRAP_ADMIN_PASSWORD=$(Nouveau-Secret 12)`n"
    [System.IO.File]::WriteAllText((Join-Path (Get-Location) $fichier), $texte)
    $nouveau = $true
}

docker compose -f docker-compose.complet.yml --env-file $fichier up --build -d
if ($LASTEXITCODE -ne 0) { throw "Docker n'a pas pu démarrer l'application." }

Write-Host "Attente du démarrage (1 à 2 minutes la première fois)..."
for ($i = 0; $i -lt 90; $i++) {
    try {
        Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/api/health' -TimeoutSec 3 | Out-Null
        Write-Host "Application prête : http://localhost:8080"
        Write-Host "Documentation de l'API : http://localhost:8080/swagger-ui.html"
        if ($nouveau) { Write-Host "Compte administrateur créé : voir BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD dans le fichier $fichier" }
        exit 0
    } catch { Start-Sleep -Seconds 2 }
}
Write-Error "L'application n'a pas démarré à temps. Voir : docker compose -f docker-compose.complet.yml logs api"
