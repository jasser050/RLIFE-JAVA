@echo off
setlocal

set "PROJECT_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_DIR=%PROJECT_DIR%.tools\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%TEMP%\studyflow-maven-%MAVEN_VERSION%-%RANDOM%.zip"
set "MAVEN_CMD=%MAVEN_DIR%\bin\mvn.cmd"
set "JDK_DIR=%PROJECT_DIR%.tools\jdk-17"
set "JDK_ZIP=%TEMP%\studyflow-jdk17-%RANDOM%.zip"
set "JAVA_CMD=java"

if exist "%JDK_DIR%\bin\java.exe" (
    set "JAVA_HOME=%JDK_DIR%"
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto ensure_maven
)

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
if exist "%JDK_ZIP%" del /f /q "%JDK_ZIP%" >nul 2>nul
if exist "%PROJECT_DIR%.tools\jdk-17-tmp" rmdir /s /q "%PROJECT_DIR%.tools\jdk-17-tmp" >nul 2>nul
if not exist "%JDK_DIR%\bin\java.exe" (
    curl.exe -L --fail --output "%JDK_ZIP%" "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
    if errorlevel 1 (
        echo Failed to download JDK 17.
        exit /b 1
    )

    if exist "%PROJECT_DIR%.tools\jdk-17-tmp" rmdir /s /q "%PROJECT_DIR%.tools\jdk-17-tmp"
    mkdir "%PROJECT_DIR%.tools\jdk-17-tmp" >nul 2>nul
    tar.exe -xf "%JDK_ZIP%" -C "%PROJECT_DIR%.tools\jdk-17-tmp"
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
if exist "%JDK_ZIP%" del /f /q "%JDK_ZIP%" >nul 2>nul
set "JAVA_HOME=%JDK_DIR%"
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
set "PATH=%JAVA_HOME%\bin;%PATH%"
if exist "%PROJECT_DIR%.tools\jdk-17-tmp" rmdir /s /q "%PROJECT_DIR%.tools\jdk-17-tmp"

:ensure_maven
if not exist "%MAVEN_CMD%" (
    echo Downloading Apache Maven %MAVEN_VERSION%...
    if exist "%MAVEN_ZIP%" del /f /q "%MAVEN_ZIP%" >nul 2>nul
    curl.exe -L --fail --output "%MAVEN_ZIP%" "https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"
    if errorlevel 1 (
        echo Failed to download Maven.
        exit /b 1
    )

    echo Extracting Maven...
    tar.exe -xf "%MAVEN_ZIP%" -C "%PROJECT_DIR%.tools"
    if errorlevel 1 (
        echo Failed to extract Maven.
        exit /b 1
    )
)
if exist "%MAVEN_ZIP%" del /f /q "%MAVEN_ZIP%" >nul 2>nul

if "%~1"=="" (
    call "%MAVEN_CMD%" -f "%PROJECT_DIR%pom.xml" javafx:run
) else (
    call "%MAVEN_CMD%" -f "%PROJECT_DIR%pom.xml" %*
)
exit /b %errorlevel%
