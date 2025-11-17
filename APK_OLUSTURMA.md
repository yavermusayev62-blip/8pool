# 📦 APK Oluşturma Rehberi

8 Ball Pool Mod Menu uygulamasını APK olarak build etmek için aşağıdaki yöntemlerden birini kullanabilirsiniz.

## Yöntem 1: Android Studio ile (Önerilen)

### Adımlar:

1. **Android Studio'yu açın** ve projeyi açın

2. **Build menüsünden**:
   - `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - Veya `Build` → `Generate Signed Bundle / APK` → `APK` seçin

3. **Build tamamlandığında**:
   - Android Studio alt kısmında bir bildirim görünecek
   - `locate` linkine tıklayın veya şu klasöre gidin:
   ```
   YeniProje/app/build/outputs/apk/debug/app-debug.apk
   ```

4. **APK'yı cihaza yükleyin**:
   - APK dosyasını telefonunuza kopyalayın
   - Telefonda "Bilinmeyen kaynaklardan yükleme" iznini açın
   - APK dosyasına tıklayarak yükleyin

## Yöntem 2: Gradle Komutları ile (Terminal)

### Windows PowerShell:

```powershell
cd "C:\Users\nesib\OneDrive\Masaüstü\8 pool\YeniProje"
.\gradlew.bat assembleDebug
```

### APK Konumu:
Build tamamlandıktan sonra APK şurada olacak:
```
YeniProje/app/build/outputs/apk/debug/app-debug.apk
```

## Yöntem 3: Release APK (İmzalı)

Release APK oluşturmak için:

1. **KeyStore oluşturma** (ilk kez):
   ```powershell
   keytool -genkey -v -keystore poolmod-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias poolmod
   ```

2. **app/build.gradle** dosyasına signing config ekleyin:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('poolmod-keystore.jks')
               storePassword 'your_password'
               keyAlias 'poolmod'
               keyPassword 'your_password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
           }
       }
   }
   ```

3. **Release APK build edin**:
   ```powershell
   .\gradlew.bat assembleRelease
   ```

4. **APK Konumu**:
   ```
   YeniProje/app/build/outputs/apk/release/app-release.apk
   ```

## Hızlı Komutlar

### Debug APK:
```powershell
cd "YeniProje"
.\gradlew.bat assembleDebug
```

### Release APK:
```powershell
cd "YeniProje"
.\gradlew.bat assembleRelease
```

### Temizle ve Build:
```powershell
cd "YeniProje"
.\gradlew.bat clean assembleDebug
```

## APK Boyutu Optimizasyonu

APK boyutunu küçültmek için:

1. **ProGuard/R8** kullanın (zaten aktif)
2. **Gereksiz kaynakları kaldırın**
3. **APK Split** kullanın (farklı ABI'ler için)

## Sorun Giderme

### Build Hatası:
- `gradlew.bat clean` çalıştırın
- Android Studio'da `File` → `Invalidate Caches / Restart`

### APK Bulunamıyor:
- `app/build/outputs/apk/` klasörünü kontrol edin
- Build loglarını kontrol edin

### Yükleme Hatası:
- "Bilinmeyen kaynaklardan yükleme" iznini açın
- Eski versiyonu kaldırın, sonra yeni APK'yı yükleyin

## Notlar

- **Debug APK**: Test için kullanılır, imzasız
- **Release APK**: Dağıtım için kullanılır, imzalı
- İlk build biraz uzun sürebilir (5-10 dakika)
- Sonraki build'ler daha hızlı olur (1-2 dakika)

