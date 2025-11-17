# APK Build Script
Write-Host "🚀 APK oluşturuluyor..." -ForegroundColor Green

# Proje dizinine git
$projectPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectPath

# Build komutu
Write-Host "`n📦 Debug APK build ediliyor..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Build başarılı!" -ForegroundColor Green
    $apkPath = Join-Path $projectPath "app\build\outputs\apk\debug\app-debug.apk"
    
    if (Test-Path $apkPath) {
        $apkSize = (Get-Item $apkPath).Length / 1MB
        Write-Host "`n📱 APK Konumu: $apkPath" -ForegroundColor Cyan
        Write-Host "📊 APK Boyutu: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Cyan
        
        # Klasörü aç
        Write-Host "`n📂 APK klasörünü açıyorum..." -ForegroundColor Yellow
        Start-Process explorer.exe -ArgumentList "/select,`"$apkPath`""
    } else {
        Write-Host "`n❌ APK bulunamadı!" -ForegroundColor Red
    }
} else {
    Write-Host "`n❌ Build başarısız!" -ForegroundColor Red
    Write-Host "Hata loglarını kontrol edin." -ForegroundColor Yellow
}

