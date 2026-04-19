@echo off
setlocal

echo ============================================
echo   SocializerAI Demo URL Starter
echo ============================================
echo.

REM Check Docker
docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running.
    echo Start Docker Desktop, then run this script again.
    exit /b 1
)

REM Resolve Cloudflared executable (PATH or common install paths)
set "CLOUDFLARED_EXE="
where cloudflared >nul 2>&1
if not errorlevel 1 set "CLOUDFLARED_EXE=cloudflared"
if not defined CLOUDFLARED_EXE if exist "%ProgramFiles%\cloudflared\cloudflared.exe" set "CLOUDFLARED_EXE=%ProgramFiles%\cloudflared\cloudflared.exe"
if not defined CLOUDFLARED_EXE if exist "%ProgramFiles(x86)%\cloudflared\cloudflared.exe" set "CLOUDFLARED_EXE=%ProgramFiles(x86)%\cloudflared\cloudflared.exe"

if not defined CLOUDFLARED_EXE (
    echo ERROR: cloudflared is not installed.
    echo Install it with: winget install Cloudflare.cloudflared
    exit /b 1
)

echo Starting containers...
docker compose up -d
if errorlevel 1 (
    echo ERROR: Failed to start containers.
    exit /b 1
)

if not exist .demo mkdir .demo
if exist .demo\cloudflared.log del /f /q .demo\cloudflared.log >nul 2>&1

REM Stop existing cloudflared instances for a clean demo URL session
taskkill /IM cloudflared.exe /F >nul 2>&1

echo Starting public tunnel...
start "" /B "%CLOUDFLARED_EXE%" tunnel --url http://localhost:3000 > .demo\cloudflared.log 2>&1

set "PUBLIC_URL="
for /l %%i in (1,1,30) do (
    for /f "usebackq delims=" %%u in (`powershell -NoProfile -Command "$m = Select-String -Path '.demo/cloudflared.log' -Pattern 'https://[-a-zA-Z0-9]+\.trycloudflare\.com' | Select-Object -Last 1; if ($m) { $m.Matches[0].Value }"`) do (
        set "PUBLIC_URL=%%u"
    )
    if defined PUBLIC_URL goto :show_url
    timeout /t 1 >nul
)

echo Tunnel started, but URL not detected yet.
echo Run this to check logs: type .demo\cloudflared.log
echo.
echo Your demo should be available soon once the URL appears in logs.
goto :done

:show_url
echo.
echo ============================================
echo   Demo URL Ready
echo ============================================
echo %PUBLIC_URL%
echo.
echo Share this URL for demo access.
echo.
echo To stop only the tunnel: demo-stop.bat
echo To stop tunnel and containers: demo-stop.bat --all

:done
endlocal
