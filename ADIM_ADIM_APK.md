# 📱 Android Studio'da APK Oluşturma - Adım Adım

## 🎯 Yöntem 1: Build Menüsü ile (En Kolay)

### Adım 1: Build Menüsünü Aç
1. Android Studio'nun **üst menü çubuğunda** `Build` yazısına tıkla
2. Açılan menüden `Build Bundle(s) / APK(s)` seçeneğine tıkla
3. Alt menüden `Build APK(s)` seçeneğine tıkla

### Adım 2: Build İşlemi
- Android Studio otomatik olarak build işlemini başlatacak
- **Alt kısımdaki "Build" sekmesinde** ilerlemeyi görebilirsin
- İlk build 2-5 dakika sürebilir (bağımlılıklar indirilir)
- Sonraki build'ler daha hızlı olur (30-60 saniye)

### Adım 3: Build Tamamlandı
- Build tamamlandığında **sağ alt köşede** bir bildirim çıkacak:
  ```
  APK(s) generated successfully
  ```
- Bildirimde **"locate"** butonuna tıkla
- Veya **"analyze"** butonuna tıklayarak APK'yı analiz edebilirsin

### Adım 4: APK Konumu
- APK dosyası şu klasörde olacak:
  ```
  YeniProje\app\build\outputs\apk\debug\app-debug.apk
  ```
- Windows Explorer'da bu klasöre gidebilirsin
- APK dosyasını görebilirsin

---

## 🎯 Yöntem 2: Gradle Panel ile

### Adım 1: Gradle Panelini Aç
1. Android Studio'nun **sağ tarafında** "Gradle" sekmesine tıkla
2. Eğer görünmüyorsa: `View` → `Tool Windows` → `Gradle`

### Adım 2: Build Task'ını Bul
1. Gradle panelinde proje adını genişlet: `YeniProje`
2. `app` klasörünü genişlet
3. `Tasks` klasörünü genişlet
4. `build` klasörünü genişlet
5. `assembleDebug` task'ına **çift tıkla**

### Adım 3: Build Tamamlandı
- Build işlemi başlayacak ve alt kısımda ilerleme görünecek
- Tamamlandığında APK hazır olacak

---

## 🎯 Yöntem 3: Terminal ile

### Adım 1: Terminal'i Aç
1. Android Studio'nun **alt kısmında** "Terminal" sekmesine tıkla
2. Veya `View` → `Tool Windows` → `Terminal`

### Adım 2: Komutu Çalıştır
Terminal'de şu komutu yaz ve Enter'a bas:
```powershell
.\gradlew.bat assembleDebug
```

### Adım 3: Build Tamamlandı
- Build işlemi başlayacak
- Tamamlandığında APK hazır olacak

---

## 📦 APK'yı Telefona Yükleme

### Adım 1: APK'yı Bul
- APK dosyası: `YeniProje\app\build\outputs\apk\debug\app-debug.apk`

### Adım 2: Telefona Kopyala
- USB ile bağla ve APK'yı kopyala
- Veya e-posta/WhatsApp ile gönder
- Veya Google Drive'a yükle ve indir

### Adım 3: Telefonda İzin Ver
1. Telefonda **Ayarlar** → **Güvenlik** (veya **Uygulamalar**)
2. **"Bilinmeyen kaynaklardan yükleme"** veya **"Güvenli olmayan kaynaklardan yükleme"** seçeneğini aç
3. Uyarıyı onayla

### Adım 4: APK'yı Yükle
1. APK dosyasına tıkla
2. **"Yükle"** veya **"Install"** butonuna tıkla
3. İzinleri onayla
4. Yükleme tamamlandığında **"Aç"** butonuna tıklayarak uygulamayı başlat

---

## 🔍 Sorun Giderme

### Build Hatası Alırsan:
1. **Clean Project**: `Build` → `Clean Project`
2. **Rebuild**: `Build` → `Rebuild Project`
3. **Invalidate Caches**: `File` → `Invalidate Caches / Restart` → `Invalidate and Restart`

### APK Bulunamıyor:
- `app\build\outputs\apk\debug\` klasörünü kontrol et
- Build'in başarılı olduğundan emin ol (Build sekmesinde hata var mı kontrol et)

### Gradle Sync Hatası:
1. `File` → `Sync Project with Gradle Files`
2. İnternet bağlantını kontrol et
3. Android Studio'yu yeniden başlat

---

## 💡 İpuçları

- ✅ İlk build uzun sürer (2-5 dakika) - sabırlı ol
- ✅ Sonraki build'ler çok daha hızlı (30-60 saniye)
- ✅ Build sırasında internet bağlantısı gerekli (bağımlılıklar indirilir)
- ✅ APK boyutu genellikle 5-15 MB arası olur
- ✅ Debug APK test için, Release APK dağıtım için kullanılır

---

## 🎉 Başarılı!

APK başarıyla oluşturuldu! Artık telefona yükleyebilirsin.

