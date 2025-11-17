@echo off
title Android Logcat - Butun Loglar
color 0B
cls
echo.
echo ========================================
echo   Android Logcat - Butun Loglar
echo   Telefonunuzdaki butun loglari goreceksiniz
echo ========================================
echo.

:: Android SDK path
set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%SDK_PATH%\platform-tools\adb.exe"

if not exist "%ADB%" (
    echo [XETA] ADB tapilmadi: %ADB%
    echo [INFO] Android SDK quraşdırın
    pause
    exit /b
)

:: Cihaz yoxla
echo [INFO] Cihaz yoxlanilir...
"%ADB%" devices
echo.

"%ADB%" devices | findstr "device$" >nul
if %errorLevel% neq 0 (
    echo [XETA] Cihaz bagli deyil!
    echo.
    echo [INFO] Yoxlayin:
    echo 1. Telefon USB ile baglidir mi?
    echo 2. USB debugging aktivdir mi?
    echo 3. Telefonda "USB debugging izni ver" mesaji gordunuz mu?
    echo.
    pause
    exit /b
)

echo [OK] Cihaz bagli!
echo.
echo ========================================
echo   Logcat Basladildi
echo   Butun loglar gosterilecek
echo   Cixmaq ucun Ctrl+C basin
echo ========================================
echo.

:: Bütün logları göstər
"%ADB%" logcat

pause

