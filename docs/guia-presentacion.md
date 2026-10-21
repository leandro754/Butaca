# Guía de Presentación y Consultas en Vivo — Butaca (13 Tablas)

Esta guía resume los pasos para iniciar el proyecto y las consultas SQL listas para copiar y pegar en la consola H2 durante la exposición frente al profesor.

## 1. Iniciar la Aplicación

En Windows, hacer doble clic en `iniciar-windows.bat` o ejecutar en PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Abrir en el navegador:
- **Web App:** `http://localhost:8080`
- **Consola H2:** `http://localhost:8080/h2-console`
  - **JDBC URL:** `jdbc:h2:mem:butaca_demo`
  - **User Name:** `sa`
  - **Password:** *(vacío)*

### Credenciales Demo Precargadas

| Rol | Email | Password |
| --- | --- | --- |
| **Administrador** | `admin@butaca.com` | `admin123` |
| **Organizador** | `org@butaca.com` | `org123` |
| **Cliente** | `cliente@butaca.com` | `cliente123` |

---

## 2. Consultas SQL en Consola H2 para Mostrar las 13 Tablas al Profesor

### A. Ver Usuarios, Roles y Estado de Verificación KYC (1:1)
```sql
SELECT u.id_usuario, u.nombre_usuario, u.apellido_usuario, u.dni_usuario,
       r.nombre_rol, v.metodo_verificacion, v.estado_verificacion, v.codigo_validacion_kyc
FROM usuario u
JOIN rol r ON r.id_rol = u.id_rol
LEFT JOIN verificacion_identidad v ON v.id_usuario = u.id_usuario
ORDER BY u.id_usuario;
```

### B. Ver el Cruce Completo de Compra + Pasarela de Pago + Comprobante (SHA-256)
```sql
SELECT c.id_compra, u.email_usuario, c.estado_compra,
       p.pasarela_pago, p.medio_pago, p.estado_pago, p.ultimos_cuatro_digitos,
       p.referencia_externa_pago, p.monto_pago, p.moneda_pago, p.codigo_autorizacion_pago,
       cb.numero_comprobante, cb.estado_envio_comprobante, cb.hash_cruce_seguridad
FROM compra c
JOIN usuario u ON u.id_usuario = c.id_cliente
JOIN pago p ON p.id_compra = c.id_compra
JOIN comprobante cb ON cb.id_pago = p.id_pago
ORDER BY c.id_compra DESC;
```

### C. Ver Entradas Nominales Emitidas (QR + DNI de cada Asistente)
```sql
SELECT e.id_entrada, e.codigo_unico_entrada,
       e.nombre_asistente_entrada, e.dni_asistente_entrada,
       e.precio_pagado_entrada, e.estado_entrada, ev.titulo_evento
FROM entrada e
JOIN evento ev ON ev.id_evento = e.id_evento
ORDER BY e.id_entrada DESC;
```

### D. Ver Trackeo en Historial de Estados de Entrada
```sql
SELECT h.id_historial_estado_entrada, h.id_entrada,
       h.estado_anterior_entrada, h.estado_nuevo_entrada,
       h.motivo_cambio_entrada, h.fecha_cambio_entrada
FROM historial_estado_entrada h
ORDER BY h.fecha_cambio_entrada DESC;
```

### E. Ver Trackeo en Bitácora Inmutable de Auditoría de Seguridad
```sql
SELECT a.id_auditoria, a.accion_auditoria, a.entidad_afectada,
       a.id_registro_afectado, a.resultado_operacion,
       a.detalle_cruce, a.fecha_evento_auditoria
FROM auditoria_seguridad a
ORDER BY a.fecha_evento_auditoria DESC;
```
