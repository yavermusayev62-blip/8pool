@echo off
title Android Logcat - Fayla Saxla
color 0C
cls
echo.
echo ========================================
echo   Android Logcat - Fayla Saxla
echo   Xeteleri fayla yazacaq
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
"%ADB%" devices | findstr "device$" >nul
if %errorLevel% neq 0 (
    echo [XETA] Cihaz bagli deyil!
    pause
    exit /b
)

:: Log faylı
set "LOG_FILE=android_logs_%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%.txt"
set "LOG_FILE=%LOG_FILE: =0%"

echo [INFO] Loglar fayla yazilir: %LOG_FILE%
echo [INFO] 10 saniye log toplanacaq...
echo [INFO] Dayandirmaq ucun Ctrl+C basin
echo.

:: Logları fayla yaz
"%ADB%" logcat -d > "%LOG_FILE%"

echo.
echo [OK] Loglar saxlandi: %LOG_FILE%
echo.
echo [INFO] Fayli acmaq isteyirsiniz? (Y/N)
set /p open="> "
if /i "%open%"=="Y" (
    notepad "%LOG_FILE%"
)

pause

