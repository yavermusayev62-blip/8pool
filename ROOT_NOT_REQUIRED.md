# ✅ Root Gerektirmez

Bu uygulama **root gerektirmez** ve root'suz cihazlarda da çalışır.

## 🔒 Kullanılan İzinler (Root Gerektirmiyor)

1. **SYSTEM_ALERT_WINDOW** - Overlay göstermek için
   - Ayarlardan manuel olarak verilir
   - Root gerektirmez

2. **FOREGROUND_SERVICE** - Arka planda çalışmak için
   - Standart Android izni
   - Root gerektirmez

3. **FOREGROUND_SERVICE_MEDIA_PROJECTION** - Ekran yakalama için
   - Standart Android izni
   - Root gerektirmez

4. **QUERY_ALL_PACKAGES** - Oyun paketini tespit etmek için
   - Standart Android izni
   - Root gerektirmez

5. **MediaProjection** - Ekran görüntüsü almak için
   - İlk kullanımda kullanıcıdan izin istenir
   - Root gerektirmez

## 🚫 Root Kullanılmıyor

- ❌ Root erişimi yok
- ❌ System dosyalarına yazma yok
- ❌ SU komutları kullanılmıyor
- ❌ Root kontrolü yok

## ✅ Root'suz Cihazlarda Çalışır

Uygulama tamamen root'suz cihazlarda çalışacak şekilde tasarlanmıştır:

- Overlay için `SYSTEM_ALERT_WINDOW` izni kullanılır
- Ekran yakalama için `MediaProjection` API kullanılır
- Tüm işlemler standart Android API'leri ile yapılır

## 📱 Gereksinimler

- Android 5.0 (API 21) veya üzeri
- Overlay izni (ayarlardan manuel)
- Ekran yakalama izni (ilk kullanımda)

**Root gerektirmez!** ✅

