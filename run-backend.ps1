# Loads the root .env into this process's environment, then starts the Spring Boot
# backend. Use this for a direct (non-docker) run so MAIL_*/GOOGLE_CLIENT_ID/etc.
# from .env actually reach the app (Spring does not read .env on its own).
#
#   powershell -ExecutionPolicy Bypass -File .\run-backend.ps1
#
$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '.env'

if (Test-Path $envFile) {
    Write-Host "Loading environment from $envFile" -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $name, $value = $line -split '=', 2
            # Strip surrounding quotes if present.
            $value = $value.Trim().Trim('"').Trim("'")
            [Environment]::SetEnvironmentVariable($name.Trim(), $value, 'Process')
        }
    }
} else {
    Write-Host "No .env found at $envFile — running with application.properties defaults." -ForegroundColor Yellow
}

Push-Location (Join-Path $PSScriptRoot 'Backend')
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
