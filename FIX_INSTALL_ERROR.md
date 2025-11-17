# APK Yükləmə Xətası Həll

## Xəta: INSTALL_FAILED_USER_RESTRICTED

Bu xəta cihazda USB ilə quraşdırmanın qadağan olmasından qaynaqlanır.

## Həll Yolları

### Metod 1: Developer Options-da USB Debugging

1. **Telefonda Settings açın**
2. **About phone** (Telefon haqqında) tapın
3. **Build number**-a 7 dəfə basın (Developer mode aktivləşir)
4. **Geri qayıdın** → **Developer options** (İnkişaf etdirici seçimləri)
5. **USB debugging** aktivləşdirin
6. **Install via USB** aktivləşdirin (əgər varsa)
7. **USB debugging (Security settings)** aktivləşdirin (əgər varsa)

### Metod 2: APK-nı Manual Yükləyin

1. **APK faylını tapın:**
   ```
   app\build\outputs\apk\debug\app-debug.apk
   ```

2. **APK-nı telefona köçürün:**
   - USB kabel ilə
   - Və ya email/cloud ilə
   - Və ya Bluetooth ilə

3. **Telefonda:**
   - APK faylını tapın
   - APK-ya basın
   - "Bilinməyən mənbələrdən yükləmə" izni verin
   - Install basın

### Metod 3: ADB ilə Force Install

```powershell
cd "8 pool\YeniProje"
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Və ya:

```powershell
adb install -r -d app\build\outputs\apk\debug\app-debug.apk
```

## Emulator üçün

Emulator-da bu xəta olmamalıdır. Əgər emulator-da görürsünüzsə:

1. **Emulator-u yenidən başladın**
2. **Settings > Developer options** açın
3. **USB debugging** aktivləşdirin

## Qeyd

**8 Ball Pool mod üçün ən asan yol:** APK-nı manual yükləyin (Metod 2)

APK: `app\build\outputs\apk\debug\app-debug.apk`

