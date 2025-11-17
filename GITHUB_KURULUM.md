# GitHub'a Kod Gönderme Rehberi

## 1. Git Yükleme

### Yöntem 1: Resmi Web Sitesinden
1. https://git-scm.com/download/win adresine gidin
2. Git'i indirin ve kurun
3. Kurulum sırasında varsayılan ayarları kullanın

### Yöntem 2: Winget ile (PowerShell - Yönetici olarak)
```powershell
winget install --id Git.Git -e --source winget
```

### Yöntem 3: Chocolatey ile (eğer yüklüyse)
```powershell
choco install git
```

**Önemli:** Git yüklendikten sonra PowerShell'i kapatıp yeniden açın!

## 2. GitHub Kimlik Doğrulama

### Personal Access Token Oluşturma
1. GitHub'a giriş yapın: https://github.com
2. Sağ üst köşedeki profil resminize tıklayın
3. **Settings** > **Developer settings** > **Personal access tokens** > **Tokens (classic)**
4. **Generate new token (classic)** butonuna tıklayın
5. Token'a bir isim verin (örn: "8pool-project")
6. **repo** seçeneğini işaretleyin (tüm repo izinleri)
7. **Generate token** butonuna tıklayın
8. **Token'ı kopyalayın ve güvenli bir yere kaydedin** (bir daha gösterilmeyecek!)

## 3. Kodları GitHub'a Gönderme

### Otomatik Script ile (Önerilen)
```powershell
cd "C:\Users\User\Desktop\8 pool\8 pool\YeniProje"
.\push_to_github.ps1
```

Script şunları yapacak:
- Git repository başlatacak (eğer yoksa)
- Remote repository ekleyecek
- Tüm dosyaları ekleyecek
- Commit yapacak
- GitHub'a push edecek

### Manuel Yöntem
Eğer script çalışmazsa, şu komutları sırayla çalıştırın:

```powershell
# Proje dizinine git
cd "C:\Users\User\Desktop\8 pool\8 pool\YeniProje"

# Git repository başlat (eğer yoksa)
git init

# Remote repository ekle
git remote add origin https://github.com/yavermusayev62-blip/8pool.git

# Tüm dosyaları ekle
git add .

# Commit yap
git commit -m "Initial commit: 8 Ball Pool Mod Menu"

# Branch'i main olarak ayarla
git branch -M main

# GitHub'a gönder
git push -u origin main
```

## 4. Sorun Giderme

### "git: command not found" hatası
- Git yüklü değil veya PATH'e eklenmemiş
- PowerShell'i yeniden başlatın
- Git'in yüklü olduğundan emin olun: `git --version`

### "Authentication failed" hatası
- Personal Access Token kullanmanız gerekiyor
- Şifre yerine token'ı kullanın
- Token'ın **repo** izni olduğundan emin olun

### "Repository not found" hatası
- Repository URL'ini kontrol edin
- Repository'ye erişim izniniz olduğundan emin olun
- Repository'nin var olduğundan emin olun

### "Permission denied" hatası
- GitHub hesabınızın repository'ye yazma izni olduğundan emin olun
- Personal Access Token'ın doğru olduğundan emin olun

## 5. Sonraki Push'lar

İlk push'tan sonra, değişiklikleri göndermek için:

```powershell
git add .
git commit -m "Değişiklik açıklaması"
git push
```

## 6. Repository Kontrolü

Kodlarınızı görmek için:
https://github.com/yavermusayev62-blip/8pool

