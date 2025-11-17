# GitHub'a kod gönderme scripti
# Kullanım: .\push_to_github.ps1

Write-Host "=== GitHub'a Kod Gönderme ===" -ForegroundColor Cyan

# Git kontrolü
$gitInstalled = $false
try {
    $gitVersion = git --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        $gitInstalled = $true
        Write-Host "✓ Git bulundu: $gitVersion" -ForegroundColor Green
    }
} catch {
    $gitInstalled = $false
}

if (-not $gitInstalled) {
    Write-Host "`n❌ Git yüklü değil!" -ForegroundColor Red
    Write-Host "`nGit'i yüklemek için:" -ForegroundColor Yellow
    Write-Host "1. https://git-scm.com/download/win adresinden Git'i indirin" -ForegroundColor Yellow
    Write-Host "2. Veya PowerShell'de (yönetici olarak):" -ForegroundColor Yellow
    Write-Host "   winget install --id Git.Git -e --source winget" -ForegroundColor Cyan
    Write-Host "`nGit yüklendikten sonra PowerShell'i yeniden başlatın ve bu scripti tekrar çalıştırın." -ForegroundColor Yellow
    exit 1
}

# Mevcut dizin kontrolü
$projectDir = Get-Location
Write-Host "`nProje dizini: $projectDir" -ForegroundColor Cyan

# Git repository kontrolü
if (-not (Test-Path ".git")) {
    Write-Host "`nGit repository başlatılıyor..." -ForegroundColor Yellow
    git init
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Git init başarısız!" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Git repository başlatıldı" -ForegroundColor Green
} else {
    Write-Host "✓ Git repository mevcut" -ForegroundColor Green
}

# Remote kontrolü
$remoteUrl = "https://github.com/yavermusayev62-blip/8pool.git"
$currentRemote = git remote get-url origin 2>&1

if ($LASTEXITCODE -ne 0 -or $currentRemote -ne $remoteUrl) {
    Write-Host "`nRemote repository ayarlanıyor..." -ForegroundColor Yellow
    if ($LASTEXITCODE -eq 0) {
        git remote set-url origin $remoteUrl
    } else {
        git remote add origin $remoteUrl
    }
    Write-Host "✓ Remote repository ayarlandı: $remoteUrl" -ForegroundColor Green
} else {
    Write-Host "✓ Remote repository zaten ayarlı" -ForegroundColor Green
}

# Dosyaları ekle
Write-Host "`nDeğişiklikler kontrol ediliyor..." -ForegroundColor Yellow
git add .
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Git add başarısız!" -ForegroundColor Red
    exit 1
}

$status = git status --short
if ([string]::IsNullOrWhiteSpace($status)) {
    Write-Host "✓ Yeni değişiklik yok" -ForegroundColor Green
} else {
    Write-Host "`nEklenen dosyalar:" -ForegroundColor Cyan
    git status --short | ForEach-Object { Write-Host "  $_" }
}

# Commit
Write-Host "`nCommit yapılıyor..." -ForegroundColor Yellow
$commitMessage = "Initial commit: 8 Ball Pool Mod Menu"
git commit -m $commitMessage
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠ Commit başarısız (muhtemelen yeni değişiklik yok)" -ForegroundColor Yellow
} else {
    Write-Host "✓ Commit yapıldı: $commitMessage" -ForegroundColor Green
}

# Branch kontrolü
$currentBranch = git branch --show-current
if ([string]::IsNullOrWhiteSpace($currentBranch)) {
    git branch -M main
    $currentBranch = "main"
}

Write-Host "`nMevcut branch: $currentBranch" -ForegroundColor Cyan

# Push
Write-Host "`nGitHub'a gönderiliyor..." -ForegroundColor Yellow
Write-Host "⚠ İlk push için GitHub kullanıcı adı ve şifre/token gerekebilir" -ForegroundColor Yellow
Write-Host "`nPush işlemi başlatılıyor..." -ForegroundColor Cyan

git push -u origin $currentBranch

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Başarılı! Kodlar GitHub'a gönderildi." -ForegroundColor Green
    Write-Host "Repository: https://github.com/yavermusayev62-blip/8pool" -ForegroundColor Cyan
} else {
    Write-Host "`n❌ Push başarısız!" -ForegroundColor Red
    Write-Host "`nOlası nedenler:" -ForegroundColor Yellow
    Write-Host "1. GitHub kimlik doğrulaması gerekebilir" -ForegroundColor Yellow
    Write-Host "2. Personal Access Token kullanmanız gerekebilir" -ForegroundColor Yellow
    Write-Host "3. Repository'ye erişim izniniz olmayabilir" -ForegroundColor Yellow
    Write-Host "`nÇözüm:" -ForegroundColor Cyan
    Write-Host "GitHub'da Settings > Developer settings > Personal access tokens > Tokens (classic)" -ForegroundColor Cyan
    Write-Host "Yeni token oluşturun ve şifre yerine token kullanın" -ForegroundColor Cyan
    exit 1
}

Write-Host "`n=== Tamamlandı ===" -ForegroundColor Green

