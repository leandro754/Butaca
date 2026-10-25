# Revisión de Butaca — 24 de octubre de 2026

> Documento de la revisión inicial. El correo y el panel del organizador se actualizaron posteriormente: ver [implementación SMTP, tarifas y configuración por evento](correo-real-y-eventos.md). Las observaciones de autorización y pagos simulados continúan vigentes.

## Resultado

La demo compila y el frontend consume la API real. El modelo de DrawDB `butaca-modelado-datos` (versión 543) y JPA contienen las mismas 16 entidades y los mismos nombres de columnas. Esto verifica nombres y presencia, no la equivalencia de cada constraint, tipo PostgreSQL ni comportamiento de concurrencia.

`analyze_schema` devolvió **0 errores, 0 advertencias y 2 sugerencias informativas**: agregar `updated_at` a las dos bitácoras. No se aplicaron porque ambas registran hechos históricos append-only. No se modificó el diagrama.

La aplicación sigue siendo una demo académica. Los pendientes de seguridad que siguen abajo impiden considerarla lista para ventas reales.

## Cambios de interfaz realizados

- Inicio con un evento protagonista, selector manual y cartelera ordenada por fecha. Se quitaron los destacados/populares ficticios (todos los eventos tenían ambas marcas) y los bloques repetidos de promoción.
- Identidad visual con fondo cálido, tinta oscura, acento terracota y menos sombras, degradados y etiquetas. Se tomó de [Passline Argentina](https://home.passline.com/home) la jerarquía de afiche, fecha, recinto y búsqueda, sin copiar su marca ni su contenido.
- Búsqueda visible en el encabezado; consultas y filtros de la cartelera en la URL (`q`, `categoria`, `desde`, `hasta`, `precio`, `orden`). Precio sin límite inicial artificial de $50.000.
- Login con formulario simple, mostrar/ocultar contraseña y cuentas demo desplegables. Los enlaces de ayuda del pie ahora tienen respuestas útiles en lugar de enlaces vacíos a la misma sección.
- Administrador: cuatro indicadores seleccionados y secciones **Resumen, Usuarios, Ventas, Catálogos y Actividad**. Búsqueda por persona y rol; altas de categorías y recintos mediante endpoints existentes. Los hashes y referencias de pago solo aparecen al abrir una compra.
- Organizador: **Mis eventos, Ventas y asistentes, Control de acceso**. Crear borrador, publicar y cancelar; ocupación calculada; búsqueda, estado, rango de fechas y orden. El control de puerta consulta primero y requiere cotejar el DNI antes de registrar el ingreso.
- Cliente: ordenar por fecha del evento o de la compra, filtrar fechas y estados, ver QR y descargar. La fecha del ticket ahora sale del evento consultado por la API, nunca de la compra. Si el detalle no está disponible se indica la ausencia de fecha, sin inventarla.
- Listas de compras, actividad, usuarios y eventos con páginas de ocho registros. La auditoría recibe como máximo las últimas 50 operaciones del backend: la pantalla indica ese alcance.
- La recaudación mostrada suma compras confirmadas con pago aprobado y excluye pagos reembolsados. El importe de una compra individual conserva su valor histórico.
- QR generados localmente en SVG y PNG, sin enviar sus UUID a servicios externos. Se corrigieron el buffer de datos vacío, la corrección Reed–Solomon, las capacidades y el intercalado de bloques del generador existente. Los PNG usan módulos enteros y margen blanco de cuatro módulos.
- Los tickets usados/anulados no muestran un QR presentado como válido ni permiten descargar una nueva entrada válida. El modal usa `dialog` nativo, foco y cierre con Escape.
- La confirmación de compra ya no afirma que se envió un correo. No existe todavía un transportador SMTP/Gmail.

Los filtros y la paginación son locales sobre las listas que ya devuelve la API. Para volúmenes grandes habrá que implementar paginación y filtros en el servidor.

## Pendientes encontrados, por prioridad

| Prioridad | Hallazgo y evidencia | Próximo cambio |
| --- | --- | --- |
| Alta | Los controladores administrativos, compras y entradas no verifican una sesión autenticada ni propiedad del recurso. `apiClient` no envía credenciales de sesión; no existe `SecurityFilterChain`. Se comprobó por lectura que `/api/admin/usuarios` responde sin autenticación. | Definir sesión segura o tokens y autorizar por rol/propietario en backend. El guardado del perfil en localStorage y los controles de React no sustituyen esa protección. |
| Alta | `AuthService.registrar` acepta el rol que llega del cliente, incluido `ADMIN` si está en el catálogo. `RegisterPage` permite pedir `ORGANIZADOR` directamente. | Forzar `CLIENTE` en registro público y resolver solicitudes de organización desde administración, como requiere el SRS. |
| Alta | `VerificacionIdentidadService` marca `VERIFICADA` sin consultar un proveedor y `CompraService` puede restaurarla automáticamente en una ruta por email/DNI. | Encapsular la simulación en perfil demo; en producción usar respuesta verificable de un proveedor. No presentar la animación del checkout como consulta real a RENAPER. |
| Alta | `EntradaService.usarEntrada` no verifica DNI presentado, rol ni organizador. El nuevo formulario compara DNI y evento en React, pero una llamada directa a la API puede eludirlo. | Validar esos datos en el servicio con la identidad de la sesión. Mantener `@Version` para rechazar un segundo uso concurrente. |
| Media | `CompraService` crea comprobantes `ENVIADO` y notificaciones como enviadas sin transporte de correo. El estado de la demo no demuestra aceptación por Gmail ni entrega al destinatario. | Implementar el flujo de envío persistente descrito abajo; comenzar en `PENDIENTE`. |
| Media | Las cinco pruebas backend no cubren de forma integral las promesas históricas del README: faltan pruebas de llamadas no autorizadas, rol de registro, DNI en backend y carreras de compra/uso. | Ampliar pruebas al implementar seguridad. No interpretar cinco pruebas verdes como certificación de esos casos. |
| Media | El hash SHA-256 del comprobante verifica un cruce de datos; no equivale por sí solo a una firma digital ni a una factura fiscal autorizada. | Mantener la etiqueta de recibo académico y especificar autenticidad/facturación si se pasa a producción. |
| Media | Los CHECK, enums PostgreSQL y acciones de FK del diagrama no quedan garantizados solamente por `ddl-auto` de Hibernate/H2. | Revisar DDL y preparar migraciones PostgreSQL antes de producción. |
| Baja | Afiches de la demo son fotos de catálogo y algunas no representan la obra concreta. | Cargar afiches propios por evento desde el formulario. La coherencia editorial de esas imágenes dará más identidad que sumar adornos. |

No se cambiaron contratos REST, reglas transaccionales ni permisos del backend en esta revisión visual. Los cambios de seguridad requieren un trabajo específico con pruebas de autorización; no quedan resueltos por el rediseño.

## Integración de Gmail propuesta

Para Butaca conviene **una cuenta remitente de la plataforma**. Los compradores reciben el correo en la dirección de su cuenta y no necesitan conectar su Gmail ni conceder acceso a su bandeja.

| Alternativa | Cuándo usarla | Configuración |
| --- | --- | --- |
| Gmail API con OAuth 2.0 | Opción recomendada para una integración mantenible | Google Cloud, Gmail API habilitada, consentimiento del remitente, acceso offline/refresh token y scope `https://www.googleapis.com/auth/gmail.send`. Este scope permite enviar sin leer la bandeja. |
| Gmail SMTP con contraseña de aplicación | Demo de poco volumen y configuración sencilla | Cuenta con verificación en dos pasos, contraseña de aplicación, `smtp.gmail.com:587` con STARTTLS y `spring-boot-starter-mail`. No usar la contraseña habitual de Google. La disponibilidad depende del tipo y políticas de la cuenta. |

Google recomienda OAuth frente a contraseñas de aplicación. Gmail también tiene límites de envío; una plataforma con volumen necesitará evaluar un proveedor de correo transaccional. No se configuró ni se envió ningún correo durante esta revisión.

### Flujo propuesto sobre las tablas existentes

1. `CompraService` confirma compra/pago y emite entradas nominales. En la misma transacción deja un trabajo persistente en `notificacion`, enlazado con `id_compra`, en `PENDIENTE`. El comprobante también comienza `PENDIENTE`. No enviar HTTP/SMTP dentro de la transacción de compra.
2. Después del commit, un trabajador procesa los trabajos pendientes. Un evento `AFTER_COMMIT` puede despertarlo, pero el registro persistente es lo que permite recuperar trabajos si la aplicación se reinicia.
3. Componer un correo HTML y su alternativa de texto con nombre del evento, fecha/hora/lugar, cantidad, total histórico y número de comprobante. Adjuntar las entradas con QR como PNG o PDF y un archivo de calendario `.ics` opcional. Generar los QR en el servidor; el `canvas` del navegador no sirve como generador de adjuntos del backend.
4. En Gmail API, construir un mensaje MIME con adjuntos, codificarlo en base64url y llamar a `users.messages.send`. Después de la aceptación de Google, guardar la referencia del mensaje, la fecha y marcar notificación `ENVIADA` y comprobante `ENVIADO`. Esto significa aceptado por el proveedor, no necesariamente entregado ni leído.
5. Ante errores, registrar `FALLIDA`/`ERROR_ENVIO`, conservar detalle saneado y reintentar con espera creciente para errores transitorios. No cancelar una compra confirmada por un fallo de correo. Cuotas, errores permanentes y direcciones inválidas deben tratarse de manera distinta.
6. Añadir “Reenviar entradas” al detalle de compra del cliente, con autenticación, comprobación de propietario y límite de frecuencia. En administración, mostrar pendientes y fallos y permitir reintentos controlados.

El modelo ya tiene los vínculos principales. Para robustecer reintentos se necesitarán migraciones con contador de intentos, próxima fecha de intento y referencia del proveedor en el registro de envío, más un mecanismo para reclamar trabajos sin que dos workers envíen lo mismo. Gmail no ofrece por este flujo una garantía de envío exactamente una vez: una desconexión posterior a la aceptación puede dejar resultado ambiguo. Hay que manejar ese caso antes de reintentar automáticamente.

Los secretos y refresh tokens se guardan únicamente en el backend mediante variables de entorno o almacén de secretos. Nunca usar variables `VITE_*`, código React, repositorio ni localStorage para credenciales de Gmail. La cuenta remitente debe otorgar consentimiento desde su navegador; no hace falta compartir su contraseña en el chat.

### Fuentes oficiales

- [Crear y enviar mensajes, incluidos adjuntos MIME](https://developers.google.com/workspace/gmail/api/guides/sending).
- [Scopes de Gmail API y clasificación de permisos](https://developers.google.com/workspace/gmail/api/auth/scopes).
- [Contraseñas de aplicación y sus requisitos](https://support.google.com/accounts/answer/185833).
- [SMTP de Gmail y OAuth 2.0](https://developers.google.com/workspace/gmail/imap/imap-smtp).
- [Límites de una cuenta Gmail personal](https://support.google.com/mail/answer/22839).
- [Límites de Google Workspace](https://knowledge.workspace.google.com/admin/gmail/gmail-sending-limits-in-google-workspace).

## Verificación

- `pnpm run build`.
- `node --test src/utils/collectionFilters.test.js src/utils/qrCode.test.js` desde `frontend/`: rangos inclusivos, orden sin mutación, tildes, exclusión de reembolsos, códigos distintos y límite de longitud.
- `mvnw.cmd compile test`: cinco pruebas backend, sin fallos. Se usó el JDK 24 instalado; el compilador tiene destino Java 17. No se afirma una ejecución con JDK 17.
- Decodificación independiente con ZXing de QR de 3, 20, 36 (UUID), 60, 80 y 106 bytes. La herramienta de validación se instaló solo bajo `target/qr-validation`, sin dependencia nueva de la app.
- Pruebas de navegación, login y filtros en el navegador sobre el backend demo real. Las capturas se conservan localmente en `target/ui-review/` y no se versionan.

- `scripts/demo.sh` levantó una segunda demo en backend 18081 / frontend 15173, separada de la app actual. Se verificaron por API registro, login de los tres roles, KYC simulado, altas de catálogo, creación/publicación de borrador, compra nominal, stock, historiales, consulta QR, ingreso, rechazo de doble ingreso secuencial, cancelación/anulación y auditoría: 14 comprobaciones correctas. La demo aislada se detuvo al terminar.
- En navegador se comprobaron antes de la indicación del usuario de realizar la revisión visual a mano: login de organizador, orden descendente de agenda y un rango de compras del 24/10/2026 que devolvió las dos compras de ese día, por $34.000, ordenadas por hora. La revisión visual restante quedó a cargo del usuario.
- Se corrigió la configuración del proxy: esta máquina usa backend 8081, con `frontend/.env.local` ignorado. El código conserva el fallback estándar 8080. `http://localhost:5173/api/eventos` devuelve 200 con seis eventos. Se añadió recuperación “Reintentar” ante errores de carga.
- Ante el aviso de que la cartelera seguía sin cargar, se revisaron los errores funcionales del navegador: los dos procesos Vite habían compartido caché de optimización y la recarga mezclaba módulos React. Se configuró una caché por puerto, se detuvo la instancia de pruebas y se reinició el frontend principal. La comprobación final en una página nueva de `localhost:5173` encontró seis tarjetas de evento y cero errores de consola. La pestaña de prueba `15173` no es la aplicación que debe usarse.
