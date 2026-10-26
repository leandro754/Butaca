# Correo real y gestión de eventos

## Activar el correo de prueba

El transporte SMTP ya está implementado. Mientras no se configure una cuenta, el envío queda `PENDIENTE`; no se simula éxito. El backend admite un destinatario fijo de pruebas. El configurador de Gmail lo fija por defecto en la propia cuenta remitente para evitar redirecciones de vuelta por Cloudflare; se puede elegir otro con `-TestRecipient`.

La opción sencilla para esta demo es Gmail. El remitente será la cuenta Gmail configurada, no la dirección que Cloudflare redirige. No hay que cambiar Email Routing ni contratar un plan de Cloudflare.

1. Activar la verificación en dos pasos de la cuenta Gmail.
2. Crear una contraseña de aplicación llamada Butaca en [Google](https://myaccount.google.com/apppasswords). Algunas cuentas administradas o con Protección Avanzada no la permiten; ver [requisitos oficiales](https://support.google.com/accounts/answer/185833?hl=es).
3. Desde la raíz del proyecto, ejecutar en PowerShell `./scripts/configure-mail.ps1`.
4. Ingresar la cuenta y la contraseña en la terminal. El script prueba SMTP con TLS, envía un mensaje real de comprobación directamente a esa cuenta Gmail y guarda la configuración solo si la prueba resulta aceptada. Usar `-TestRecipient otra@direccion.com` para un destino diferente.
5. Reiniciar el backend. El trabajador procesa hasta 20 avisos pendientes por ciclo de cinco segundos. Una demo H2 normal se reinicia con su semilla; los datos propios requieren exportación previa.

El archivo `secrets/mail.properties` está ignorado por Git y tiene permisos locales restringidos al usuario que lo crea. Nunca colocar las credenciales en React, `VITE_*`, localStorage o documentación. El backend importa ese archivo de forma opcional. Alternativamente se pueden utilizar variables de entorno:

| Variable | Uso |
| --- | --- |
| `MAIL_ENABLED` | `true` para activar el trabajador |
| `MAIL_FROM` | Cuenta remitente autorizada |
| `MAIL_REPLY_TO` | Dirección de respuesta; por defecto `contact@leandro754.moe` |
| `SMTP_HOST`, `SMTP_PORT` | Servidor y puerto; Gmail usa `smtp.gmail.com:587` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Cuenta y contraseña de aplicación |
| `SMTP_STARTTLS`, `SMTP_SSL` | Gmail: `true` y `false`, respectivamente |
| `MAIL_TEST_RECIPIENT` | Destino fijo para pruebas; el archivo generado por el configurador usa la cuenta Gmail elegida |
| `BUTACA_PUBLIC_URL` | URL incluida en los enlaces; por defecto `http://localhost:5173` |

Si se usa el archivo local, sus propiedades específicas prevalecen sobre los valores predeterminados que remiten a variables. Borrar o editar ese archivo para cambiar de configuración.

Cloudflare Routing recibe/redirige mensajes. Cloudflare también documenta un producto separado de envío con SMTP y token, pero no es necesario para esta implementación de prueba. [SMTP Cloudflare](https://developers.cloudflare.com/email-service/api/send-emails/smtp/).

Si el remitente Gmail envía al alias de Cloudflare y este redirige al mismo Gmail, el destinatario puede descartar la copia. Cloudflare recomienda probar las reglas desde una cuenta diferente del destino. Para la demo usamos entrega directa al Gmail del usuario y `Reply-To: contact@leandro754.moe`; no se modifican sus reglas de recepción. [Limitación de la prueba con redirección](https://developers.cloudflare.com/email-service/get-started/route-emails/#test-your-routing-rule).

## Qué se envía y cuándo

La compra guarda el comprobante y una notificación enlazada con `id_compra` en `PENDIENTE`, dentro de su transacción. El trabajador solo ve trabajos confirmados y envía en otra transacción. No espera ni llama a SMTP dentro de la compra.

El correo de compra contiene un recibo HTML y texto alternativo, evento/fecha/lugar, importe histórico, comprobante y un PNG nominal por entrada con QR generado en el servidor. Incluye el recibo académico como TXT; no es factura fiscal. Los cambios de fecha/hora y las cancelaciones generan avisos reales por el mismo transporte.

Solo después de que SMTP acepta el mensaje se marca la notificación `ENVIADA` y el comprobante `ENVIADO`. Esto acredita aceptación SMTP, no recepción en bandeja ni lectura. Ante error, quedan `FALLIDA` / `ERROR_ENVIO`, con un diagnóstico saneado; la compra continúa confirmada. Sin credenciales, quedan pendientes.

Esta implementación usa un trabajador por aplicación y no reintenta automáticamente los fallos ambiguos: una desconexión después de aceptar el mensaje podría causar duplicados al reintentar. Los trabajos pendientes sobreviven a reinicios en PostgreSQL; H2 sigue siendo una base efímera. Los fallos pueden investigarse y volver a encolarse desde el backend/base de pruebas; no se expone un endpoint público de reenvío sin autenticación. El envío de la demo debe mantenerse redirigido al destinatario de prueba mientras la autorización por sesión/rol siga pendiente.

La integración utiliza el [transporte de correo de Spring Boot](https://docs.spring.io/spring-boot/reference/io/email.html), con límites de conexión, lectura y escritura. ZXing genera el QR; Java ImageIO compone el PNG, sin servicios de imágenes externos.

## Panel del organizador

`/dashboard` muestra solo Mis eventos, con búsqueda, fechas, estado y paginación. Sus métricas son eventos publicados con fecha vigente, borradores, entradas vendidas y dinero vendido. Las ventas cuentan entradas `CONFIRMADA` o `USADA` de compras confirmadas con pago aprobado; excluyen las anuladas y los pagos reembolsados. La recaudación suma `precio_pagado`, nunca el precio actual del catálogo.

Cada fila enlaza a `/dashboard/eventos/:eventId`, donde se puede editar la publicación, reprogramar fecha y hora, modificar cupo y configurar hasta 20 tarifas en ARS. Al menos una debe estar activa. Las tarifas retiradas se desactivan: sus referencias históricas no se eliminan. El comprador elige una tarifa activa del propio evento; el backend calcula su precio.

Cancelar exige dos pasos: aceptar el impacto y escribir el título exacto. El backend exige ambas confirmaciones y la versión vigente del evento. Anula también las entradas ya usadas, registra una devolución por compra y deja los avisos pendientes. Repetir la cancelación no duplica devoluciones. La pasarela y los reembolsos siguen siendo simulados, aunque los correos sean reales.

No cambian las 16 entidades ni se agregan columnas. Se reutilizan los vínculos y estados existentes. `@Version` protege compras y ediciones, incluyendo cambios que solo alteran tarifas. La API todavía necesita la autorización por sesión/rol descrita en la revisión previa; comprobar un ID declarado por el cliente no la reemplaza.

## Apariencia

La preferencia claro/oscuro se guarda en `butaca-theme` y se aplica antes del primer pintado. El botón del encabezado cierra dos paños rojos de terciopelo, cambia el tema y vuelve a abrirlos. El SVG aporta pliegues curvos, sombras, tejido y luz; la animación usa transforms, desfase corto y asentamiento. Con movimiento reducido el cambio es inmediato. El overlay tiene limpieza y límite de duración para evitar bloquear la navegación.

La intro de Butaca usa la apariencia guardada: en claro, blanco y letras oscuras; en oscuro, carbón con luz roja tenue y letras marfil durante toda la secuencia. Ambas mantienen B roja, cinco pochoclos dorados y el lema al final. La preferencia de tema no impide reproducirla al abrir o recargar; solo movimiento reducido o salto explícito la omiten.

«Tu próxima salida» queda como línea propia encima del título La cartelera.

La revisión visual del sitio queda a cargo del usuario. Se verifican compilación, contratos, tarifas, cancelación, estados de correo y decodificación de los QR adjuntos con pruebas automatizadas.
