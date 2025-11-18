# Debug Dosyaları Kullanım Kılavuzu

## 📁 Debug Dosyaları Nerede?

Debug log dosyaları uygulamanın internal storage'ında saklanır:
```
/data/data/com.poolmod.menu/files/debug_logs/
```

### Log Dosyaları:

1. **`errors.log`** - Tüm hatalar ve exception'lar
2. **`crashes.log`** - Uygulama çökmeleri (crash'ler)
3. **`debug.log`** - Debug, info ve warning logları

## 🔍 Log Dosyalarına Nasıl Erişilir?

### Yöntem 1: Android Studio Device File Explorer

1. Android Studio'yu açın
2. `View` → `Tool Windows` → `Device File Explorer`
3. Cihazınızı seçin
4. Şu yolu açın: `/data/data/com.poolmod.menu/files/debug_logs/`
5. Log dosyalarını bilgisayarınıza kopyalayın

### Yöntem 2: ADB Komutları

```bash
# Tüm log dosyalarını bilgisayara kopyala
adb pull /data/data/com.poolmod.menu/files/debug_logs/ ./debug_logs/

# Sadece error log'unu kopyala
adb pull /data/data/com.poolmod.menu/files/debug_logs/errors.log ./

# Sadece crash log'unu kopyala
adb pull /data/data/com.poolmod.menu/files/debug_logs/crashes.log ./
```

### Yöntem 3: Uygulama İçinden (Gelecek Özellik)

Uygulama içinde log dosyalarını görüntüleme özelliği eklenebilir.

## 📊 Log Dosyası Formatı

### Error Log Formatı:
```
[2025-11-18 10:15:00.123] [ERROR] [ModMenuService] SecurityException - Overlay izni gerekli
java.lang.SecurityException: ...
    at com.poolmod.menu.ModMenuService.showModMenu(ModMenuService.kt:395)
    ...
```

### Crash Log Formatı:
```
================================================================================
CRASH DETECTED
================================================================================
Time: 2025-11-18 10:15:00.123
Thread: main (1)
Exception: java.lang.NullPointerException
Message: Attempt to invoke virtual method on a null object reference

Stack Trace:
java.lang.NullPointerException: Attempt to invoke virtual method on a null object reference
    at com.poolmod.menu.ModMenuService.toggleMenu(ModMenuService.kt:307)
    ...

Device Info:
  Manufacturer: Xiaomi
  Model: Redmi Note 10
  Android Version: 13 (API 33)
  App Version: 1.0.0 (1)
================================================================================
```

## 🔧 Debug Logger Kullanımı

### Kod İçinde Kullanım:

```kotlin
// Debug log
DebugLogger.logDebug("TAG", "Debug mesajı")

// Info log
DebugLogger.logInfo("TAG", "Bilgi mesajı")

// Warning log
DebugLogger.logWarning("TAG", "Uyarı mesajı")

// Error log
DebugLogger.logError("TAG", "Hata mesajı")

// Exception log
try {
    // Kod
} catch (e: Exception) {
    DebugLogger.logException("TAG", "Açıklama", e)
}
```

## 🧹 Log Dosyalarını Temizleme

### Otomatik Temizleme:
- Log dosyaları 5MB'ı geçtiğinde otomatik olarak rotate edilir
- En fazla 5 eski log dosyası saklanır
- Daha eski dosyalar otomatik silinir

### Manuel Temizleme:

```kotlin
// Tüm log dosyalarını temizle
DebugLogger.clearAllLogs()
```

## 📱 Log Dosyalarını Görüntüleme

### ADB ile:
```bash
# Error log'unu görüntüle
adb shell cat /data/data/com.poolmod.menu/files/debug_logs/errors.log

# Crash log'unu görüntüle
adb shell cat /data/data/com.poolmod.menu/files/debug_logs/crashes.log

# Son 50 satırı görüntüle
adb shell tail -n 50 /data/data/com.poolmod.menu/files/debug_logs/errors.log
```

## 🐛 Yaygın Hatalar ve Çözümleri

### 1. SecurityException - Overlay İzni
**Dosya:** `errors.log`
**Çözüm:** Ayarlar → Özel İzinler → Diğer uygulamaların üzerinde görüntüleme → PoolMod'u aktifleştir

### 2. IllegalArgumentException - WindowManager
**Dosya:** `errors.log`
**Çözüm:** Uygulamayı yeniden başlatın

### 3. NullPointerException
**Dosya:** `crashes.log` veya `errors.log`
**Çözüm:** Log dosyasındaki stack trace'e bakarak hangi satırda hata olduğunu bulun

## 📝 Notlar

- Log dosyaları uygulama silindiğinde otomatik silinir
- Log dosyaları root erişimi gerektirmez
- Log dosyaları sadece uygulama içinde saklanır (güvenlik)
- Production build'de log seviyesi azaltılabilir

## 🔐 Güvenlik

- Log dosyaları sadece uygulama internal storage'ında saklanır
- Başka uygulamalar bu dosyalara erişemez
- Root erişimi gerektirmez
- Log dosyaları hassas bilgiler içerebilir, paylaşırken dikkatli olun

