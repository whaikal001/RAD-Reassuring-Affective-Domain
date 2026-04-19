@echo off
setlocal

echo Stopping demo tunnel...
taskkill /IM cloudflared.exe /F >nul 2>&1
if errorlevel 1 (
    echo No cloudflared process found.
) else (
    echo Tunnel stopped.
)

if /I "%~1"=="--all" (
    echo Stopping containers...
    docker compose down
)

if exist .demo\cloudflared.log del /f /q .demo\cloudflared.log >nul 2>&1

echo Done.
endlocal
