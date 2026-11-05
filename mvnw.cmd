@echo off
setlocal

set "PRIMARY_MAVEN=C:\Program Files\NetBeans-12.6\netbeans\java\maven\bin\mvn.cmd"
set "WRAPPER_MAVEN=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd"

if exist "%PRIMARY_MAVEN%" (
    call "%PRIMARY_MAVEN%" %*
    exit /b %errorlevel%
)

if exist "%WRAPPER_MAVEN%" (
    call "%WRAPPER_MAVEN%" %*
    exit /b %errorlevel%
)

echo Maven no esta disponible en las rutas conocidas.
echo Instala Maven o actualiza mvnw.cmd con la ruta correcta.
exit /b 1
