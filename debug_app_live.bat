@echo off
title Android Debug - Canli Xeteler
color 0C
cls
echo.
echo ========================================
echo   Android Debug - Canli Xeteler
echo   Xeteler canli olaraq gosterilir
echo ========================================
echo.

:: Android SDK path
set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%SDK_PATH%\platform-tools\adb.exe"

if not exist "%ADB%" (
    echo [XETA] ADB tapilmadi: %ADB%
    pause
    exit /b
)

:: Cihaz yoxla
echo [INFO] Cihaz yoxlanilir...
"%ADB%" devices | findstr "device$" >nul
if %errorLevel% neq 0 (
    echo [XETA] Cihaz bagli deyil!
    pause
    exit /b
)

echo [OK] Cihaz bagli!
echo.

:: Logcat-i temizle
echo [INFO] Logcat temizlenir...
"%ADB%" logcat -c
echo [OK] Logcat temizlendi
echo.

echo ========================================
echo   CANLI XETELER GOSTERILIR
echo   Cixmaq ucun Ctrl+C basin
echo ========================================
echo.
echo [INFO] Yalniz xeteler gosterilir:
echo   - ERROR seviyesi loglar
echo   - FATAL seviyesi loglar  
echo   - AndroidRuntime xeteleri
echo   - PoolMod paketi xeteleri
echo.

:: Xətaları canlı göstər
"%ADB%" logcat -v time *:E *:F AndroidRuntime:E com.poolmod.menu:* *:S

pause

