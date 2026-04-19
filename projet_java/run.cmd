@echo off
setlocal

set "PROJECT_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_DIR=%PROJECT_DIR%.tools\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%PROJECT_DIR%.tools\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_CMD=%MAVEN_DIR%\bin\mvn.cmd"
set "JDK_DIR=%PROJECT_DIR%.tools\jdk-17"
set "JDK_ZIP=%PROJECT_DIR%.tools\jdk-17.zip"
set "JAVA_CMD=java"

where java >nul 2>nul
if errorlevel 1 (
    set "JAVA_MAJOR=0"
    goto bootstrap_jdk
)

for /f "tokens=3 delims= " %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION_RAW=%%v"
set "JAVA_VERSION_RAW=%JAVA_VERSION_RAW:"=%"
for /f "tokens=1 delims=." %%v in ("%JAVA_VERSION_RAW%") do set "JAVA_MAJOR=%%v"

if "%JAVA_MAJOR%"=="" (
    set "JAVA_MAJOR=0"
)

if %JAVA_MAJOR% GEQ 17 (
    goto ensure_maven
)

:bootstrap_jdk
if not exist "%PROJECT_DIR%.tools" mkdir "%PROJECT_DIR%.tools"

echo Java 17 was not found on PATH. Bootstrapping a project-local JDK 17...
if not exist "%JDK_DIR%\bin\java.exe" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk' -OutFile '%JDK_ZIP%'"
    if errorlevel 1 (
        echo Failed to download JDK 17.
        exit /b 1
    )

    if exist "%PROJECT_DIR%.tools\jdk-17-tmp" rmdir /s /q "%PROJECT_DIR%.tools\jdk-17-tmp"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%JDK_ZIP%' -DestinationPath '%PROJECT_DIR%.tools\jdk-17-tmp' -Force"
    if errorlevel 1 (
        echo Failed to extract JDK 17.
        exit /b 1
    )

    for /d %%d in ("%PROJECT_DIR%.tools\jdk-17-tmp\*") do (
        move "%%d" "%JDK_DIR%" >nul
        goto jdk_ready
    )

    echo Failed to locate the extracted JDK directory.
    exit /b 1
)

:jdk_ready
set "JAVA_HOME=%JDK_DIR%"
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
set "PATH=%JAVA_HOME%\bin;%PATH%"
if exist "%PROJECT_DIR%.tools\jdk-17-tmp" rmdir /s /q "%PROJECT_DIR%.tools\jdk-17-tmp"

:ensure_maven
if not exist "%MAVEN_CMD%" (
    echo Downloading Apache Maven %MAVEN_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%MAVEN_ZIP%'"
    if errorlevel 1 (
        echo Failed to download Maven.
        exit /b 1
    )

    echo Extracting Maven...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%PROJECT_DIR%.tools' -Force"
    if errorlevel 1 (
        echo Failed to extract Maven.
        exit /b 1
    )
)

if "%~1"=="" (
    call "%MAVEN_CMD%" -f "%PROJECT_DIR%pom.xml" javafx:run
) else (
    call "%MAVEN_CMD%" -f "%PROJECT_DIR%pom.xml" %*
)
exit /b %errorlevel%
