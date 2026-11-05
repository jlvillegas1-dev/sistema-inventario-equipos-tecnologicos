#!/usr/bin/env pwsh

# Función para corregir encoding de caracteres dañados
function Fix-EncodingIssues {
    param($FilePath)
    
    # Leer el archivo como bytes
    $bytes = [System.IO.File]::ReadAllBytes($FilePath)
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)
    
    # Reemplazar caracteres dañados uno por uno
    # Ãƒâ€œN → Ó, Ãƒâ€" → Á, etc.
    $text = $text -replace 'DESFRAGMENTACIÃƒâ€œN', 'DESFRAGMENTACIÓN'
    $text = $text -replace 'DEPURACIÃƒâ€œN', 'DEPURACIÓN'
    $text = $text -replace 'ANÃƒÂLISIS', 'ANÁLISIS'
    $text = $text -replace 'INSTALACIÃƒâ€œN', 'INSTALACIÓN'
    $text = $text -replace 'REORGANIZACIÃƒâ€œN', 'REORGANIZACIÓN'
    $text = $text -replace 'CONEXIÃƒâ€œN', 'CONEXIÓN'
    $text = $text -replace 'OBSERVACIÃ'ƒâ€œN', 'OBSERVACIÓN'
    $text = $text -replace 'propÃ³sito', 'propósito'
    $text = $text -replace 'InstituciÃ³n', 'Institución'
    $text = $text -replace 'Ãºnica', 'única'
    $text = $text -replace 'instalaciÃ³n', 'instalación'
    $text = $text -replace 'Ã¡rea', 'área'
    $text = $text -replace 'tÃ©cnicos', 'técnicos'
    
    # Escribir el archivo corregido
    [System.IO.File]::WriteAllText($FilePath, $text, [System.Text.Encoding]::UTF8)
}

# Aplicar correcciones
$files = @(
    "c:\Users\mayer\Documents\NetBeansProjects\SistemaInventarioV3\src\main\webapp\js\app.js",
    "c:\Users\mayer\Documents\NetBeansProjects\SistemaInventarioV3\target\SistemaInventarioV3-1.0-SNAPSHOT\js\app.js"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "Corrigiendo: $file"
        Fix-EncodingIssues $file
        Write-Host "Completado!"
    }
}
