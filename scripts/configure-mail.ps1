param([string]$Account = '', [string]$TestRecipient = '')
$ErrorActionPreference = 'Stop'
if (-not $Account) { $Account = Read-Host 'Cuenta Gmail que enviará los correos' }
if ($Account -notmatch '^[^\s@]+@[^\s@]+\.[^\s@]+$') { throw 'Ingresá una dirección de correo válida.' }
Write-Host 'Creá una contraseña de aplicación para Butaca en https://myaccount.google.com/apppasswords'
$appPassword = Read-Host 'Contraseña de aplicación de Google (se oculta al escribir)' -AsSecureString
$credential = [System.Net.NetworkCredential]::new($Account, $appPassword)
$password = $credential.Password.Replace(' ', '')
$recipient = if ($TestRecipient) { $TestRecipient } else { $Account }
$message = [System.Net.Mail.MailMessage]::new($Account, $recipient)
$message.Subject = 'Butaca · Prueba real de correo'
$message.SubjectEncoding = [System.Text.Encoding]::UTF8
$message.BodyEncoding = [System.Text.Encoding]::UTF8
$message.Body = 'La conexión SMTP de Butaca funciona. Las entradas y avisos de prueba se enviarán a esta dirección. Los pagos siguen siendo simulados.'
$client = [System.Net.Mail.SmtpClient]::new('smtp.gmail.com', 587)
$client.EnableSsl = $true
$client.Timeout = 15000
$client.Credentials = [System.Net.NetworkCredential]::new($Account, $password)
try { $client.Send($message) } finally { $message.Dispose(); $client.Dispose() }
$secretDirectory = Join-Path $PSScriptRoot '../secrets'
New-Item -ItemType Directory -Path $secretDirectory -Force | Out-Null
$secretPath = Join-Path $secretDirectory 'mail.properties'
function Escape-Property([string]$Value) { return $Value.Replace('\', '\\').Replace("`r", '\r').Replace("`n", '\n') }
$properties = @(
    'butaca.mail.enabled=true'
    "butaca.mail.from=$(Escape-Property $Account)"
    "butaca.mail.test-recipient=$recipient"
    'butaca.mail.reply-to=contact@leandro754.moe'
    'spring.mail.host=smtp.gmail.com'
    'spring.mail.port=587'
    "spring.mail.username=$(Escape-Property $Account)"
    "spring.mail.password=$(Escape-Property $password)"
    'spring.mail.properties.mail.smtp.auth=true'
    'spring.mail.properties.mail.smtp.starttls.enable=true'
    'spring.mail.properties.mail.smtp.starttls.required=true'
    'spring.mail.properties.mail.smtp.ssl.enable=false'
)
[System.IO.File]::WriteAllLines($secretPath, $properties, [System.Text.UTF8Encoding]::new($false))
$password = $null
try {
    $acl = Get-Acl -LiteralPath $secretPath
    $acl.SetAccessRuleProtection($true, $false)
    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    $rule = [System.Security.AccessControl.FileSystemAccessRule]::new($identity, 'FullControl', 'Allow')
    $acl.AddAccessRule($rule)
    Set-Acl -LiteralPath $secretPath -AclObject $acl
} catch { Write-Warning 'No se pudo limitar la ACL del archivo; verificá sus permisos locales.' }
Write-Host "Prueba aceptada por Gmail para $recipient. Configuración guardada fuera de Git."
Write-Host 'Reiniciá el backend para activar el envío de entradas y avisos pendientes.'
