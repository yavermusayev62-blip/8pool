# Build Temizleme Script
Write-Host "🧹 Build temizleniyor..." -ForegroundColor Yellow

# Proje dizinine git
$projectPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectPath

# Gradle clean komutu
Write-Host "`n📦 Gradle clean çalıştırılıyor..." -ForegroundColor Cyan
.\gradlew.bat clean

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Build temizlendi!" -ForegroundColor Green
    
    # Build klasörlerini kontrol et
    $appBuildPath = Join-Path $projectPath "app\build"
    $rootBuildPath = Join-Path $projectPath "build"
    
    if (Test-Path $appBuildPath) {
        Write-Host "⚠️  app\build klasörü hala mevcut, manuel olarak silinebilir." -ForegroundColor Yellow
    }
    if (Test-Path $rootBuildPath) {
        Write-Host "⚠️  build klasörü hala mevcut, manuel olarak silinebilir." -ForegroundColor Yellow
    }
} else {
    Write-Host "`n❌ Clean başarısız!" -ForegroundColor Red
    Write-Host "Hata loglarını kontrol edin." -ForegroundColor Yellow
}

