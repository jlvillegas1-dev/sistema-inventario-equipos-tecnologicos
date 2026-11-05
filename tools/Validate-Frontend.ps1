param(
    [string]$WebRoot = "src/main/webapp"
)

$ErrorActionPreference = "Stop"

function Assert-Exists {
    param(
        [string]$Path,
        [string]$Message
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw $Message
    }
}

function Get-ReferencedPaths {
    param(
        [string]$HtmlPath
    )

    $content = Get-Content -Raw -LiteralPath $HtmlPath
    $dir = Split-Path -Parent $HtmlPath
    $results = @()

    $patterns = @(
        'href="([^"]+\.(?:css|html))"',
        'src="([^"]+\.(?:js))"'
    )

    foreach ($pattern in $patterns) {
        foreach ($match in [regex]::Matches($content, $pattern)) {
            $ref = $match.Groups[1].Value
            if ($ref -match '^(https?:)?//') {
                continue
            }
            $normalized = Join-Path $dir $ref
            $results += [pscustomobject]@{
                Source = $HtmlPath
                Reference = $ref
                Resolved = [System.IO.Path]::GetFullPath($normalized)
            }
        }
    }

    return $results
}

function Test-CssBraceBalance {
    param(
        [string]$Text,
        [string]$Path
    )

    $opens = ([regex]::Matches($Text, '\{')).Count
    $closes = ([regex]::Matches($Text, '\}')).Count
    if ($opens -ne $closes) {
        throw "Balance de llaves invalido en $Path. Aperturas: $opens. Cierres: $closes."
    }
}

$htmlFiles = Get-ChildItem -Path $WebRoot -Recurse -Filter *.html
$cssFiles = Get-ChildItem -Path $WebRoot -Recurse -Filter *.css
$jsFiles = Get-ChildItem -Path $WebRoot -Recurse -Filter *.js

if (-not $htmlFiles) {
    throw "No se encontraron archivos HTML en $WebRoot"
}

foreach ($htmlFile in $htmlFiles) {
    $refs = Get-ReferencedPaths -HtmlPath $htmlFile.FullName
    foreach ($ref in $refs) {
        Assert-Exists -Path $ref.Resolved -Message "Referencia rota: $($ref.Reference) en $($ref.Source)"
    }
}

foreach ($jsFile in $jsFiles) {
    $content = Get-Content -Raw -LiteralPath $jsFile.FullName
    if ([string]::IsNullOrWhiteSpace($content)) {
        throw "Archivo JS vacio: $($jsFile.FullName)"
    }
    if ($content -match '<<<<<<<|=======|>>>>>>>') {
        throw "Archivo JS con marcadores de conflicto: $($jsFile.FullName)"
    }
}

foreach ($cssFile in $cssFiles) {
    $content = Get-Content -Raw -LiteralPath $cssFile.FullName
    Test-CssBraceBalance -Text $content -Path $cssFile.FullName
}

Write-Host "Validacion frontend completada correctamente." -ForegroundColor Green
Write-Host "HTML:" $htmlFiles.Count "| CSS:" $cssFiles.Count "| JS:" $jsFiles.Count
