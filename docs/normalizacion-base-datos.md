# Verificación de Normalización (1FN, 2FN, 3FN y BCNF) y Seguridad Informática - Butaca

> **Alcance de la demo:** la pasarela de pago y el proveedor de identidad son simulados. El proyecto no afirma certificación PCI-DSS ni integración real con RENAPER; las referencias a PCI-DSS describen decisiones de minimización de datos, no una certificación.

Este documento detalla el modelo relacional definitivo de **Butaca** (13 entidades normalizadas), incorporando todas las correcciones solicitadas por la cátedra:
1. **Verificación de Identidad Obligatoria antes de comprar (KYC)** (`verificacion_identidad` + `usuario.dni_usuario`).
2. **Pasarela de Pago desacoplada con minimización de datos inspirada en PCI-DSS** (`pago`).
3. **Método de Cruce de Información al comprar (Verificación + Pago + Envío de Entrada + Comprobante con firma SHA-256)** (`comprobante` + `CompraService.comprarEntrada`).
4. **Entradas Nominales por Asistente** (`entrada.dni_asistente_entrada`, `entrada.nombre_asistente_entrada`).
5. **Tracking y Auditoría de Datos de Seguridad Informática** (`auditoria_seguridad` + `historial_estado_entrada`).

El archivo gráfico editable en DBML es [`butaca.dbml`](../butaca.dbml) y el diagrama editable para Draw.io es [`diagrama Butaca.drawio`](../diagrama%20Butaca.drawio).

---

## 1. Diagrama Entidad-Relación (Mermaid ER Completo en 3FN)

Podés copiar este bloque `erDiagram` directamente en **Draw.io** (`Organizar -> Insertar -> Avanzado -> Mermaid...`) o visualizarlo en GitHub/Markdown:

```mermaid
erDiagram
    ROL {
        Bigint id_rol PK
        Varchar nombre_rol UK
        Timestamp created_at
        Timestamp updated_at
    }
    USUARIO {
        Bigint id_usuario PK
        Bigint id_rol FK
        Varchar nombre_usuario
        Varchar apellido_usuario
        Varchar dni_usuario UK
        Varchar email_usuario UK
        Varchar password_hash_usuario
        Varchar telefono_usuario
        Enum estado_usuario
        Timestamp created_at
        Timestamp updated_at
    }
    VERIFICACION_IDENTIDAD {
        Bigint id_verificacion PK
        Bigint id_usuario FK_UK
        Enum metodo_verificacion
        Enum estado_verificacion
        Varchar codigo_validacion_kyc UK
        Varchar observacion_verificacion
        Timestamp fecha_solicitud
        Timestamp fecha_verificacion
        Timestamp created_at
        Timestamp updated_at
    }
    UBICACION {
        Bigint id_ubicacion PK
        Varchar nombre_ubicacion
        Varchar direccion_ubicacion
        Integer capacidad_maxima_ubicacion
        Boolean activa_ubicacion
        Timestamp created_at
        Timestamp updated_at
    }
    CATEGORIA {
        Bigint id_categoria PK
        Varchar nombre_categoria UK
        Text descripcion_categoria
        Timestamp created_at
        Timestamp updated_at
    }
    EVENTO {
        Bigint id_evento PK
        Bigint id_organizador FK
        Bigint id_ubicacion FK
        Bigint id_categoria FK
        Varchar titulo_evento
        Text descripcion_evento
        Date fecha_evento
        Time hora_evento
        Decimal precio_actual_entrada
        Integer cupo_total_evento
        Enum estado_evento
        Text imagen_url_evento
        Integer version_evento
        Timestamp created_at
        Timestamp updated_at
    }
    COMPRA {
        Bigint id_compra PK
        Bigint id_cliente FK
        Timestamp fecha_compra
        Enum estado_compra
        Timestamp created_at
        Timestamp updated_at
    }
    PAGO {
        Bigint id_pago PK
        Bigint id_compra FK
        Enum pasarela_pago
        Enum medio_pago
        Enum estado_pago
        Varchar referencia_externa_pago UK
        Varchar ultimos_cuatro_digitos
        Timestamp fecha_pago
        Timestamp created_at
        Timestamp updated_at
    }
    COMPROBANTE {
        Bigint id_comprobante PK
        Bigint id_pago FK_UK
        Varchar numero_comprobante UK
        Enum tipo_comprobante
        Varchar hash_cruce_seguridad UK
        Enum estado_envio_comprobante
        Timestamp fecha_emision_comprobante
        Timestamp created_at
        Timestamp updated_at
    }
    ENTRADA {
        Bigint id_entrada PK
        UUID codigo_unico_entrada UK
        Bigint id_evento FK
        Bigint id_cliente FK
        Bigint id_compra FK
        Varchar dni_asistente_entrada
        Varchar nombre_asistente_entrada
        Enum estado_entrada
        Decimal precio_pagado_entrada
        Timestamp fecha_emision_entrada
        Timestamp fecha_validacion_entrada
        Integer version_entrada
        Timestamp created_at
        Timestamp updated_at
    }
    HISTORIAL_ESTADO_ENTRADA {
        Bigint id_historial_entrada PK
        Bigint id_entrada FK
        Enum estado_anterior_entrada
        Enum estado_nuevo_entrada
        Bigint id_usuario_responsable FK
        Varchar motivo_cambio_entrada
        Timestamp fecha_cambio_entrada
    }
    NOTIFICACION {
        Bigint id_notificacion PK
        Bigint id_usuario_destinatario FK
        Bigint id_evento_relacionado FK
        Text mensaje_notificacion
        Varchar tipo_notificacion
        Boolean leida_notificacion
        Timestamp fecha_creacion_notificacion
        Timestamp fecha_envio_notificacion
        Timestamp created_at
        Timestamp updated_at
    }
    AUDITORIA_SEGURIDAD {
        Bigint id_auditoria PK
        Bigint id_usuario FK
        Varchar accion_auditoria
        Varchar entidad_afectada
        Bigint id_registro_afectado
        Varchar resultado_auditoria
        Varchar ip_origen
        Varchar detalle_tracking
        Timestamp fecha_auditoria
    }

    ROL ||--o{ USUARIO : "asigna"
    USUARIO ||--o| VERIFICACION_IDENTIDAD : "verifica KYC"
    USUARIO ||--o{ EVENTO : "organiza"
    UBICACION ||--o{ EVENTO : "aloja"
    CATEGORIA ||--o{ EVENTO : "clasifica"
    USUARIO ||--o{ COMPRA : "realiza"
    COMPRA ||--o{ PAGO : "procesa en pasarela"
    PAGO ||--o| COMPROBANTE : "emite cruce SHA-256"
    COMPRA o|--o{ ENTRADA : "contiene"
    EVENTO ||--o{ ENTRADA : "emite nominal"
    USUARIO ||--o{ ENTRADA : "administra"
    ENTRADA ||--o{ HISTORIAL_ESTADO_ENTRADA : "audita estado"
    USUARIO o|--o{ HISTORIAL_ESTADO_ENTRADA : "responsabiliza"
    USUARIO ||--o{ NOTIFICACION : "recibe"
    EVENTO o|--o{ NOTIFICACION : "notifica"
    USUARIO o|--o{ AUDITORIA_SEGURIDAD : "trackea actividad"
```

---

## 2. Cumplimiento Riguroso de las 3 Formas Normales (3FN) y BCNF

### Primera Forma Normal (1FN)
- **Columnas atómicas e indivisibles**: Ninguna tabla almacena listas separadas por comas, arreglos JSON ni múltiples valores en una celda.
- **Sin grupos repetitivos**: Una compra de $N$ entradas genera $N$ filas independientes en `entrada`, cada una con su propio `codigo_unico_entrada` (UUID) y su propio asistente nominal (`dni_asistente_entrada`, `nombre_asistente_entrada`).

### Segunda Forma Normal (2FN)
- El modelo cumple 1FN y todas las entidades poseen una **Clave Primaria simple** (`id_...`).
- Todo atributo no primo depende funcionalmente de forma completa de la clave primaria de su tabla, sin dependencias parciales.

### Tercera Forma Normal (3FN) y Forma Normal de Boyce-Codd (BCNF)
No existen dependencias transitivas ($PK \to X \to Y$) ni atributos calculados redundantes:

1. **Separación de `verificacion_identidad` respecto de `usuario`**:
   - El DNI y nombre de la persona viven una única vez en `usuario` (`dni_usuario`, `nombre_usuario`, `apellido_usuario`).
   - `verificacion_identidad` almacena únicamente los atributos propios del proceso de validación KYC (`metodo_verificacion`, `estado_verificacion`, `codigo_validacion_kyc`, `fecha_verificacion`), referenciando a `id_usuario` mediante FK sin duplicar datos personales.
2. **Separación de `pago` respecto de `compra`**:
   - Una `compra` no almacena `total` ni `cantidad` (se derivan de sus filas en `entrada`).
   - `pago` referencia a `id_compra` y almacena los atributos propios de la pasarela (`pasarela_pago`, `medio_pago`, `estado_pago`, `referencia_externa_pago`, `ultimos_cuatro_digitos`). No repite `id_cliente` ni `monto_total`, evitando anomalías de actualización entre `compra`, `entrada` y `pago`.
3. **Encadenamiento en 3FN de `comprobante` hacia `pago`**:
   - `comprobante` se vincula directamente a `id_pago` (`UNIQUE FK`).
   - **Por qué NO tiene `id_compra` ni `id_cliente`**: Porque dado `id_pago`, la compra (`pago.id_compra`) y el cliente (`compra.id_cliente`) ya están unívocamente determinados. Incluir `id_compra` o `id_cliente` en `comprobante` introduciría una dependencia transitiva ($id\_comprobante \to id\_pago \to id\_compra \to id\_cliente$).
4. **Justificación de Entradas Nominales (`entrada`)**:
   - Mientras `compra.id_cliente` identifica al **comprador verificado que paga**, cada ticket individual (`entrada`) puede ser utilizado por un asistente distinto (por ejemplo, cuando una persona compra 3 entradas para sí misma y dos acompañantes).
   - `dni_asistente_entrada` y `nombre_asistente_entrada`, al igual que `precio_pagado_entrada`, congelan los datos nominales e históricos impresos en ese pase específico al momento de la emisión para su contraste con el DNI físico en puerta.

---

## 3. Respuesta a los 5 Puntos Solicitados por el Profesor

### A. ¿Cómo verificar la identidad de alguien en la app (obligatorio antes de comprar)?
Se implementó un flujo de **KYC (*Know Your Customer*) obligatorio**:
- **En la Base de Datos**: Tabla `verificacion_identidad` vinculada a `usuario` y restricción `UNIQUE` sobre `usuario.dni_usuario`.
- **Regla de Seguridad y Privacidad (Ley 25.326)**: En lugar de almacenar fotografías crudas del rostro o del frente/dorso del DNI en la base de datos (lo cual representa un riesgo crítico ante filtraciones), el sistema valida contra el proveedor (`BIOMETRIA_RENAPER`, `DOCUMENTO_DNI` o `OTP_EMAIL`) y persiste únicamente el **token de validación** (`codigo_validacion_kyc`), el estado, el proveedor, el puntaje y los **hashes SHA-256** de las evidencias. Las imágenes se procesan en memoria y no se guardan.
- **Bloqueo en Backend (`CompraService`)**: Antes de iniciar cualquier compra, `CompraService.comprarEntrada` consulta `verificacion_identidad`. Si el usuario no posee estado `VERIFICADA`, el backend **rechaza la transacción**, registra el intento bloqueado en `auditoria_seguridad` y exige completar la verificación.

### B. Pasarela de Pago desacoplada y minimización de datos (inspirada en PCI-DSS)
- **En la Base de Datos**: Tabla `pago` vinculada a `compra`.
- **Buenas prácticas de PCI-DSS (sin certificar cumplimiento)**:
  - **Prohibición absoluta de almacenar PAN y CVV**: La base de datos jamás recibe ni persiste los 16 dígitos de la tarjeta ni el código de seguridad CVV/CVC.
  - **Tokenización e Idempotencia**: Se almacena únicamente la `referencia_externa_pago` (`UNIQUE`, ej. `PAY-MP-...`) provista por la pasarela (`MERCADOPAGO`, `STRIPE`, `MODO`), los `ultimos_cuatro_digitos` (`VARCHAR(4)`) y el monto/moneda autorizados por el proveedor para el cruce.

### C. Método que cruza la información al comprar la entrada
En [`CompraService.java`](../src/main/java/com/demo/butaca/service/CompraService.java) el método `@Transactional comprarEntrada(CompraRequestDTO)` ejecuta de manera atómica (ACID) el cruce de 6 eslabones:
1. **Cruce de Identidad**: Verifica que `usuario` esté `ACTIVO` y su `verificacion_identidad` esté `VERIFICADA`.
2. **Cruce de Cupo y Concurrencia**: Bloquea optimistamente `evento` (`@Version`) y comprueba que `cupo_total_evento - entradas_vigentes >= cantidad`.
3. **Cruce con Pasarela de Pago**: Procesa el pago mediante `PagoGateway` (simulado en la demo) y valida que el monto y la moneda aprobados coincidan con el total calculado.
4. **Cruce de Nominatividad**: Emite cada `entrada` con su `UUID` único, `precio_pagado_entrada` congelado y los datos nominales de cada asistente (`dni_asistente_entrada`, `nombre_asistente_entrada`).
5. **Cruce de Comprobante y Envío Digital**: Genera la fila en `comprobante` con un **Hash criptográfico SHA-256** (`hash_cruce_seguridad`) calculado sobre `id_compra | dni_comprador | token_kyc | referencia_pago | UUIDs_entradas | timestamp`, marcando `estado_envio_comprobante = ENVIADO` y disparando la `notificacion` al cliente.
6. **Cruce de Auditoría**: Registra la operación en `historial_estado_entrada` y en `auditoria_seguridad`. Si cualquiera de los pasos falla, toda la transacción hace *rollback*.

### D. ¿Que la entrada sea nominal o qué es lo más óptimo?
La solución **más óptima tanto en seguridad operativa como en modelo relacional 3FN** es la **Entrada Nominal por Asistente con Comprador Verificado**:
- **Por qué no alcanza con una entrada al portador (anónima)**: Permite reventa especulativa, uso de bots y capturas de pantalla compartidas.
- **Por qué no obligar a que cada acompañante tenga cuenta previa en la app**: En compras familiares o grupales (ej. un padre comprando para sus hijos o un grupo de amigos), exigir cuenta individual a cada acompañante genera alta fricción de conversión.
- **Solución óptima implementada**:
  1. El **comprador** debe tener cuenta y **verificación de identidad KYC aprobada** (`verificacion_identidad`).
  2. Al comprar $N$ entradas, asigna el **Nombre y DNI de cada asistente** (`dni_asistente_entrada`, `nombre_asistente_entrada` en `entrada`), con índice compuesto `(id_evento, dni_asistente_entrada)`.
  3. En puerta, el organizador escanea el QR (`codigo_unico_entrada`) y verifica que el DNI físico del asistente coincida con `dni_asistente_entrada`.

### E. Trackear datos (Trazabilidad y Seguridad Informática)
El sistema implementa **doble nivel de tracking inmutable (*append-only*)**:
1. **Tracking de Dominio (`historial_estado_entrada`)**: Audita cada transición de estado de cada ticket (`CONFIRMADA -> USADA` o `ANULADA`), quién fue el responsable, fecha y motivo (incluyendo el comprobante y DNI validado en puerta).
2. **Tracking de Seguridad y Operaciones (`auditoria_seguridad`)**: Bitácora general bajo lineamientos **OWASP Logging & Monitoring / ISO 27001** que registra `LOGIN`, `REGISTRO`, `VERIFICACION_KYC`, `CRUCE_COMPRA_BLOQUEADO`, `CRUCE_COMPRA_EXITOSO` y `VALIDACION_PUERTA`, guardando usuario, entidad afectada, ID de registro, IP de origen, resultado (`EXITOSO`, `RECHAZADO`, `BLOQUEADO`) y fecha exacta.
