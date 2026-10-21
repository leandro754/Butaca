# Plan de Exposición de Butaca — 10 Minutos (Versión Final 13 Tablas)

> **Alcance de la demo:** la pasarela de pago y el proveedor de identidad son simulados. El proyecto no afirma certificación PCI-DSS ni integración real con RENAPER; las referencias a PCI-DSS describen decisiones de minimización de datos.

Este documento propone una defensa del proyecto **desde el resultado hacia las decisiones**: primero se muestra que Butaca funciona de punta a punta y después se explica cómo se resolvieron las correcciones pedidas por la cátedra (pasarela de pago desacoplada, verificación de identidad KYC obligatoria, método de cruce de información con firma SHA-256, entrada nominal por DNI y trackeo en bitácora de seguridad) cumpliendo estrictamente **1FN, 2FN, 3FN y FNBC**.

---

## ⏱️ Distribución de los 10 Minutos

| Tiempo | Responsable | Núcleo | Qué se muestra o explica |
| --- | --- | --- | --- |
| 0:00–2:00 | Mariano | Resultado en vivo | Demo: filtro por Categorías, KYC con proveedor simulado (tipo RENAPER), pago simulado y emisión de entradas nominales con QR + DNI. |
| 2:00–3:30 | Santiago | Problema y Roles | Separación de vistas: Cliente, Organizador (escáner QR + DNI en puerta) y Administrador (gestión de roles y auditoría). |
| 3:30–5:30 | Ignacio | Diagrama ER (13 Tablas) y 3FN | Cómo se mueven los datos en el diagrama durante una compra y por qué cumple 1FN, 2FN y 3FN sin redundancia. |
| 5:30–7:30 | Leandro | Seguridad Informática | Minimización de datos en `pago`, KYC en `verificacion_identidad`, firma `SHA-256` en `comprobante`, `@Version` y bitácora `auditoria_seguridad`. |
| 7:30–9:15 | Mariano | Pruebas y Concurrencia | Los 10 tests de integración automatizados (`ButacaApplicationTests`) que validan el circuito. |
| 9:15–10:00 | Santiago | Cierre | Síntesis del valor académico y evolución a producción. |

---

## 1. Recorrido de Demo en Vivo (`http://localhost:8080`)

### Paso A — Vista Cliente (`cliente@butaca.com` / `cliente123`)
1. Mostrar el modo oscuro renovado con imágenes únicas por espectáculo y filtrar desde el botón **Categorías** de la barra superior.
2. Entrar a un evento y hacer clic en **Comprar entradas**.
3. En el Checkout:
   - Mostrar el **Paso 1 (Identidad verificada con proveedor simulado)**: explicar que el backend bloquea cualquier compra si el usuario no tiene registro `VERIFICADA` en `verificacion_identidad`.
   - Mostrar el **Paso 2 (Titularidad Nominal)**: si se compran 2 entradas, se pide nombre y DNI de cada asistente individual.
   - Mostrar el **Paso 3 (Pasarela de pago simulada)**: tarjeta interactiva o billetera virtual; aclarar que al servidor solo viajan los últimos 4 dígitos.
4. Al confirmar, mostrar la pantalla limpia del cliente con sus tickets QR nominales listos para presentar en puerta (sin abrumarlo con hashes técnicos).

### Paso B — Vista Organizador (`org@butaca.com` / `org123`)
1. En el panel del organizador, ir al **Control de Acceso en Puerta (QR + DNI Nominal)**.
2. Pegar el código UUID de la entrada recién comprada e ingresar primero un **DNI falso**: mostrar cómo el sistema **rechaza el ingreso** porque la entrada es nominal.
3. Ingresar el **DNI real del titular**: mostrar cómo el ticket pasa a `USADA` y queda registrado en `historial_estado_entrada` y `auditoria_seguridad`.

### Paso C — Vista Administrador (`admin@butaca.com` / `admin123`)
1. Mostrar la sección de **Usuarios y Roles**: cómo las cuentas nuevas nacen siempre como `CLIENTE` y solo el administrador puede promoverlas a `ORGANIZADOR`.
2. Mostrar la tabla de **Cruce Técnico de Compras y Pagos**: donde el administrador audita la referencia externa de pasarela, los últimos 4 dígitos y el hash de integridad `SHA-256` del comprobante.
3. Mostrar la **Bitácora de Auditoría de Seguridad** en vivo.

---

## 2. Preguntas Clave del Profesor y Respuestas Exactas

### ¿Cómo verifican la identidad de alguien en la app antes de comprar?
Mediante la tabla **`verificacion_identidad`** (relación `1:1` con `usuario`). Antes de ejecutar `CompraService.comprarEntrada()`, el servicio verifica que el comprador posea `estado_verificacion = VERIFICADA`. Siguiendo el principio de minimización de datos (Ley 25.326), no guardamos imágenes biométricas crudas en la base, sino el método, el proveedor, el puntaje, el token único `codigo_validacion_kyc` y los hashes SHA-256 de las evidencias. El proveedor incluido es un simulador; no es RENAPER real.

### ¿Cómo agregaron la pasarela de pago cuidando 3FN y seguridad?
Agregamos la entidad **`pago`** vinculada `1:1` con `compra`.
- **Minimización de datos:** Nunca se almacena el número completo de tarjeta (PAN) ni el CVV; solo se guarda `ultimos_cuatro_digitos`, `pasarela_pago`, `medio_pago`, la `referencia_externa_pago`, el `monto_pago`, la moneda y el código de autorización. Esto sigue buenas prácticas inspiradas en PCI-DSS, pero no constituye una certificación.
- **3FN:** La cantidad y el total de la compra se obtienen de las entradas. `monto_pago` es un hecho externo: el importe autorizado por la pasarela, y se usa para el cruce contra la suma de `entrada.precio_pagado_entrada`.

### ¿Qué hace exactamente el método que cruza la información al comprar?
Es el método transaccional `CompraService.comprarEntrada()`, anotado con `@Transactional`. En una sola unidad atómica cruza **8 tablas**:
1. Valida identidad `VERIFICADA` en `verificacion_identidad`.
2. Bloquea optimistamente el evento con `@Version` en `evento` y verifica el cupo libre contando `entrada`.
3. Crea la cabecera en `compra`.
4. Procesa el pago con `PagoGateway` (simulado en la demo) y valida monto y moneda antes de registrar la transacción aprobada en `pago`.
5. Emite cada ticket nominal con `dni_asistente_entrada` y `nombre_asistente_entrada` en `entrada` + su traza inicial en `historial_estado_entrada`.
6. Genera el `comprobante` calculando un hash `SHA-256` (`hash_cruce_seguridad`) que sella comprador + DNI + código KYC + referencia/monto del pago + UUIDs y datos nominales de las entradas, y envía el aviso en `notificacion`.
7. Deja constancia en `auditoria_seguridad`; los rechazos se auditan en una transacción independiente para que el rollback no borre la evidencia.

### ¿Por qué hicieron la entrada nominal en la tabla `entrada` y no en `compra`?
Porque un mismo comprador puede adquirir 3 entradas en una sola compra (para él y dos acompañantes). Si el DNI estuviera en `compra`, habría grupos repetitivos (violando **1FN**) o las 3 personas tendrían el mismo DNI en su entrada. Al colocar `nombre_asistente_entrada` y `dni_asistente_entrada` en cada fila de **`entrada`**, cada ticket QR pertenece a un asistente físico único y se valida en puerta contra su DNI real.
