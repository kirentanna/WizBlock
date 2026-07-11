@echo off
setlocal

cd /d "%~dp0"

set "MODE=%~1"
set "APP_ID=com.wizblock"
set "MAIN_ACTIVITY=com.wizblock/.MainActivity"
set "APK=app\build\outputs\apk\debug\app-debug.apk"
set "ADB=adb"

where adb >nul 2>nul
if errorlevel 1 (
  if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
)

if /I "%MODE%"=="help" goto :help
if /I "%MODE%"=="--help" goto :help
if /I "%MODE%"=="/?" goto :help

call :check_device
if errorlevel 1 exit /b %errorlevel%

if /I "%MODE%"=="launch-only" goto :launch_app

if /I "%MODE%"=="install-only" (
  if not exist "%APK%" (
    echo Missing "%APK%".
    echo Run runphone first, or run gradlew.bat :app:assembleDebug before runphone install-only.
    exit /b 1
  )
  echo Installing existing WizBlock debug APK...
  "%ADB%" install -r -d "%APK%"
  if errorlevel 1 exit /b %errorlevel%
  goto :launch_app
)

if /I "%MODE%"=="clear-data" (
  echo Clearing WizBlock app data...
  "%ADB%" shell pm clear "%APP_ID%"
  if errorlevel 1 exit /b %errorlevel%
  goto :launch_app
)

if not "%MODE%"=="" (
  echo Unknown mode: %MODE%
  echo.
  goto :help
)

echo Installing WizBlock debug build...
call gradlew.bat :app:installDebug --parallel --build-cache
if errorlevel 1 exit /b %errorlevel%

:launch_app
echo.
echo Launching WizBlock...
"%ADB%" shell am start -n "%MAIN_ACTIVITY%"
if errorlevel 1 exit /b %errorlevel%

echo.
echo WizBlock installed and launched.
endlocal
exit /b 0

:help
echo Usage:
echo   runphone              Build/install debug APK, then launch WizBlock.
echo   runphone install-only Install existing debug APK, then launch WizBlock.
echo   runphone launch-only  Launch the installed WizBlock app.
echo   runphone clear-data   Clear WizBlock data, then launch the app.
endlocal
exit /b 0

:check_device
"%ADB%" get-state >nul 2>nul
if errorlevel 1 (
  echo No adb device is connected.
  echo Connect/unlock the phone and accept USB debugging, then run this again.
  exit /b 1
)
exit /b 0
