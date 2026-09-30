@echo off
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
    echo Java Development Kit, or JDK, not found.
    echo Install one first, for example:  winget install EclipseAdoptium.Temurin.26.JDK
    pause
    exit /b 1
)

echo Compiling...
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\mingeriacc\*.java
if errorlevel 1 (
    echo.
    echo Compilation failed.
    pause
    exit /b 1
)

echo Packaging Mingeriacc.jar...
echo Main-Class: mingeriacc.Main> out\manifest.txt
pushd out
jar cfm ..\Mingeriacc.jar manifest.txt mingeriacc\*.class
if errorlevel 1 (
    popd
    echo.
    echo Packaging failed.
    pause
    exit /b 1
)
popd
rmdir /s /q out

echo.
echo Done! Mingeriacc.jar has been updated.
pause
