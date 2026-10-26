# GuÃ­a de presentaciÃ³n y consultas H2

Esta guÃ­a muestra el arranque y consultas sobre las 16 tablas del modelo vigente. Preparar dependencias y ensayar el recorrido antes de presentar.

## Iniciar la demo

Windows: ejecutar `.\iniciar-windows.bat`. macOS/Linux: `bash ./scripts/demo.sh`. Los scripts compilan la web y esperan al backend. Mantener la terminal abierta; Ctrl+C cierra los procesos. Los logs quedan en target/demo-logs/.

- Web: http://localhost:8081
- Consola H2: http://localhost:8081/h2-console
- JDBC: `jdbc:h2:mem:butaca_demo`; usuario `sa`; contraseÃ±a vacÃ­a.

| Rol | Email | ContraseÃ±a de demo |
| --- | --- | --- |
| Administrador | admin@butaca.com | admin123 |
| Organizador | org@butaca.com | org123 |
| Cliente | cliente@butaca.com | cliente123 |

H2 se recrea al arrancar. Los usuarios semilla ya tienen KYC; para demostrar el proceso completo registrar otra cuenta. El correo requiere configuraciÃ³n local explÃ­cita; ver [correo y eventos](correo-real-y-eventos.md).

## Identidad, cuenta y KYC

```sql
SELECT u.id_usuario, p.dni_persona, p.nombre_persona, p.apellido_persona,
       r.nombre_rol, v.estado_verificacion, v.proveedor_identidad
FROM usuario u
LEFT JOIN persona p ON p.id_persona = u.id_persona
JOIN rol r ON r.id_rol = u.id_rol
LEFT JOIN verificacion_identidad v ON v.id_usuario = u.id_usuario;
```

## Compra, pago y comprobante

```sql
SELECT c.id_compra, u.email_usuario, c.estado_compra,
       p.proveedor_pago, p.monto_pago, p.moneda_pago, p.referencia_externa_pago,
       cb.numero_comprobante, cb.estado_envio_comprobante, cb.hash_cruce_seguridad
FROM compra c
JOIN usuario u ON u.id_usuario = c.id_usuario_comprador
JOIN pago p ON p.id_compra = c.id_compra
JOIN comprobante cb ON cb.id_pago = p.id_pago
ORDER BY c.id_compra DESC;
```

## Entradas nominales y tarifas

```sql
SELECT e.id_entrada, e.codigo_unico_entrada, p.dni_persona,
       p.nombre_persona, p.apellido_persona, t.nombre_tipo_entrada,
       e.precio_pagado_entrada, e.estado_entrada, ev.titulo_evento
FROM entrada e
JOIN persona p ON p.id_persona = e.id_persona_titular
LEFT JOIN tipo_entrada t ON t.id_tipo_entrada = e.id_tipo_entrada
JOIN evento ev ON ev.id_evento = e.id_evento;
```

## Historial y auditorÃ­a

```sql
SELECT id_historial_entrada, id_entrada, estado_anterior_entrada,
       estado_nuevo_entrada, motivo_cambio_entrada, fecha_cambio_entrada
FROM historial_estado_entrada
ORDER BY fecha_cambio_entrada DESC;

SELECT accion_auditoria, resultado_auditoria, detalle_tracking, fecha_auditoria
FROM auditoria_seguridad
ORDER BY fecha_auditoria DESC;
```

## Cola de correo

```sql
SELECT id_notificacion, id_compra, tipo_notificacion, estado_envio_notificacion,
       fecha_envio_notificacion, leida_notificacion
FROM notificacion
ORDER BY id_notificacion DESC;
```

La compra puede estar confirmada aunque el correo estÃ© pendiente o falle. Mostrar esa diferencia y aclarar que la pasarela y los reembolsos de la demo no operan dinero real.
