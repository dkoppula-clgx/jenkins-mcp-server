@echo off

echo.
echo Jenkins MCP Server Startup
echo =============================
echo.

set "username="
set /p username="Enter Jenkins username: "
if not defined username (
    echo [ERROR] Username cannot be empty
    exit /b 1
)

set "password="
for /f "usebackq delims=" %%p in (`powershell -NoProfile -Command "$p = Read-Host -AsSecureString 'Enter Jenkins password'; [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($p))"`) do set "password=%%p"
if not defined password (
    echo [ERROR] Password cannot be empty
    exit /b 1
)

set /p showpw="Show password? (y/n): "
if /i "%showpw%"=="y" echo   Password: %password%

set "port="
set /p port="Enter server port (default: 8080): "
if not defined port set port=8080

echo.
echo Starting server with:
echo   Username: %username%
echo   Port: %port%
echo.

call gradlew.bat bootRun --args="--jenkins.username=%username% --jenkins.password=%password% --server.port=%port%"
