# 🔧 USB Kurulum Hatası Çözümü

## ❌ Hata Mesajı
```
INSTALL_FAILED_USER_RESTRICTED
Installation via USB is disabled
```

## 📱 MIUI/Xiaomi Cihazlarda Çözüm

### Yöntem 1: Developer Options'tan USB Kurulum İzni (Önerilen)

1. **Developer Options'ı Aktifleştir:**
   - `Ayarlar` → `Cihaz Hakkında` → `MIUI Sürümü`'ne 7 kez tıklayın
   - "Developer options aktifleştirildi" mesajını görürsünüz

2. **USB Kurulum İznini Aç:**
   - `Ayarlar` → `Ek ayarlar` → `Geliştirici seçenekleri`
   - `USB kurulumunu etkinleştir` (Enable USB Installation) seçeneğini **AÇ**
   - Onay verin

3. **APK'yı Tekrar Yükleyin:**
   - Android Studio'dan Run butonuna basın
   - Veya: `adb install app-debug.apk`

### Yöntem 2: ADB ile Kurulum

1. **Developer Options'ta USB Debugging'i Aç:**
   - `Ayarlar` → `Ek ayarlar` → `Geliştirici seçenekleri`
   - `USB debugging` (USB Hata Ayıklama) → **AÇ**

2. **USB Kurulum İznini Aç:**
   - `USB kurulumunu etkinleştir` → **AÇ**

3. **Komut İstemi'nden Kur:**
   ```bash
   cd "C:\Users\User\Desktop\8 pool\8 pool\YeniProje"
   adb install app\build\intermediates\apk\debug\app-debug.apk
   ```

### Yöntem 3: Manuel APK Transfer (En Kolay)

1. **APK Dosyasını Bulun:**
   ```
   C:\Users\User\Desktop\8 pool\8 pool\YeniProje\app\build\outputs\apk\debug\app-debug.apk
   ```

2. **APK'yı Cihaza Transfer Edin:**
   - USB kabloyla cihazı bilgisayara bağlayın
   - APK dosyasını cihazın İndirilenler klasörüne kopyalayın
   - Veya Bluetooth/WhatsApp ile gönderin

3. **Cihazda Kurun:**
   - Dosya Yöneticisi'ni açın
   - `app-debug.apk` dosyasını bulun
   - Dokunun ve "Yükle" butonuna basın
   - İzin verin ve kurulumu tamamlayın

### Yöntem 4: MIUI Security Ayarları

1. **Güvenlik Merkezi:**
   - `Ayarlar` → `Güvenlik` → `Güvenlik Merkezi`
   - `Uygulama kilidi` → `Bilinmeyen kaynaklardan yükleme` → İzin ver

2. **Özel İzinler:**
   - `Ayarlar` → `Özel izinler` → `Bilinmeyen kaynaklardan yükleme`
   - Kurulum yapacak uygulamayı seçin (Dosya Yöneticisi, vb.)
   - İzni **AÇ**

## ✅ Kontrol

Kurulum başarılı mı kontrol edin:
```bash
adb shell pm list packages | findstr poolmod
```

Çıktı: `package:com.poolmod.menu` görünmeli

## 🚀 Kurulum Sonrası

Uygulama kurulduktan sonra:
1. `Ayarlar` → `Özel izinler` → `Diğer uygulamaların üzerinde görünme`
2. `8 Ball Pool Mod` uygulamasını bulun ve **İZİN VER**
3. Uygulamayı başlatın

## ⚠️ Notlar

- MIUI cihazlarda USB kurulum güvenlik nedeniyle varsayılan olarak kapalıdır
- Developer Options'ı açmadan önce cihazın kilidini açın
- Bazı MIUI sürümlerinde menü yolları farklı olabilir
- Kurulum izni genellikle sadece bir kez verilir, sonra hatırlanır

## 🔍 Alternatif Çözüm

Eğer yukarıdaki yöntemler çalışmazsa:
1. Cihazı yeniden başlatın
2. USB kablosunu değiştirin
3. Farklı bir USB portu deneyin
4. MIUI Security uygulamasını güncelleyin

