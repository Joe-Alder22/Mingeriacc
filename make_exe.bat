@echo off
rem OPTIONAL: creates a folder Mingeriacc-exe\Mingeriacc containing Mingeriacc.exe
rem with its own Java included, so you can give it to a friend who has no Java.
rem Needs a JDK 17 or newer (winget install EclipseAdoptium.Temurin.26.JDK).
cd /d "%~dp0"

where jpackage >nul 2>nul
if errorlevel 1 (
    echo jpackage was not found. It comes with the JDK:
    echo     winget install EclipseAdoptium.Temurin.26.JDK
    pause
    exit /b 1
)

if not exist Mingeriacc.jar (
    echo Mingeriacc.jar not found. Run build.bat first.
    pause
    exit /b 1
)

if exist exe-tmp rmdir /s /q exe-tmp
if exist Mingeriacc-exe rmdir /s /q Mingeriacc-exe
mkdir exe-tmp
copy Mingeriacc.jar exe-tmp\ >nul
jpackage --type app-image --name Mingeriacc --app-version 0.4.0 --input exe-tmp --main-jar Mingeriacc.jar --main-class mingeriacc.Main --java-options "-Dsun.java2d.uiScale=1" --dest Mingeriacc-exe
if errorlevel 1 goto fail
rmdir /s /q exe-tmp
echo.
echo Done! The game: Mingeriacc-exe\Mingeriacc\Mingeriacc.exe
pause
exit /b 0

:fail
echo.
echo Creating the exe failed, see the errors above.
pause
exit /b 1
