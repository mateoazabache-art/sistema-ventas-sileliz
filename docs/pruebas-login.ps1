# =====================================================
# pruebas-login.ps1
# Prueba la API de sesion contra Tomcat (casos CP-01 a CP-04 y CP-16).
# Requisitos: proyecto corriendo en NetBeans (Run) y 02_datos_prueba.sql cargado.
# Uso, desde la carpeta del repo:
#     powershell -ExecutionPolicy Bypass -File docs\pruebas-login.ps1
# Para probar tambien el bloqueo (deja al vendedor bloqueado 15 min):
#     powershell -ExecutionPolicy Bypass -File docs\pruebas-login.ps1 -ProbarBloqueo
# =====================================================
param([switch]$ProbarBloqueo)

$base = "http://localhost:8080/sileliz"
$script:ok = 0
$script:mal = 0

function Llamar($metodo, $ruta, $cuerpo, $sesion, $tipo = "application/json") {
    $p = @{ Uri = "$base$ruta"; Method = $metodo; WebSession = $sesion; UseBasicParsing = $true }
    if ($null -ne $cuerpo) {
        $p.Body = ($cuerpo | ConvertTo-Json -Compress)
        $p.ContentType = $tipo
    }
    try {
        $r = Invoke-WebRequest @p
        return @{ codigo = [int]$r.StatusCode; texto = $r.Content }
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { return @{ codigo = 0; texto = $_.Exception.Message } }
        return @{ codigo = [int]$resp.StatusCode; texto = "$($_.ErrorDetails.Message)" }
    }
}

function Probar($nombre, $resultado, $esperado) {
    if ($resultado.codigo -eq $esperado) {
        $script:ok++
        Write-Host ("OK    {0} -> {1}" -f $nombre, $resultado.codigo) -ForegroundColor Green
    } else {
        $script:mal++
        Write-Host ("FALLA {0} -> {1} (esperado {2}) {3}" -f $nombre, $resultado.codigo, $esperado, $resultado.texto) -ForegroundColor Red
    }
}

function NuevaSesion { New-Object Microsoft.PowerShell.Commands.WebRequestSession }

$admin    = @{ usuario = "admin";    clave = "Admin2026" }
$vendedor = @{ usuario = "vendedor"; clave = "Venta2026" }
$mala     = @{ usuario = "admin";    clave = "mala1234" }

Write-Host "`n--- Sin sesion ---"
$s = NuevaSesion
Probar "GET /api/salud es publico"                 (Llamar GET  "/api/salud"  $null $s) 200
Probar "GET /api/sesion sin login -> 401"          (Llamar GET  "/api/sesion" $null $s) 401
Probar "CP-16 login con text/plain -> 415"         (Llamar POST "/api/login"  $admin $s "text/plain") 415
Probar "CP-02 clave incorrecta -> 401"             (Llamar POST "/api/login"  $mala  $s) 401

Write-Host "`n--- Administrador ---"
$s = NuevaSesion
Probar "CP-01 login admin -> 200"                  (Llamar POST "/api/login"  $admin $s) 200
Probar "GET /api/sesion con login -> 200"          (Llamar GET  "/api/sesion" $null $s) 200
Probar "POST /api/logout -> 200"                   (Llamar POST "/api/logout" @{} $s) 200
Probar "GET /api/sesion despues de salir -> 401"   (Llamar GET  "/api/sesion" $null $s) 401

Write-Host "`n--- Vendedor ---"
$s = NuevaSesion
Probar "login vendedor -> 200"                     (Llamar POST "/api/login"    $vendedor $s) 200
Probar "CP-04 vendedor a /api/usuarios -> 403"     (Llamar GET  "/api/usuarios" $null $s) 403
Probar "vendedor crea producto -> 403"             (Llamar POST "/api/productos" @{} $s) 403
Llamar POST "/api/logout" @{} $s | Out-Null

if ($ProbarBloqueo) {
    Write-Host "`n--- CP-03 Bloqueo (vendedor) ---"
    $s = NuevaSesion
    $malaV = @{ usuario = "vendedor"; clave = "mala1234" }
    for ($i = 1; $i -le 4; $i++) {
        Probar "intento fallido $i -> 401" (Llamar POST "/api/login" $malaV $s) 401
    }
    Probar "intento fallido 5 -> 423"              (Llamar POST "/api/login" $malaV $s) 423
    Probar "clave correcta pero bloqueado -> 423"  (Llamar POST "/api/login" $vendedor $s) 423
    Write-Host "`nPara desbloquear al vendedor, en pgAdmin (ventas_db):" -ForegroundColor Yellow
    Write-Host "UPDATE usuario SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE nombre_usuario = 'vendedor';" -ForegroundColor Yellow
}

Write-Host ("`nResultado: {0} OK, {1} fallas" -f $script:ok, $script:mal)
