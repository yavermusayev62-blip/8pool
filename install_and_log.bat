@echo off
title APK Yukle ve Logcat Ac
color 0A
cls
echo.
echo ========================================
echo   APK Yukle ve Logcat Ac
echo   APK yuklenecek ve xeteler gosterilecek
echo ========================================
echo.

cd /d "%~dp0"

:: Android SDK path
set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%SDK_PATH%\platform-tools\adb.exe"

:: Cihaz yoxla
echo [1/3] Cihaz yoxlanilir...
if not exist "%ADB%" (
    echo [XETA] ADB tapilmadi
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

:: Logcat-i təmizlə
echo [2/3] Logcat temizlenir...
"%ADB%" logcat -c
echo [OK] Logcat temizlendi
echo.

:: APK yüklə
echo [3/3] APK build edilir ve yuklenir...
call gradlew.bat installDebug

if %errorLevel% equ 0 (
    echo.
    echo [OK] APK yuklendi!
    echo.
    echo ========================================
    echo   Logcat Basladildi
    echo   Xeteleri gormek ucun gozleyin...
    echo   Cixmaq ucun Ctrl+C basin
    echo ========================================
    echo.
    
    :: Xətaları göstər
    "%ADB%" logcat *:E *:F AndroidRuntime:E *:S
) else (
    echo.
    echo [XETA] APK yuklenmedi
    echo.
    echo [INFO] Logcat-i acmaq isteyirsiniz? (Y/N)
    set /p open="> "
    if /i "%open%"=="Y" (
        start "" cmd /k "%ADB%" logcat *:E *:F AndroidRuntime:E *:S
    )
)

pause

