
# 🎱 8 Ball Pool Mod Menu

8 Ball Pool oyunu için gelişmiş mod menu uygulaması. Anti-cheat bypass özellikleri ile güvenli kullanım.

## ✨ Özellikler

- ✅ **Otomatik Oyun Tespiti** - Cihazınızdaki 8 Ball Pool'u otomatik bulur
- ✅ **Oyun Başlatma** - Tek tıkla oyunu başlatır
- ✅ **Oyun İçi Mod Menu** - Floating overlay ile oyun içinde mod menüsü
- ✅ **Root Gerektirmez** - Root'suz cihazlarda da çalışır
- ✅ **Anti-Cheat Bypass** - Root detection, process hiding ve memory protection
- ✅ **2 Ana Mod** - Auto Aim ve Top Yolu Göster

## 🎮 Mod Özellikleri

1. **🎯 Auto Aim** - Otomatik nişan alma, delikleri tanır ve en iyi hedefi gösterir
2. **📊 Top Yolu Göster** - Ekranı okuyup topların gideceği ve duracağı yerleri tahmin eder, çizgilerle gösterir

### 🎯 Auto Aim Özelliği

- **Delik Tespiti**: Masadaki 6 deliği otomatik tespit eder (4 köşe + 2 kenar)
- **Hedef Seçimi**: En iyi hedef topu ve deliği seçer
- **Aim Hesaplama**: Otomatik olarak açı ve güç hesaplar
- **Görsel Gösterim**: Hedef delik kırmızı ile vurgulanır, aim çizgisi sarı kesikli çizgi ile gösterilir
- **Aim Bilgisi**: Açı ve güç bilgisi beyaz top üzerinde gösterilir
- **Aktif/Deaktif**: Mod menu'den açıp kapatabilirsiniz

### 📊 Top Yolu Göster Özelliği

- **Ekran Okuma**: Oyun ekranını sürekli analiz eder
- **Top Tespiti**: Tüm topları otomatik tespit eder ve numaralandırır
- **Yol Hesaplama**: Fizik simülasyonu ile topların gideceği yolları hesaplar
- **Görsel Gösterim**: Her top için renkli çizgiler ve numaralar gösterir
- **Çarpışma Tahmini**: Topların birbirine çarpacağı noktaları işaretler
- **Delik Gösterimi**: Tüm delikler sarı çemberlerle işaretlenir

## 🛡️ Güçlü Stealth Bypass Sistemi

### 25. Parti Yazılım Tespitini Bypass Eder

- ✅ **Package Manager'dan Gizleme** - Uygulama yüklü uygulamalar listesinde görünmez
- ✅ **Sistem Paketi Gibi Gösterme** - 25. parti yazılım değil, sistem uygulaması gibi görünür
- ✅ **Package Name Spoofing** - Package name'i sistem paketi gibi gösterir
- ✅ **App Signature Spoofing** - İmzayı sistem uygulaması gibi gösterir
- ✅ **Process Name Hiding** - Process name'i gizler
- ✅ **Root detection bypass** - Root tespitini atlatır
- ✅ **Process hiding** - Process'i gizler
- ✅ **Memory protection** - Memory dump koruması
- ✅ **Debugger detection bypass** - Debugger tespitini atlatır
- ✅ **Emulator detection bypass** - Emulator tespitini atlatır
- ✅ **Hook detection bypass** - Xposed, Frida vb. hook tespitini atlatır
- ✅ **Code obfuscation** - Kod obfuscation ile korunur

## 📱 Kullanım

1. **Oyunu Tespit Et** - "🔍 Oyunu Tespit Et" butonuna tıklayın
2. **Oyunu Başlat** - "▶ Oyunu Başlat" butonuna tıklayın
3. **Mod Menu Başlat** - "🎮 Mod Menu Başlat" butonuna tıklayın
4. **Overlay İzni Ver** - İlk kullanımda overlay izni verin
5. **Top Yolu Modunu Aktifleştir** - Mod menu'den "📊 Top Yolu Göster" modunu açın
6. **Ekran Yakalama İzni Ver** - İlk kullanımda ekran yakalama izni verin
7. **Modları Aktifleştir** - Oyun içinde floating menu'den diğer modları açın

### Top Yolu Göster Nasıl Çalışır?

1. Mod aktifleştirildiğinde ekran yakalama başlar
2. Her 0.5 saniyede bir ekran görüntüsü alınır
3. Toplar otomatik tespit edilir ve numaralandırılır
4. Fizik simülasyonu ile yollar hesaplanır
5. Overlay üzerinde renkli çizgiler ve numaralar gösterilir
6. Her top için farklı renk kullanılır
7. Çarpışma noktaları sarı işaretlerle gösterilir

## 🔧 Kurulum ve Build

### Hızlı Build (Terminal)

```powershell
cd "C:\Users\nesib\OneDrive\Masaüstü\8 pool\YeniProje"
.\gradlew.bat assembleDebug
```

APK konumu: `app\build\outputs\apk\debug\app-debug.apk`

### Android Studio'da Build

1. Android Studio'da projeyi açın (`File > Open`)
2. Gradle sync otomatik yapılır (yoksa `File > Sync Project with Gradle Files`)
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)` seçeneğini tıklayın
4. Build tamamlandığında bildirim gelecek, `locate` butonuna tıklayın

### Detaylı Build Talimatları

`BUILD.md` dosyasına bakın.

## ⚙️ İzinler

- **SYSTEM_ALERT_WINDOW** - Overlay göstermek için (ayarlardan manuel)
- **FOREGROUND_SERVICE** - Arka planda çalışmak için
- **FOREGROUND_SERVICE_MEDIA_PROJECTION** - Ekran yakalama için
- **QUERY_ALL_PACKAGES** - Oyun paketini tespit etmek için
- **MediaProjection** - Ekran görüntüsü almak için (ilk kullanımda izin istenir)

## ⚠️ Önemli Notlar

- ✅ **Root gerektirmez** - Root'suz cihazlarda da çalışır
- ✅ **25. Parti Yazılım Tespitini Bypass Eder** - Uygulama sistem paketi gibi görünür
- ✅ **Güçlü Stealth Bypass** - Anti-cheat sistemleri uygulamayı tespit edemez
- Overlay izni ayarlardan manuel olarak verilmelidir
- Oyun başlatıldıktan sonra mod menu otomatik açılır
- Anti-cheat sisteminden korunmak için tüm bypass özellikleri aktif
- Mod özellikleri oyun içinde hook sistemi ile uygulanır
- Tüm izinler standart Android izinleri (root gerektirmiyor)
- Uygulama Package Manager'da sistem uygulaması gibi görünür

## 🚀 Build

```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## 📝 Geliştirme

### Yeni Mod Ekleme

1. `ModMenuConfig.kt` dosyasına yeni mod anahtarı ekleyin:
```kotlin
const val MOD_YENI_MOD = "yeni_mod"
```

2. `ModMenuView.kt` dosyasına yeni switch ekleyin:
```kotlin
addModOption("Yeni Mod", ModMenuConfig.MOD_YENI_MOD)
```

3. `ModHookManager.kt` dosyasına hook fonksiyonu ekleyin:
```kotlin
ModMenuConfig.MOD_YENI_MOD -> enableYeniMod()
```

## 📄 Lisans

Eğitim amaçlıdır. Oyunlarda hile kullanımı oyunun hizmet şartlarını ihlal edebilir.
