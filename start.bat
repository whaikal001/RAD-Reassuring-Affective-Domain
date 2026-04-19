@echo off
echo ============================================
echo    SocializerAI - Docker Quick Start
echo ============================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

REM Create .env if not exists
if not exist .env (
    echo Creating .env file from template...
    copy .env.example .env
    echo.
    echo NOTE: Edit .env file to customize settings
    echo.
)

echo Starting SocializerAI...
echo This may take a few minutes on first run.
echo.

REM Build and start containers
docker-compose up --build -d

echo.
echo ============================================
echo    SocializerAI is starting up!
echo ============================================
echo.
echo Frontend: http://localhost
echo Backend:  http://localhost:8080
echo.
echo To view logs:   docker-compose logs -f
echo To stop:        docker-compose down
echo.
pause
