@echo off
title Build ve Yukle - PoolMod
color 0A
cls
echo.
echo ========================================
echo   Build ve Yukle - PoolMod
echo   Yeni APK build edilir ve yuklenir
echo ========================================
echo.

cd /d "%~dp0"

:: Android SDK path
set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%SDK_PATH%\platform-tools\adb.exe"

:: Cihaz yoxla
echo [1/4] Cihaz yoxlanilir...
if not exist "%ADB%" (
    echo [XETA] ADB tapilmadi: %ADK_PATH%
    echo [INFO] Android SDK quraşdırın
    pause
    exit /b
)

"%ADB%" devices | findstr "device$" >nul
if %errorLevel% neq 0 (
    echo [XETA] Cihaz bagli deyil!
    echo [INFO] Telefonu USB ile baglayin ve USB debugging aktivlesdirin
    pause
    exit /b
)

echo [OK] Cihaz bagli!
echo.

:: Eski APK-ni sil
echo [2/4] Eski APK silinir...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    del /f /q "app\build\outputs\apk\debug\app-debug.apk"
    echo [OK] Eski APK silindi
) else (
    echo [INFO] Eski APK tapilmadi
)
echo.

:: Yeni APK build et
echo [3/4] Yeni APK build edilir...
echo [INFO] Bu biraz vaxt ala biler...
echo.
call gradlew.bat clean assembleDebug

if %errorLevel% neq 0 (
    echo.
    echo [XETA] Build xetasi!
    echo [INFO] Xetalari yoxlayin
    pause
    exit /b
)

echo.
echo [OK] APK build edildi!
echo.

:: APK yukle
echo [4/4] APK yuklenir...
"%ADB%" install -r "app\build\outputs\apk\debug\app-debug.apk"

if %errorLevel% equ 0 (
    echo.
    echo ========================================
    echo   [OK] APK YUKLENDI!
    echo ========================================
    echo.
    echo [INFO] Proqrami calistirin ve test edin
    echo [INFO] ModMenuService indi normal isleyecek
    echo.
) else (
    echo.
    echo [XETA] APK yuklenmedi!
    echo.
    echo [INFO] Manual yukleme ucun:
    echo   1. install_apk_manual.bat ise salin
    echo   2. VEYA: adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo.
)

pause

