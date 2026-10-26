# Estado del proyecto y próximos pasos

Revisión del 26 de octubre de 2026. Butaca está en desarrollo; esta lista distingue el circuito disponible de los pendientes que deben resolverse o explicarse antes de la entrega.

## Circuito disponible

- Registro e inicio de sesión con BCrypt, perfil local y paneles por rol.
- Catálogo de eventos publicados, categorías y ubicaciones.
- Creación, publicación y cancelación de eventos.
- Verificación de identidad mediante un proveedor simulado.
- Compra nominal y pago simulado con validación de monto.
- Tickets con UUID/QR, precio histórico, historial y marca de uso.
- Comprobantes con cruce SHA-256, notificaciones y auditoría.
- Tarifas por evento, reprogramacion y reembolsos simulados.
- Cola de correo SMTP opcional, con tickets y recibos adjuntos.
- Demo con H2 y datos semilla; configuración alternativa para PostgreSQL.

## Antes de presentar

| Pendiente | Resultado esperado |
| --- | --- |
| Conciliar diagramas | Master y esta rama ya integran el modelo de 16 entidades. Mantener sincronizados el snapshot DrawDB, el DBML, el DDL y las entidades JPA; las evidencias KYC extendidas se documentan en los archivos locales. |
| Ensayar el recorrido | Probar cliente, organizador y administrador con las mismas cuentas, eventos y versión de la presentación. |
| Preparar recursos | Resolver dependencias y comprobar las imagenes externas y las fuentes locales con la conexión que se usará. |
| Ampliar verificación | Añadir pruebas concurrentes de compra y uso de tickets; las pruebas actuales no demuestran comportamiento bajo carga. |
| Validar otros sistemas | Probar el arranque Bash en macOS/Linux con todos los requisitos; la revisión actual ejecutó ambos modos en Windows y verificó la sintaxis Bash. |

## Evolución funcional y técnica

- Implementar sesiones o tokens y autorización por rol en el servidor. La separación de paneles y el perfil en `localStorage` no protegen la API; el registro de demo admite un rol en el request.
- Definir si el control nominal seguirá siendo una comparación visual del DNI o tendrá validación explícita en el endpoint de uso.
- Incorporar proveedores reales de identidad y pago únicamente cuando se amplíe el alcance. La autorización, anulación/reembolso y compensación de pagos externos necesitan un flujo específico.
- Reemplazar la actualización automática del esquema por una estrategia de migraciones revisada para despliegues persistentes.

Los cambios de alcance deben reflejarse primero en el SRS y luego en el modelo, la API y la interfaz. La lista no declara funcionalidades futuras como implementadas.
