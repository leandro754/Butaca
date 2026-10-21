# Butaca - Plataforma de Eventos y Entradas Nominales

Sistema full-stack (**Spring Boot + React + JPA**) para gestionar eventos, validar identidad (KYC), procesar un pago y emitir entradas nominales con QR + DNI. Incluye un cruce de seguridad entre identidad, precio, pago y datos de cada asistente, con comprobante SHA-256 y auditoría.

> **Estado de las integraciones:** la pasarela de pago y el proveedor de identidad son **simulados** para la demo. No se afirma cumplimiento PCI-DSS ni integración real con RENAPER. Para conectar proveedores reales hay que implementar los adaptadores `PagoGateway` y `ProveedorIdentidad`, obtener credenciales y cumplir los requisitos legales/contractuales correspondientes.

---

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
- El total se calcula **en el servidor** como `precio del evento × cantidad`.
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

- [`docs/migraciones/V2__kyc_pago_seguridad.sql`](docs/migraciones/V2__kyc_pago_seguridad.sql)

Ese script es para ejecutar cuando se decida migrar. No se ejecuta automáticamente en la demo. Los comprobantes históricos conservan su hash original; si se quiere que el endpoint de cruce los valide, hay que reemitirlos o definir una estrategia de migración de hashes.

Documentación adicional:

- [`butaca.dbml`](butaca.dbml) - modelo DBML actualizado.
- [`docs/normalizacion-base-datos.md`](docs/normalizacion-base-datos.md)
- [`docs/glosario-y-modelo-base-de-datos.md`](docs/glosario-y-modelo-base-de-datos.md)
- [`docs/guia-presentacion.md`](docs/guia-presentacion.md)

---

## Ejecución rápida

### Requisitos

- Java 17.
- Node.js >= 20.19 y pnpm >= 11.3 solo si se recompila el frontend.

### Demo H2 (backend + frontend estático)

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

- Aplicación: `http://localhost:8080`
- API: `http://localhost:8080/api`
- H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:butaca_demo`
  - Usuario: `sa`
  - Password: vacío

### Frontend con hot-reload

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo.ps1
```

### Credenciales de prueba

| Rol | Email | Contraseña |
| --- | --- | --- |
| Cliente | `cliente@butaca.com` | `cliente123` |
| Organizador | `org@butaca.com` | `org123` |
| Administrador | `admin@butaca.com` | `admin123` |

Los usuarios demo ya tienen KYC verificado. Para probar el flujo KYC completo, registrar una cuenta nueva y subir una imagen de DNI y una selfie.

---

## Endpoints principales

### Autenticación

- `POST /api/auth/register`
- `POST /api/auth/login`

### Identidad / KYC

- `GET /api/verificaciones/usuario/{usuarioId}`
- `POST /api/verificaciones` - `multipart/form-data`:
  - `usuario_id`
  - `email_usuario`
  - `nombre_usuario`
  - `apellido_usuario`
  - `dni_usuario`
  - `numero_tramite`
  - `metodo_verificacion`
  - `foto_dni` (archivo)
  - `selfie` (archivo)

### Eventos

- `GET /api/eventos`
- `GET /api/eventos/{id}`
- `POST /api/eventos`
- `PUT /api/eventos/{id}`
- `PUT /api/eventos/{id}/publicar`
- `PUT /api/eventos/{id}/cancelar`

### Compras y pago

- `POST /api/compras`
- `GET /api/compras/{id}`
- `GET /api/compras/{id}/verificar-cruce`
- `GET /api/compras/usuario/{id}`
- `GET /api/compras/organizador/{id}`

Ejemplo de request de compra:

```json
{
  "evento_id": 1,
  "usuario_id": 2,
  "email_usuario": "cliente@butaca.com",
  "dni_usuario": "42850123",
  "cantidad": 2,
  "pasarela_pago": "MERCADOPAGO",
  "medio_pago": "TARJETA_CREDITO",
  "ultimos_cuatro_digitos": "4242",
  "tipo_comprobante": "RECIBO_DIGITAL",
  "token_pago": "TOKEN-DEMO",
  "asistentes": [
    { "dni_asistente": "42850123", "nombre_asistente": "Juan Perez" },
    { "dni_asistente": "43999888", "nombre_asistente": "Lucia Fernandez" }
  ]
}
```

### Entradas y validación en puerta

- `GET /api/entradas/validar/{codigo}`
- `PUT /api/entradas/{id}/usar`

### Administración

- `GET /api/admin/dashboard`
- `GET /api/admin/stats`
- `GET /api/admin/auditoria`
- `GET /api/admin/compras`
- `GET /api/admin/usuarios`
- `PUT /api/admin/usuarios/{id}`
- `PUT /api/admin/usuarios/{id}/estado`
- `PUT /api/admin/usuarios/{id}/rol`
- `DELETE /api/admin/usuarios/{id}`
- `GET /api/admin/categorias`
- `POST /api/admin/categorias`
- `GET /api/admin/ubicaciones`
- `POST /api/admin/ubicaciones`

---

## Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas cubren:

1. Catálogo público con disponibilidad.
2. Cruce válido de un comprobante demo.
3. Compra nominal con precio calculado, pago y hash.
4. Bloqueo de compra sin KYC y habilitación posterior.
5. KYC sin usuario existente (no crea cuentas).
6. KYC con foto inválida y auditoría persistente.
7. KYC con DNI distinto al registrado, sin verificar la cuenta y con auditoría.
8. Login fallido con auditoría persistente.
9. Pago rechazado sin emitir entradas y con auditoría independiente.
10. Verificación del cruce de integridad.

Compilar frontend:

```powershell
cd frontend
corepack pnpm install --frozen-lockfile
corepack pnpm build
```

---

## Privacidad y alcance

- No se persisten fotos del DNI ni selfies.
- No se persisten PAN ni CVV; solo los últimos cuatro dígitos.
- La identidad y el pago son simulados: no hay conexión real con RENAPER ni con una pasarela.
- La pasarela simulada no cobra. En un adaptador real, una autorización aprobada que no supere la validación de monto/moneda debe anularse o reembolsarse; este flujo demo no implementa esa compensación.
- La autenticación actual es de demo (usuario en localStorage) y no reemplaza un esquema de sesiones/tokens de producción.
- Para producción se requiere: proveedor KYC autorizado, pasarela tokenizada, HTTPS, gestión de secretos, autorización por roles y revisión legal de datos personales.
