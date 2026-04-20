@echo off
setlocal

set "ROOT=%~dp0"
set "JAVA_HOME="

if exist "%ROOT%\.tools\jdk-17\bin\java.exe" set "JAVA_HOME=%ROOT%\.tools\jdk-17"
if not defined JAVA_HOME if exist "%USERPROFILE%\.jdks\ms-17.0.18\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\.jdks\ms-17.0.18"
if not defined JAVA_HOME if exist "%USERPROFILE%\.jdks\jbr-17.0.14\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\.jdks\jbr-17.0.14"
if not defined JAVA_HOME if exist "%USERPROFILE%\Desktop\projet_java\.tools\jdk-17\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\Desktop\projet_java\.tools\jdk-17"

if not defined JAVA_HOME (
    echo Java 17 not found.
    echo Checked:
    echo   local .tools\jdk-17
    echo   %USERPROFILE%\.jdks\ms-17.0.18
    echo   %USERPROFILE%\.jdks\jbr-17.0.14
    echo   %USERPROFILE%\Desktop\projet_java\.tools\jdk-17
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

if exist "%ROOT%apache-maven-3.9.6\bin\mvn.cmd" (
    set "MVN=%ROOT%apache-maven-3.9.6\bin\mvn.cmd"
) else (
    set "MVN=mvn"
)

echo Using JAVA_HOME=%JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version
call "%MVN%" -q -DskipTests clean javafx:run
exit /b %errorlevel%
