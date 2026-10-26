[CmdletBinding()]
param(
    [switch]$Dev,
    [ValidateRange(1, 65535)][int]$BackendPort = 8080,
    [ValidateRange(1, 65535)][int]$FrontendPort = 5173
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$frontendDirectory = Join-Path $projectRoot 'frontend'
$mavenWrapper = Join-Path $projectRoot 'mvnw.cmd'
$logDirectory = Join-Path $projectRoot 'target\demo-logs'

function Assert-PortAvailable([int]$Port) {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
    try { $listener.Start() }
    catch { throw "Puerto $Port ocupado. Usa -BackendPort/-FrontendPort o cerra el servidor anterior." }
    finally { $listener.Stop() }
}

function Invoke-Checked([string]$File, [string[]]$Arguments) {
    & $File @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$File fallo con codigo $LASTEXITCODE." }
}

function Wait-ForEndpoint([string]$Url, [System.Diagnostics.Process]$Process, [string]$Name) {
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        if ($Process.HasExited) { throw "$Name termino. Revisa $logDirectory." }
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 2 | Out-Null
            return
        }
        catch { Start-Sleep -Seconds 1 }
    }
    throw "$Name no respondio. Revisa $logDirectory."
}

function Stop-ProcessTree([int]$RootProcessId, [object[]]$Processes) {
    $children = $Processes | Where-Object { $_.ParentProcessId -eq $RootProcessId }
    foreach ($child in $children) { Stop-ProcessTree $child.ProcessId $Processes }
    Stop-Process -Id $RootProcessId -Force -ErrorAction SilentlyContinue
}

foreach ($name in @('java', 'node')) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Falta $name. Revisa los requisitos de README.md y configura JAVA_HOME para Java."
    }
}
if (Get-Command corepack.cmd -ErrorAction SilentlyContinue) {
    $pnpmFile = 'corepack.cmd'; $pnpmPrefix = @('pnpm')
}
elseif (Get-Command pnpm.cmd -ErrorAction SilentlyContinue) {
    $pnpmFile = 'pnpm.cmd'; $pnpmPrefix = @()
}
else { throw 'Falta pnpm. Instala la version indicada en frontend/package.json.' }

Assert-PortAvailable $BackendPort
if ($Dev) {
    Assert-PortAvailable $FrontendPort
    if ($BackendPort -eq $FrontendPort) { throw 'Backend y frontend deben usar puertos distintos.' }
}

Push-Location $frontendDirectory
try {
    Invoke-Checked 'node' @('scripts/check-env.js')
    Invoke-Checked $pnpmFile ($pnpmPrefix + @('install', '--frozen-lockfile'))
    if (-not $Dev) { Invoke-Checked $pnpmFile ($pnpmPrefix + @('run', 'build:demo')) }
}
finally { Pop-Location }

Push-Location $projectRoot
try { Invoke-Checked $mavenWrapper @('clean') }
finally { Pop-Location }
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

$previousApiUrl = $env:VITE_API_BASE_URL
$previousBackendUrl = $env:BUTACA_BACKEND_URL
$backendProcess = $null
$frontendProcess = $null
try {
    Write-Host "Iniciando backend... Logs: $logDirectory"
    $backendProcess = Start-Process -FilePath $mavenWrapper `
        -ArgumentList @('spring-boot:run', '-Dspring-boot.run.profiles=demo', "-Dspring-boot.run.arguments=--server.port=$BackendPort") `
        -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDirectory 'backend.out.log') `
        -RedirectStandardError (Join-Path $logDirectory 'backend.err.log')
    Wait-ForEndpoint "http://127.0.0.1:$BackendPort/api/eventos" $backendProcess 'Backend'
    $webUrl = "http://localhost:$BackendPort"
    if ($Dev) {
        $env:VITE_API_BASE_URL = '/api'
        $env:BUTACA_BACKEND_URL = "http://127.0.0.1:$BackendPort"
        $frontendProcess = Start-Process -FilePath $pnpmFile `
            -ArgumentList ($pnpmPrefix + @('run', 'dev', '--host', '127.0.0.1', '--port', "$FrontendPort", '--strictPort')) `
            -WorkingDirectory $frontendDirectory -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logDirectory 'frontend.out.log') `
            -RedirectStandardError (Join-Path $logDirectory 'frontend.err.log')
        $webUrl = "http://localhost:$FrontendPort"
        Wait-ForEndpoint "http://127.0.0.1:$FrontendPort" $frontendProcess 'Frontend'
    }
    else { Wait-ForEndpoint "http://127.0.0.1:$BackendPort" $backendProcess 'Web' }

    Write-Host "Butaca listo: $webUrl" -ForegroundColor Green
    Write-Host "API: http://localhost:$BackendPort/api | H2: http://localhost:$BackendPort/h2-console"
    Write-Host 'H2: jdbc:h2:mem:butaca_demo | usuario sa | sin password'
    Write-Host 'Cuentas: admin@butaca.com / admin123; org@butaca.com / org123; cliente@butaca.com / cliente123'
    Write-Host "Logs: $logDirectory. Ctrl+C detiene los servidores."
    while (-not $backendProcess.HasExited -and (-not $frontendProcess -or -not $frontendProcess.HasExited)) {
        Start-Sleep -Seconds 1
    }
    throw "Un servidor termino inesperadamente. Revisa $logDirectory."
}
finally {
    Write-Host 'Cerrando Butaca...'
    $processes = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    if ($frontendProcess) { Stop-ProcessTree $frontendProcess.Id $processes }
    if ($backendProcess) { Stop-ProcessTree $backendProcess.Id $processes }
    $env:VITE_API_BASE_URL = $previousApiUrl
    $env:BUTACA_BACKEND_URL = $previousBackendUrl
}
