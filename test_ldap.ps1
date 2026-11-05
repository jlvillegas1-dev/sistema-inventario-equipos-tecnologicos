# Script para probar la conexion LDAP y credenciales del Active Directory

Add-Type -AssemblyName System.DirectoryServices

$ldapServer = "192.168.1.1"
$domain = "iepi.gov.ec"

Write-Host "=== TEST CONEXION ACTIVE DIRECTORY (LDAP) ===" -ForegroundColor Cyan
$username = Read-Host "Ingrese el usuario de AD (ej: mcabrera)"
$password = Read-Host "Ingrese la contraseña"

if ([string]::IsNullOrEmpty($username) -or [string]::IsNullOrEmpty($password)) {
    Write-Host "Error: Usuario y contraseña son obligatorios." -ForegroundColor Red
    exit
}

# Construir ruta LDAP
$ldapPath = "LDAP://$ldapServer/DC=iepi,DC=gov,DC=ec"
$userBind = "$username@$domain"

Write-Host "`nIntentando autenticar a $userBind contra $ldapServer..." -ForegroundColor Yellow

try {
    # Crear objeto de conexion
    $entry = New-Object System.DirectoryServices.DirectoryEntry($ldapPath, $userBind, $password)
    
    # Intentar leer una propiedad para forzar el bind/autenticacion
    $guid = $entry.NativeGuid
    Write-Host "¡Conectado exitosamente!" -ForegroundColor Green
    Write-Host "ID único de la conexion (GUID): $guid" -ForegroundColor Gray
    
    # Buscar el usuario para ver sus grupos
    Write-Host "`nBuscando información del usuario y sus grupos..." -ForegroundColor Yellow
    $searcher = New-Object System.DirectoryServices.DirectorySearcher($entry)
    $searcher.Filter = "(sAMAccountName=$username)"
    $searcher.PropertiesToLoad.Add("memberOf") | Out-Null
    $searcher.PropertiesToLoad.Add("displayName") | Out-Null
    
    $result = $searcher.FindOne()
    if ($result) {
        $displayName = $result.Properties["displayName"][0]
        Write-Host "Nombre completo: $displayName" -ForegroundColor Green
        
        Write-Host "`nGrupos a los que pertenece:" -ForegroundColor Cyan
        $groups = $result.Properties["memberof"]
        if ($groups) {
            foreach ($group in $groups) {
                # Extraer el CN del grupo
                if ($group -match "CN=([^,]+)") {
                    $cn = $Matches[1]
                    # Resaltar si pertenece a los grupos de inventario
                    if ($cn -match "SC_Inv|SC_Tecnico|SC_Custodio|SC_Inventario") {
                        Write-Host "  * $cn (GRUPO AUTORIZADO)" -ForegroundColor Green
                    } else {
                        Write-Host "  * $cn" -ForegroundColor Gray
                    }
                }
            }
        } else {
            Write-Host "El usuario no pertenece a ningún grupo." -ForegroundColor Yellow
        }
    } else {
        Write-Host "Error: Usuario autenticado pero no encontrado al buscar sus propiedades." -ForegroundColor Red
    }
} catch {
    Write-Host "`nERROR DE AUTENTICACION:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor DarkRed
    
    if ($_.Exception.InnerException) {
        Write-Host $_.Exception.InnerException.Message -ForegroundColor DarkRed
    }
}

Write-Host "`nPresione cualquier tecla para salir..."
$null = [Console]::ReadKey($true)
