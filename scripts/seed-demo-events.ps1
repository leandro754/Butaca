param([string]$ApiBaseUrl = 'http://localhost:8081/api')

# Add the shared demo fixture to an already running local demo, preserving purchases.
$ErrorActionPreference = 'Stop'
$api = $ApiBaseUrl.TrimEnd('/')
$users = Invoke-RestMethod "$api/admin/usuarios"
$organizer = $users | Where-Object { $_.email_usuario -eq 'org@butaca.com' -and $_.rol_usuario -eq 'ORGANIZADOR' } | Select-Object -First 1
if (-not $organizer) { throw 'No se encontró el organizador de demo org@butaca.com.' }
$events = Invoke-RestMethod "$api/eventos/organizador/$($organizer.id_usuario)"
$categories = Invoke-RestMethod "$api/admin/categorias"
$locations = Invoke-RestMethod "$api/admin/ubicaciones"
$fixture = Join-Path $PSScriptRoot '../src/main/resources/demo-events.tsv'
$created = 0
foreach ($row in (Import-Csv -LiteralPath $fixture -Delimiter "`t" -Encoding utf8)) {
    if ($events | Where-Object { $_.titulo_evento -eq $row.titulo }) { continue }
    $category = $categories | Where-Object { $_.nombre -eq $row.categoria } | Select-Object -First 1
    if (-not $category) {
        $body = @{ nombre = $row.categoria; descripcion = "Eventos de $($row.categoria)" } | ConvertTo-Json
        $category = Invoke-RestMethod "$api/admin/categorias" -Method Post -ContentType 'application/json; charset=utf-8' -Body $body
        $categories = @($categories) + @($category)
    }
    $location = $locations | Where-Object { $_.nombre_lugar -eq $row.ubicacion -and $_.activo } | Select-Object -First 1
    if (-not $location) { throw "No existe una ubicación activa: $($row.ubicacion)" }
    if ([int]$row.cupos -gt $location.capacidad_maxima) { throw "Capacidad excedida: $($row.titulo)" }
    $body = @{
        titulo_evento = $row.titulo
        descripcion_evento = $row.descripcion
        categoria_id = $category.id_categoria
        ubicacion_id = $location.id_ubicacion
        organizador_id_evento = $organizer.id_usuario
        fecha_evento = (Get-Date).Date.AddDays([int]$row.dias).ToString('yyyy-MM-dd')
        hora_evento = $row.hora
        precio_entrada_evento = [decimal]$row.precio
        cupos_totales_evento = [int]$row.cupos
        estado_publicacion_evento = 'PUBLICADO'
        imagen_url = "https://images.unsplash.com/$($row.foto)?auto=format&fit=crop&w=1200&q=80"
    } | ConvertTo-Json
    $event = Invoke-RestMethod "$api/eventos" -Method Post -ContentType 'application/json; charset=utf-8' -Body $body
    $events = @($events) + @($event)
    $created++
}
Write-Output "Eventos de prueba creados: $created. Los existentes se conservaron."
