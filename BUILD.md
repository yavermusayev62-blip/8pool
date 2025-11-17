# 🚀 Build Talimatları

## Windows'ta Build Etme

### 1. Terminal'de Proje Klasörüne Git

```powershell
cd "C:\Users\nesib\OneDrive\Masaüstü\8 pool\YeniProje"
```

### 2. Gradle Wrapper ile Build

```powershell
.\gradlew.bat assembleDebug
```

### 3. APK Konumu

Build tamamlandıktan sonra APK şu konumda olacak:
```
app\build\outputs\apk\debug\app-debug.apk
```

## Android Studio'da Build Etme

### 1. Projeyi Aç
- Android Studio'yu aç
- `File > Open` seçeneğini tıkla
- `YeniProje` klasörünü seç

### 2. Gradle Sync
- Android Studio otomatik olarak Gradle sync yapacak
- Eğer yapmazsa: `File > Sync Project with Gradle Files`

### 3. APK Build
- `Build > Build Bundle(s) / APK(s) > Build APK(s)` seçeneğini tıkla
- Build tamamlandığında bildirim gelecek
- `locate` butonuna tıklayarak APK konumuna gidebilirsin

### 4. Release APK (Obfuscated)
- `Build > Generate Signed Bundle / APK` seçeneğini tıkla
- APK seçeneğini seç
- Key store oluştur veya mevcut olanı kullan
- Release build type'ı seç
- Build tamamlandığında APK hazır olacak

## Terminal Komutları

### Debug APK
```powershell
.\gradlew.bat assembleDebug
```

### Release APK
```powershell
.\gradlew.bat assembleRelease
```

### Clean Build
```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

### Build ve Install (USB Debugging ile)
```powershell
.\gradlew.bat installDebug
```

## Sorun Giderme

### Gradle Sync Hatası
```powershell
.\gradlew.bat clean
.\gradlew.bat --refresh-dependencies
```

### Build Hatası
1. Android Studio'da `File > Invalidate Caches / Restart` yap
2. `Build > Clean Project` yap
3. `Build > Rebuild Project` yap

### Gradle Wrapper Bulunamadı
Android Studio'da projeyi açtığında otomatik oluşturulur. Eğer yoksa:
```powershell
gradle wrapper
```

## APK Yükleme

### USB ile Yükleme
1. Telefonda USB Debugging'i aç
2. USB ile bağla
3. `.\gradlew.bat installDebug` komutunu çalıştır

### Manuel Yükleme
1. APK dosyasını telefona kopyala
2. Telefonda `Bilinmeyen Kaynaklardan Yükleme` iznini ver
3. APK dosyasına tıkla ve yükle

## Build Süresi

- İlk build: ~2-5 dakika (bağımlılıklar indirilir)
- Sonraki buildler: ~30-60 saniye

## Notlar

- İlk build'de internet bağlantısı gerekli (bağımlılıklar indirilir)
- Java 8 veya üzeri gerekli
- Android SDK gerekli (Android Studio ile otomatik kurulur)

