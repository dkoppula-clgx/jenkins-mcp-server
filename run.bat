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
set /p password="Enter Jenkins password: "
if not defined password (
    echo [ERROR] Password cannot be empty
    exit /b 1
)

set "port="
set /p port="Enter server port (default: 8080): "
if not defined port set port=8080

echo.
echo Starting server with:
echo   Username: %username%
echo   Port: %port%
echo.

call gradlew.bat bootRun --args="--jenkins.username=%username% --jenkins.password=%password% --server.port=%port%"
