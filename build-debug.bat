@echo off
chcp 65001 >nul
title Alarm Sync Wear - Build

echo ============================================
echo   Alarm Sync Wear - Debug Build
echo ============================================
echo.

:: Set environment
set JAVA_HOME=D:\jdk17-build
set GRADLE_USER_HOME=D:\gradle
set ANDROID_HOME=D:\Android\Sdk
set ANDROID_SDK_ROOT=D:\Android\Sdk

:: Clear Dr.Web TLS interception env var
set JAVA_TOOL_OPTIONS=

:: Add to PATH
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\cmdline-tools\latest\bin;%PATH%

cd /d D:\GitHub\alarm-sync-wear

echo [1/3] Stopping Gradle daemon...
call gradlew.bat --stop >nul 2>&1

echo [2/4] Cleaning KSP cache...
if exist "app\build\generated\ksp" rmdir /s /q "app\build\generated\ksp"
if exist "app\build\tmp\ksp" rmdir /s /q "app\build\tmp\ksp"

echo [3/4] Building phone + wear debug APKs...
call gradlew.bat :app:assembleFdroidDebug :wear:assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo BUILD FAILED!
    pause
    exit /b 1
)

echo.
echo [4/4] Build complete!
echo.
echo APKs:
echo   Phone: app\build\outputs\apk\fdroid\debug\app-fdroid-debug.apk
echo   Wear:  wear\build\outputs\apk\debug\wear-debug.apk
echo.
pause