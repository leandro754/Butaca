# Verificación de Normalización (1FN, 2FN, 3FN y BCNF) y Seguridad Informática - Butaca

> **Alcance de la demo:** la pasarela de pago y el proveedor de identidad son simulados. El proyecto no afirma certificación PCI-DSS ni integración real con RENAPER; las referencias a PCI-DSS describen decisiones de minimización de datos, no una certificación.

Este documento detalla el modelo relacional implementado en **Butaca** (16 entidades), con las decisiones de diseño trabajadas para la entrega académica:
1. **Verificación de Identidad Obligatoria antes de comprar (KYC)** (`verificacion_identidad` + `persona.dni_persona`).
2. **Pasarela de Pago desacoplada con minimización de datos inspirada en PCI-DSS** (`pago`).
3. **Método de Cruce de Información al comprar (Verificación + Pago + Envío de Entrada + Comprobante con firma SHA-256)** (`comprobante` + `CompraService.comprarEntrada`).
4. **Entradas Nominales por Asistente** (`persona.dni_persona`, `persona.nombre_persona`).
5. **Tracking y Auditoría de Datos de Seguridad Informática** (`auditoria_seguridad` + `historial_estado_entrada`).

El archivo gráfico editable en DBML es [`butaca.dbml`](../butaca.dbml) y el diagrama DrawDB de referencia es [`docs/drawdb-butaca-modelado-datos.json`](drawdb-butaca-modelado-datos.json).

---

## 1. Diagrama Entidad-Relación (Mermaid ER Completo en 3FN)

Podés copiar este bloque `erDiagram` directamente en **Draw.io** (`Organizar -> Insertar -> Avanzado -> Mermaid...`) o visualizarlo en GitHub/Markdown:

```mermaid
erDiagram
  rol {
    bigint id_rol PK
    varchar nombre_rol UK
    timestamp created_at
    timestamp updated_at
  }
  usuario {
    bigint id_usuario PK
    bigint id_persona
    bigint id_rol
    estado_usuario estado_usuario
    varchar email_usuario UK
    varchar password_hash_usuario
    varchar telefono_usuario
    timestamp created_at
    timestamp updated_at
  }
  persona {
    bigint id_persona PK
    bigint dni_persona UK
    varchar nombre_persona
    varchar apellido_persona
    varchar email_persona
    varchar telefono_persona
    timestamp created_at
    timestamp updated_at
  }
  ubicacion {
    bigint id_ubicacion PK
    varchar nombre_ubicacion
    varchar direccion_ubicacion
    int capacidad_maxima_ubicacion
    boolean activa_ubicacion
    timestamp created_at
    timestamp updated_at
  }
  categoria {
    bigint id_categoria PK
    varchar nombre_categoria UK
    text descripcion_categoria
    timestamp created_at
    timestamp updated_at
  }
  evento {
    bigint id_evento PK
    bigint id_usuario_organizador
    bigint id_ubicacion
    bigint id_categoria
    varchar titulo_evento
    text descripcion_evento
    date fecha_evento
    time hora_evento
    int cupo_total_evento
    estado_evento estado_evento
    text imagen_url_evento
    bigint version_evento
    timestamp created_at
    timestamp updated_at
  }
  entrada {
    bigint id_entrada PK
    uuid codigo_unico_entrada UK
    bigint id_evento
    bigint id_tipo_entrada
    bigint id_persona_titular
    bigint id_compra
    estado_entrada estado_entrada
    numeric precio_pagado_entrada
    char moneda_entrada
    timestamp fecha_emision_entrada
    timestamp fecha_validacion_entrada
    bigint version_entrada
    timestamp created_at
    timestamp updated_at
  }
  compra {
    bigint id_compra PK
    bigint id_usuario_comprador
    timestamp fecha_compra
    estado_compra estado_compra
    char moneda_compra
    timestamp created_at
    timestamp updated_at
  }
  notificacion {
    bigint id_notificacion PK
    bigint id_usuario
    bigint id_evento
    bigint id_compra
    varchar tipo_notificacion
    varchar asunto_notificacion
    text mensaje_notificacion
    estado_envio_notificacion estado_envio_notificacion
    boolean leida_notificacion
    timestamp fecha_envio_notificacion
    text error_envio
    timestamp created_at
    timestamp updated_at
  }
  tipo_entrada {
    bigint id_tipo_entrada PK
    bigint id_evento
    varchar nombre_tipo_entrada
    text descripcion_tipo_entrada
    numeric precio_tipo_entrada
    char moneda_tipo_entrada
    boolean activo_tipo_entrada
    timestamp created_at
    timestamp updated_at
  }
  pago {
    bigint id_pago PK
    bigint id_compra
    varchar proveedor_pago
    varchar referencia_externa_pago
    numeric monto_pago
    char moneda_pago
    estado_pago estado_pago
    timestamp fecha_creacion_pago
    timestamp fecha_confirmacion_pago
    text respuesta_proveedor_pago
    timestamp created_at
    timestamp updated_at
  }
  reembolso {
    bigint id_reembolso PK
    bigint id_pago
    numeric monto_reembolso
    char moneda_reembolso
    text motivo_reembolso
    estado_reembolso estado_reembolso
    varchar referencia_externa_reembolso
    timestamp fecha_solicitud_reembolso
    timestamp fecha_procesamiento_reembolso
    timestamp created_at
    timestamp updated_at
  }
  verificacion_identidad {
    bigint id_verificacion PK
    bigint id_usuario UK
    metodo_verificacion metodo_verificacion
    estado_verificacion estado_verificacion
    varchar codigo_validacion_kyc UK
    varchar observacion_verificacion
    timestamp fecha_solicitud
    timestamp fecha_verificacion
    timestamp created_at
    timestamp updated_at
    varchar hash_evidencia_documental
    varchar hash_selfie
    varchar proveedor_identidad
    decimal puntaje_coincidencia
  }
  comprobante {
    bigint id_comprobante PK
    bigint id_pago UK
    varchar numero_comprobante UK
    tipo_comprobante tipo_comprobante
    varchar hash_cruce_seguridad UK
    estado_envio_comprobante estado_envio_comprobante
    timestamp fecha_emision_comprobante
    timestamp created_at
    timestamp updated_at
  }
  historial_estado_entrada {
    bigint id_historial_entrada PK
    bigint id_entrada
    bigint id_usuario_responsable
    estado_entrada estado_anterior_entrada
    estado_entrada estado_nuevo_entrada
    varchar motivo_cambio_entrada
    timestamp fecha_cambio_entrada
  }
  auditoria_seguridad {
    bigint id_auditoria PK
    bigint id_usuario
    varchar accion_auditoria
    varchar entidad_afectada
    bigint id_registro_afectado
    varchar resultado_auditoria
    varchar ip_origen
    varchar detalle_tracking
    timestamp fecha_auditoria
  }
  persona ||--o{ usuario : "fk_usuario_persona"
  rol ||--o{ usuario : "fk_usuario_rol"
  usuario ||--o{ evento : "fk_evento_organizador"
  ubicacion ||--o{ evento : "fk_evento_ubicacion"
  categoria ||--o{ evento : "fk_evento_categoria"
  usuario ||--o{ compra : "fk_compra_usuario"
  compra ||--o{ entrada : "fk_entrada_compra"
  evento ||--o{ entrada : "fk_entrada_evento"
  persona ||--o{ entrada : "fk_entrada_titular"
  usuario ||--o{ notificacion : "fk_notificacion_usuario"
  evento ||--o{ notificacion : "fk_notificacion_evento"
  compra ||--o{ notificacion : "fk_compra_id_compra_notificacion"
  evento ||--o{ tipo_entrada : "fk_tipo_entrada_evento"
  tipo_entrada ||--o{ entrada : "fk_entrada_tipo_entrada"
  compra ||--o{ pago : "fk_pago_compra"
  pago ||--o{ reembolso : "fk_reembolso_pago"
  verificacion_identidad ||--|| usuario : "fk_verificacion_usuario"
  comprobante ||--|| pago : "fk_comprobante_pago"
  entrada ||--o{ historial_estado_entrada : "fk_historial_entrada"
  usuario ||--o{ historial_estado_entrada : "fk_historial_usuario_responsable"
  usuario ||--o{ auditoria_seguridad : "fk_auditoria_usuario"

```

---

## 2. NormalizaciÃ³n y datos histÃ³ricos

La identidad civil pertenece a `persona`. `usuario` contiene credenciales y referencias a persona y rol; los acompaÃ±antes pueden tener una persona titular sin crear una cuenta.

Cada evento referencia una ubicaciÃ³n y una categorÃ­a. Las tarifas residen en `tipo_entrada`; los precios actuales se separan de los histÃ³ricos que conserva cada `entrada`. El DNI y el nombre que devuelve el DTO del ticket se proyectan desde su persona titular.

Una compra agrupa filas de entrada. Cantidad, total y cupo disponible se calculan desde esas filas. El monto de pago se conserva porque es el importe externo autorizado, que debe coincidir con la suma de entradas.

El comprobante referencia al pago, por el que se recuperan compra y comprador. Los reembolsos se vinculan al pago. No se duplican compra ni cliente en comprobante.

Las claves primarias simples evitan dependencias parciales de una clave compuesta. La separaciÃ³n entre persona, cuenta, tarifa, transacciÃ³n y comprobante reduce dependencias transitivas. La normalizaciÃ³n debe revisarse con las dependencias funcionales del dominio; la presencia de un ID por tabla no demuestra por sÃ­ sola 3FN o BCNF.

## 3. Compra, integridad y concurrencia

La compra exige usuario activo, KYC verificado y datos coherentes del comprador. Valida asistentes, disponibilidad y tarifa del evento; procesa el pago mediante el adaptador simulado y contrasta monto y moneda.

En una transacciÃ³n guarda compra, pago, tickets, historial, comprobante y notificaciÃ³n pendiente. El hash SHA-256 usa la fecha, comprador, token KYC, monto y referencia del pago, UUIDs y titulares. El endpoint de cruce recalcula esa representaciÃ³n con los datos persistidos. No constituye una firma digital certificada.

Evento y entrada usan `@Version` para detectar escrituras concurrentes. Las pruebas de tarifas comprueban que editar el precio de catÃ¡logo no cambia el histÃ³rico. Queda pendiente ampliar pruebas concurrentes y de carga.

## 4. Evidencias, envÃ­o y auditorÃ­a

La implementaciÃ³n integra cuatro campos de evidencia KYC en `verificacion_identidad`: hashes SHA-256 de documento y selfie, proveedor y puntaje. Las imÃ¡genes se procesan en memoria y no se guardan. El snapshot DrawDB se conserva como referencia; el DBML y el DDL locales incluyen estas extensiones.

Pago no guarda PAN ni CVV. Su respuesta reducida conserva medio, Ãºltimos cuatro dÃ­gitos y cÃ³digo de autorizaciÃ³n; monto y moneda tienen una sola columna cada uno.

El trabajador de correo opera despuÃ©s del commit. Sin SMTP configurado, la notificaciÃ³n sigue pendiente; una aceptaciÃ³n SMTP habilita el estado enviado. Un fallo de correo no revierte la compra.

Historial de entrada y auditorÃ­a registran operaciones de dominio y seguridad. Los rechazos usan transacciones independientes para conservar evidencia. La aplicaciÃ³n no demuestra certificaciÃ³n PCI-DSS, ISO 27001 ni inmutabilidad frente a un administrador de base.

El control nominal de DNI es visual y la autorizaciÃ³n de la API mediante sesiÃ³n y rol sigue pendiente. KYC, pasarela y reembolsos son simulados; SMTP es real solo cuando se habilita.
