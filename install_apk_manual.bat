@echo off
title APK Manual Yukle
color 0A
cls
echo.
echo ========================================
echo   APK Manual Yukle
echo ========================================
echo.

cd /d "%~dp0"

set "APK_PATH=app\build\outputs\apk\debug\app-debug.apk"

if not exist "%APK_PATH%" (
    echo [XETA] APK tapilmadi!
    echo.
    echo [INFO] Evvelce APK build edin:
    echo        .\gradlew.bat assembleDebug
    echo.
    pause
    exit /b
)

echo [OK] APK tapildi: %APK_PATH%
echo.
echo [INFO] APK-ni telefona kocurun:
echo.
echo Metod 1: USB kabel
echo   1. Telefoni USB ile baglayin
echo   2. APK faylini telefona kopyalayin
echo   3. Telefonda APK faylina basin ve yukleyin
echo.
echo Metod 2: Email/Cloud
echo   1. APK-ni email-e gonderin
echo   2. Telefonda email-i acin
echo   3. APK-ni endirin ve yukleyin
echo.
echo Metod 3: ADB Force Install
echo   Bu komandani ise salin:
echo   adb install -r -d "%APK_PATH%"
echo.

:: APK path-ini göstər
echo ========================================
echo APK Path:
echo %CD%\%APK_PATH%
echo ========================================
echo.

:: APK-nı explorer-da aç
set /p open="APK faylini Explorer-da acmaq isteyirsiniz? (Y/N): "
if /i "%open%"=="Y" (
    explorer.exe /select,"%CD%\%APK_PATH%"
)

pause

