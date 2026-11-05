param(
    [string]$Url = "http://localhost:8080/inventario_dtic/"
)

$ErrorActionPreference = "Stop"

Write-Host "Abriendo aplicacion en: $Url" -ForegroundColor Cyan
Start-Process $Url
