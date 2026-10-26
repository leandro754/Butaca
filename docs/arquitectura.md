# Arquitectura y alcance

## Qué se implementó

### 1. KYC con DNI, foto y selfie

- El usuario debe existir: la verificación **no crea cuentas** ni asigna contraseñas por defecto.
- Se envían por multipart:
  - `usuario_id`, `dni_usuario`, `numero_tramite`.
  - `foto_dni` y `selfie` como imágenes.
- `ProveedorIdentidadSimulado` valida formato de DNI, presencia/tipo de imagen y coincidencia entre el nombre declarado y el de la cuenta.
- Las imágenes se procesan en memoria. **No se persisten**: solo se guardan hashes SHA-256, proveedor, puntaje, estado y código KYC.
- Un rechazo queda registrado en `verificacion_identidad` y en `auditoria_seguridad`; no se borra al lanzar el error de negocio.
- Si la cuenta ya está verificada con el mismo DNI, la operación es idempotente para no romper el hash de comprobantes ya emitidos.

### 2. Pasarela de pago con adaptador

- La compra usa `PagoGateway`; la implementación incluida es `PagoSimuladoGateway`.
- El servidor obtiene el monto esperado del precio del evento y los asistentes; el total de la compra se expone como suma de los precios históricos de sus entradas.
- Cada `Entrada` guarda `precio_pagado` con el precio histórico del evento.
- El `Pago` guarda `monto`, `moneda`, `referencia_externa`, `codigo_autorizacion` y solo los últimos cuatro dígitos.
- Si el monto aprobado por la pasarela no coincide con el total calculado, la compra se bloquea.
- Para probar un rechazo en la demo, enviar `token_pago` con un valor que empiece con `RECHAZ` (por ejemplo `RECHAZAR-DEMO`).

### 3. Cruce de información y entrada nominal

`CompraService.comprarEntrada` valida, en una sola transacción:

1. Usuario existente, activo y con KYC `VERIFICADA`.
2. Email/DNI del comprador coherentes con la cuenta.
3. Evento publicado, precio válido y cupo disponible.
4. Exactamente `cantidad` asistentes, cada uno con nombre y DNI de 7 u 8 dígitos, sin DNI repetidos.
5. Pago aprobado y monto exacto.
6. Emisión de una entrada nominal por asistente.
7. Comprobante con hash SHA-256 que cruza compra, DNI del comprador, código KYC, referencia/monto del pago, UUIDs de entradas y datos nominales.
8. Auditoría del cruce exitoso.

El endpoint `GET /api/compras/{id}/verificar-cruce` recalcula el hash y devuelve checks de integridad:

- `compra_confirmada`
- `kyc_verificada`
- `pago_aprobado`
- `monto_pago_coincide`
- `precio_unitario_coincide`
- `entradas_nominales`
- `hash_comprobante_coincide`

### 4. Auditoría de seguridad

- Los eventos exitosos se guardan dentro de la transacción de negocio.
- Los rechazos/bloqueos (login fallido, compra sin KYC, pago rechazado, KYC rechazado) usan una transacción independiente (`REQUIRES_NEW`) para que el error no borre la evidencia.
- La IP se obtiene del request (`X-Forwarded-For` o `remoteAddr`) y no está hardcodeada.

### 5. Frontend

- Checkout con carga de foto del DNI y selfie.
- Envío `multipart/form-data` real.
- Si el KYC es rechazado, se muestra el motivo y no se habilita el pago.
- La confirmación consulta el cruce de integridad y muestra el monto efectivamente autorizado.
- La UI aclara que la pasarela y el KYC son simulados y que no se envían al backend el número completo de tarjeta ni el CVV.

---

## Modelo de datos y migración pendiente

El modelo actual ya incluye los campos nuevos. En la demo H2 el esquema se recrea con `spring.jpa.hibernate.ddl-auto=create-drop`.

Campos agregados:

- `verificacion_identidad`: `hash_evidencia_documental`, `hash_selfie`, `proveedor_identidad`, `puntaje_coincidencia`.
- `pago`: `monto_pago`, `moneda_pago`, `codigo_autorizacion_pago`.

Para una base PostgreSQL existente, el script de migración está en:

- [`docs/migraciones/V2__kyc_pago_seguridad.sql`](migraciones/V2__kyc_pago_seguridad.sql)

Ese script es para ejecutar cuando se decida migrar. No se ejecuta automáticamente en la demo. Los comprobantes históricos conservan su hash original; si se quiere que el endpoint de cruce los valide, hay que reemitirlos o definir una estrategia de migración de hashes.

Documentación adicional:

- [`butaca.dbml`](../butaca.dbml) - modelo DBML actualizado.
- [`docs/normalizacion-base-datos.md`](normalizacion-base-datos.md)
- [`docs/glosario-y-modelo-base-de-datos.md`](glosario-y-modelo-base-de-datos.md)
- [`docs/guia-presentacion.md`](guia-presentacion.md)

---

## Privacidad y alcance

- No se persisten fotos del DNI ni selfies.
- No se persisten PAN ni CVV; solo los últimos cuatro dígitos.
- La identidad y el pago son simulados: no hay conexión real con RENAPER ni con una pasarela.
- La pasarela simulada no cobra. En un adaptador real, una autorización aprobada que no supere la validación de monto/moneda debe anularse o reembolsarse; este flujo demo no implementa esa compensación.
- La autenticación actual es de demo (usuario en localStorage) y no reemplaza un esquema de sesiones/tokens de producción.
- Para producción se requiere: proveedor KYC autorizado, pasarela tokenizada, HTTPS, gestión de secretos, autorización por roles y revisión legal de datos personales.
