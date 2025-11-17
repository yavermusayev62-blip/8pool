# Android Studio-da Xətaları Görmək Üçün Təlimat

## Metod 1: Android Studio Logcat (Ən Yaxşı)

### Adımlar:

1. **Android Studio-da Logcat penceresini açın:**
   - `Alt + 6` basın
   - VEYA: `View` → `Tool Windows` → `Logcat`

2. **Cihazı seçin:**
   - Logcat penceresinin sağ üst küncündə telefonunuzu seçin

3. **Filter tətbiq edin:**
   - Filter qutusuna yazın: `com.poolmod.menu`
   - Log Level: `Error` seçin
   - "Show only selected application" işarələyin

4. **Proqramı işə salın və xətaları görün**

## Metod 2: Batch Scriptlər

### `quick_debug.bat` ⭐ (Ən Asan)
- Android Studio-da Logcat açmaq üçün təlimat verir
- Logcat-i təmizləyir
- Alternativ olaraq bu pəncərədə xətələri göstərə bilər
- **İstifadə:** İkiqat klikləyin və təlimatları izləyin

### `debug_app_live.bat` ⭐ (Canlı Xətələr)
- Yalnız xətələri canlı göstərir (ekranda)
- Real-time logcat
- PoolMod paketi xətələri
- **İstifadə:** Xətələri dərhal görmək istəyirsinizsə

### `show_all_logs.bat`
- Bütün logları göstərir (ERROR, WARN, INFO, DEBUG)
- Real-time logcat
- **İstifadə:** Ətraflı məlumat lazımdırsa

### `save_logs.bat`
- Logları fayla yazır
- `logcat_output.txt` faylına yazır
- **İstifadə:** Logları saxlamaq istəyirsinizsə

### `install_and_log.bat`
- APK yükləyir
- Sonra xətələri göstərir
- **İstifadə:** APK yükləyib dərhal xətələri görmək istəyirsinizsə

### `install_apk_manual.bat`
- APK faylının yerini göstərir
- Manual yükləmə üçün
- **İstifadə:** USB ilə yükləmə işləmirsə

## Ümumi Xətalar və Həllər

### 1. SecurityException
**Səbəb:** Overlay izni yoxdur
**Həll:**
- Telefonun Ayarları → Xüsusi İcazələr → Digər proqramların üzərində göstərilməsi
- PoolMod proqramını tapın və aktivləşdirin

### 2. IllegalArgumentException
**Səbəb:** WindowManager parametrləri yanlışdır
**Həll:** Proqramı yenidən başladın

### 3. Cihaz bağlı deyil
**Həll:**
- USB kabelini yoxlayın
- USB debugging aktivdir mi?
- Telefonda "USB debugging izni ver" mesajı gördünüzmü?

## Android Studio-da Filter Nümunələri

### Yalnız xətələr:
```
package:com.poolmod.menu level:ERROR
```

### Xətələr və xəbərdarlıqlar:
```
package:com.poolmod.menu level:WARN
```

### Bütün loglar:
```
package:com.poolmod.menu
```

### Xüsusi tag:
```
tag:ModMenuService
```

## Faydalı ADB Əmrləri

### Logcat-i təmizlə:
```bash
adb logcat -c
```

### Yalnız xətələri göstər:
```bash
adb logcat *:E *:F AndroidRuntime:E
```

### PoolMod paketi logları:
```bash
adb logcat com.poolmod.menu:*
```

### Logları fayla yaz:
```bash
adb logcat > logcat_output.txt
```

### Real-time + fayla yaz:
```bash
adb logcat | tee logcat_output.txt
```

## Android Studio-da Logcat İkonları

- 🔴 **Error (E)**: Qırmızı - Xətələr
- 🟠 **Warning (W)**: Narıncı - Xəbərdarlıqlar
- 🔵 **Info (I)**: Mavi - Məlumat
- 🟢 **Debug (D)**: Yaşıl - Debug məlumatları
- ⚪ **Verbose (V)**: Ağ - Ətraflı məlumat

## İpucu

Android Studio-da Logcat pencerəsini ayrı bir pəncərədə açmaq üçün:
1. Logcat pencerəsini sağ klikləyin
2. "Float" seçin
3. İndi Logcat ayrı pəncərədədir

## Tez Başlanğıc

**Android Studio-da xətələri görmək üçün:**

1. `quick_debug.bat` faylını işə salın
2. Android Studio-da `Alt + 6` basın (Logcat)
3. Filter: `com.poolmod.menu` yazın
4. Log Level: `Error` seçin
5. Proqramı işə salın və xətələri görün

**Və ya (Canlı Xətələr):**

1. `debug_app_live.bat` faylını işə salın
2. Xətələr bu pəncərədə real-time görünəcək
3. Proqramı işə salın və xətələri izləyin

