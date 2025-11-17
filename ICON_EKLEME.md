# 🐉 Uygulama İkonu Ekleme Rehberi

Ejderha görselini uygulama ikonu olarak eklemek için aşağıdaki adımları izleyin:

## Yöntem 1: Android Studio ile (Önerilen)

1. **Android Studio'yu açın** ve projeyi açın
2. **Görseli hazırlayın**: Ejderha görselini PNG formatında hazırlayın (transparent arka planlı)
3. **Image Asset Studio'yu açın**:
   - Sağ tık → `New` → `Image Asset`
   - Veya `File` → `New` → `Image Asset`
4. **Görseli seçin**:
   - `Icon Type`: `Launcher Icons (Adaptive and Legacy)`
   - `Foreground Layer` → `Path` → Ejderha görselinizi seçin
   - `Background Layer` → `Color` → Koyu renk seçin (örn: #000000 veya #1a0000)
5. **Kaydedin**: `Next` → `Finish`

## Yöntem 2: Manuel Ekleme

Görseli farklı boyutlarda hazırlayıp şu klasörlere ekleyin:

### Gerekli Boyutlar:
- **mipmap-mdpi**: 48x48 px → `ic_launcher.png` ve `ic_launcher_round.png`
- **mipmap-hdpi**: 72x72 px → `ic_launcher.png` ve `ic_launcher_round.png`
- **mipmap-xhdpi**: 96x96 px → `ic_launcher.png` ve `ic_launcher_round.png`
- **mipmap-xxhdpi**: 144x144 px → `ic_launcher.png` ve `ic_launcher_round.png`
- **mipmap-xxxhdpi**: 192x192 px → `ic_launcher.png` ve `ic_launcher_round.png`

### Foreground için (Adaptive Icon):
- **mipmap-mdpi**: 108x108 px → `ic_launcher_foreground.png`
- **mipmap-hdpi**: 162x162 px → `ic_launcher_foreground.png`
- **mipmap-xhdpi**: 216x216 px → `ic_launcher_foreground.png`
- **mipmap-xxhdpi**: 324x324 px → `ic_launcher_foreground.png`
- **mipmap-xxxhdpi**: 432x432 px → `ic_launcher_foreground.png`

### Klasör Yapısı:
```
app/src/main/res/
├── mipmap-mdpi/
│   ├── ic_launcher.png
│   ├── ic_launcher_round.png
│   └── ic_launcher_foreground.png
├── mipmap-hdpi/
│   ├── ic_launcher.png
│   ├── ic_launcher_round.png
│   └── ic_launcher_foreground.png
├── mipmap-xhdpi/
│   ├── ic_launcher.png
│   ├── ic_launcher_round.png
│   └── ic_launcher_foreground.png
├── mipmap-xxhdpi/
│   ├── ic_launcher.png
│   ├── ic_launcher_round.png
│   └── ic_launcher_foreground.png
├── mipmap-xxxhdpi/
│   ├── ic_launcher.png
│   ├── ic_launcher_round.png
│   └── ic_launcher_foreground.png
└── mipmap-anydpi-v26/
    ├── ic_launcher.xml (zaten oluşturuldu)
    └── ic_launcher_round.xml (zaten oluşturuldu)
```

## Hızlı Çözüm (Tek Görsel)

Eğer görseli tek bir PNG olarak eklemek isterseniz:

1. Görseli `app/src/main/res/drawable/dragon_icon.png` olarak kaydedin
2. `AndroidManifest.xml`'de şu şekilde değiştirin:
   ```xml
   android:icon="@drawable/dragon_icon"
   android:roundIcon="@drawable/dragon_icon"
   ```

**Not**: Bu yöntem tüm cihazlarda aynı boyutta görünecektir. Önerilen yöntem Image Asset Studio kullanmaktır.

## Online Araçlar

Görseli farklı boyutlara dönüştürmek için:
- https://www.appicon.co/
- https://icon.kitchen/
- https://romannurik.github.io/AndroidAssetStudio/

Bu araçlar görselinizi otomatik olarak tüm gerekli boyutlara dönüştürür.

