# ⚡ Hızlı Çözüm: USB Kurulum Hatası

## 🔍 Sorun
Daha önce çalışıyordu ama şimdi "INSTALL_FAILED_USER_RESTRICTED" hatası veriyor.

## ✅ Hızlı Çözüm (2 Dakika)

### Adım 1: Developer Options Kontrolü

1. **Developer Options Açık mı?**
   - `Ayarlar` → `Ek ayarlar` → `Geliştirici seçenekleri`
   - Eğer görünmüyorsa:
     - `Ayarlar` → `Cihaz Hakkında` → `MIUI Sürümü`'ne **7 kez tıklayın**

2. **USB Kurulum İzni Açık mı?**
   - `Geliştirici seçenekleri` içinde
   - **"USB kurulumunu etkinleştir"** (USB Installation) seçeneğini bulun
   - **AÇ** konumuna getirin
   - Onay verin

### Adım 2: USB Debugging Kontrolü

- `Geliştirici seçenekleri` içinde
- **"USB hata ayıklama"** (USB Debugging) → **AÇ**

### Adım 3: Tekrar Dene

Android Studio'da **Run** butonuna basın veya:
```bash
adb install app\build\outputs\apk\debug\app-debug.apk
```

## 🔄 Neden Olabilir?

1. **MIUI Güncellemesi** - Ayarlar sıfırlanmış olabilir
2. **Cihaz Yeniden Başlatma** - Developer Options kapanmış olabilir
3. **Güvenlik Güncellemesi** - USB kurulum izni kapanmış olabilir
4. **Ayarlar Sıfırlama** - Factory reset veya ayar sıfırlama yapılmış olabilir

## 🎯 En Hızlı Alternatif

Eğer Developer Options'ı açmak istemiyorsanız:

1. APK dosyasını bulun:
   ```
   app\build\outputs\apk\debug\app-debug.apk
   ```

2. Cihaza kopyalayın (USB, Bluetooth, WhatsApp)

3. Cihazda Dosya Yöneticisi → APK'yı aç → Yükle

Bu yöntem Developer Options gerektirmez!

