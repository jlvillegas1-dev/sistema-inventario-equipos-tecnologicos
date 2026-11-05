param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location $projectRoot
try {
    Write-Host "==> Validando frontend" -ForegroundColor Cyan
    & powershell -ExecutionPolicy Bypass -File ".\tools\Validate-Frontend.ps1"
    if ($LASTEXITCODE -ne 0) {
        throw "La validacion frontend fallo."
    }

    if (-not $SkipBuild) {
        Write-Host "==> Compilando con Maven wrapper local" -ForegroundColor Cyan
        & ".\mvnw.cmd" -q -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "La compilacion Maven fallo."
        }
    }

    Write-Host "Validacion completa OK." -ForegroundColor Green
} finally {
    Pop-Location
}
