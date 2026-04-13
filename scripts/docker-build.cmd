@echo off
REM ================================================================
REM Build all KIP Docker images
REM Usage: scripts\docker-build.cmd [version]
REM Example: scripts\docker-build.cmd 0.157.1
REM ================================================================

set VERSION=%1
if "%VERSION%"=="" set VERSION=latest

echo Building KIP Platform Docker images (tag: %VERSION%)
echo ==================================================

echo.
echo [1/3] Building frontend (kip-web:%VERSION%)...
docker build -f web/Dockerfile -t "kip-web:%VERSION%" .
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

echo.
echo [2/3] Building IMS (kip-ims:%VERSION%)...
docker build -f api/integration-management-service/Dockerfile -t "kip-ims:%VERSION%" .
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

echo.
echo [3/3] Building IES (kip-ies:%VERSION%)...
docker build -f api/integration-execution-service/Dockerfile -t "kip-ies:%VERSION%" .
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

echo.
echo ==================================================
echo All images built successfully!
echo.
docker images --filter "reference=kip-*" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedSince}}"
