@echo off
title Quick Debug - Android Studio
color 0A
cls
echo.
echo ========================================
echo   QUICK DEBUG - Android Studio
echo ========================================
echo.

:: Android SDK path
set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%SDK_PATH%\platform-tools\adb.exe"

if not exist "%ADB%" (
    echo [XETA] ADB tapilmadi
    goto :show_instructions
)

:: Cihaz yoxla
"%ADB%" devices | findstr "device$" >nul
if %errorLevel% neq 0 (
    echo [XETA] Cihaz bagli deyil!
    goto :show_instructions
)

echo [OK] Cihaz bagli!
echo [INFO] Logcat temizlenir...
"%ADB%" logcat -c
echo [OK] Logcat temizlendi!
echo.

:show_instructions
echo ========================================
echo   ANDROID STUDIO-DA LOGCAT AÇMAQ
echo ========================================
echo.
echo [ADIM 1] Android Studio-da:
echo   - Alt + 6 basin (Logcat penceresi)
echo   - VEYA: View ^> Tool Windows ^> Logcat
echo.
echo [ADIM 2] Logcat penceresinde:
echo   1. Sag ust kuncde cihazi secin
echo   2. Filter: "com.poolmod.menu" yazin
echo   3. Log Level: "Error" secin
echo   4. "Show only selected application" isaretleyin
echo.
echo [ADIM 3] Proqrami calistirin
echo   - Xeteler Android Studio-da gorunecek
echo.
echo ========================================
echo   ALTERNATIV: Bu pencerede gormek
echo ========================================
echo.
echo [SORU] Bu pencerede xeteleri gormek isteyirsiniz? (Y/N)
set /p choice="> "

if /i "%choice%"=="Y" (
    echo.
    echo [INFO] Xeteler gosterilir...
    echo [INFO] Cixmaq ucun Ctrl+C basin
    echo.
    if exist "%ADB%" (
        "%ADB%" logcat -v time com.poolmod.menu:* *:E *:F AndroidRuntime:E *:S
    ) else (
        echo [XETA] ADB tapilmadi, logcat acila bilmedi
    )
) else (
    echo.
    echo [OK] Android Studio-da Logcat penceresini acin
    echo [INFO] Filter: "com.poolmod.menu"
    echo [INFO] Log Level: "Error"
)

echo.
pause

