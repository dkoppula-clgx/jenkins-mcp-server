@echo off

echo.
echo Jenkins MCP Server Configuration Setup
echo ========================================
echo.
echo This script will scaffold your application-test.yml configuration
echo by discovering jobs from your Jenkins space.
echo.

set "businessUnit="
set /p businessUnit="Enter Business Unit (e.g., credit-us): "
if not defined businessUnit (
    echo [ERROR] Business Unit cannot be empty
    exit /b 1
)

set "space="
set /p space="Enter Project Space (e.g., pbs, dhqcare): "
if not defined space (
    echo [ERROR] Project Space cannot be empty
    exit /b 1
)

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

echo.
echo Configuration:
echo   Business Unit: %businessUnit%
echo   Project Space: %space%
echo   Username: %username%
echo.
echo Discovering jobs from Jenkins...
echo.

node setup\scaffold-config.js "%businessUnit%" "%space%" "%username%" "%password%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Setup failed. Please check the error messages above.
    exit /b 1
)

echo.
echo [SUCCESS] Configuration file created at: src\main\resources\application.yml
echo.
echo You can now run the server with:
echo   .\run.bat
echo.
