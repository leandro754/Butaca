# API REST

Base: `http://localhost:8081/api`. Los DTO usan `snake_case`. El acceso es de demo; no hay sesiones ni tokens del servidor.

## Autenticación

- `POST /api/auth/register`
- `POST /api/auth/login`

## Identidad / KYC

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

## Eventos

- `GET /api/eventos`
- `GET /api/eventos/{id}`
- `GET /api/eventos/organizador/{id}`
- `GET /api/eventos/categoria/{categoriaId}`
- `GET /api/eventos/{id}/entradas`
- `POST /api/eventos`
- `PUT /api/eventos/{id}`
- `PUT /api/eventos/{id}/publicar`
- `PUT /api/eventos/{id}/cancelar`

## Compras y pago

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
  "tipo_entrada_id": 1,
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

## Entradas y validación en puerta

- `GET /api/entradas/validar/{codigo}`
- `PUT /api/entradas/{id}/usar`
- `GET /api/entradas/usuario/{id}`

## Administración

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


## Notificaciones

- `POST /api/notificaciones`
- `GET /api/notificaciones/usuario/{id}`
- `GET /api/notificaciones/evento/{eventoId}`
- `PUT /api/notificaciones/{id}/leida`

## Contratos de eventos de la version integrada

La edicion acepta `version` y `tipos_entrada`. La cancelacion requiere JSON con `confirmar_reembolsos`, `titulo_confirmacion` y `version`. Consultar los nombres exactos de estos campos en los DTO y la guia de correo y eventos. Las consultas de notificaciones se marcan con `PUT /api/notificaciones/{id}/leida`.
