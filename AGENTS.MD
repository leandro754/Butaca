# Directrices Para Trabajar En Butaca

Este repositorio debe mantenerse alineado con `srs_proyecto.md`. Antes de cambiar arquitectura, contratos REST, modelo de datos o flujos de UI, leer ese archivo y este documento.

## Objetivo Técnico

Butaca es una plataforma academica de gestion y venta simulada de entradas para eventos. El backend Spring Boot define el dominio relacional y el frontend React consume la API REST bajo `/api`.

## Reglas Del Dominio

- Roles validos: `ADMIN`, `ORGANIZADOR`, `CLIENTE`.
- Estados de evento: `BORRADOR`, `PUBLICADO`, `CANCELADO`.
- Estados de compra: `PENDIENTE`, `CONFIRMADA`, `CANCELADA`.
- Estados de entrada: `CONFIRMADA`, `USADA`, `ANULADA`.
- Los clientes solo deben ver eventos `PUBLICADO` con cupos disponibles.
- Un evento pertenece a un organizador y a una ubicacion.
- Una entrada pertenece a un evento, a un cliente y opcionalmente a una compra.
- Las compras congelan el precio historico en cada entrada mediante `precio_pagado`.
- Los cupos disponibles, la cantidad de una compra y su total se calculan desde
  las entradas; no se persisten como columnas.
- El control de concurrencia de eventos se apoya en `Evento.version` con `@Version`.
- El uso concurrente de tickets se protege con `Entrada.versionEntrada`.

## Modelo De Datos

El modelo normalizado vigente en 3FN sincronizado con DrawDB (`butaca-modelado-datos`) usa estas 16 entidades en 5 módulos:

- `rol`: catalogo de roles (`ADMIN`, `ORGANIZADOR`, `CLIENTE`).
- `persona`: identidad real con `dni_persona` unico, nombre, apellido, email y telefono de contacto.
- `usuario`: credenciales, estado de cuenta, vinculo a `persona` y `rol`.
- `verificacion_identidad`: validacion KYC obligatoria antes de comprar (`VERIFICADA`), vinculada 1:1 a `usuario` sin duplicar DNI ni nombre.
- `ubicacion`: lugares fisicos con direccion, capacidad maxima (`capacidad_maxima_ubicacion`) y estado activo.
- `categoria`: clasificacion de eventos.
- `evento`: publicacion, cupo total, categoria, ubicacion, organizador y `version_evento` (`@Version`).
- `tipo_entrada`: tarifas configuradas por evento (`nombre_tipo_entrada`, `precio_tipo_entrada`, `moneda_tipo_entrada`, `activo_tipo_entrada`).
- `compra`: cabecera minima con cliente comprador (`id_usuario_comprador`), fecha, estado y moneda.
- `entrada`: tickets nominales emitidos con UUID unico, vinculo a `evento`, `tipo_entrada`, `persona` titular (`id_persona_titular`), `compra`, precio historico y `version_entrada`.
- `historial_estado_entrada`: trazabilidad de emision, uso nominal en puerta y anulacion.
- `pago`: transaccion de pasarela desacoplada (`proveedor_pago`, `referencia_externa_pago`, `monto_pago`, `moneda_pago`, `estado_pago`).
- `reembolso`: devoluciones totales o parciales asociadas a un `pago`.
- `comprobante`: recibo/factura vinculada 1:1 a `pago` con firma SHA-256 de cruce de informacion y estado de envio digital.
- `notificacion`: mensajes internos del sistema con estado de envio y lectura (`leida_notificacion`).
- `auditoria_seguridad`: bitacora append-only de tracking de seguridad e informatica.

No volver a guardar datos de ubicacion como texto duplicado en `evento` si ya
existe `id_ubicacion`. No reintroducir `cupos_disponibles`, `cantidad`,
`total` ni `id_evento` en `compra` o `pago`: esos valores se obtienen desde `entrada`.
No duplicar `id_compra` ni `id_cliente` en `comprobante` (se obtienen desde `id_pago`).

## Backend

- Mantener controladores delgados; la regla de negocio vive en `service`.
- Mantener DTOs con `@JsonProperty` en snake_case para compatibilidad con la API actual.
- Evitar strings magicos para estados; usar enums del paquete `model`.
- No crear endpoints nuevos sin documentarlos en `README.md`.
- No duplicar handlers globales de excepciones. Usar `ApiExceptionHandler`.
- No exponer passwords ni hashes en DTOs de respuesta.

Comandos esperados:

```bash
./mvnw compile
./mvnw test
./mvnw spring-boot:run
./scripts/demo.sh
```

Requisito local: Java 17 instalado y `JAVA_HOME` configurado.

## Frontend

- Consumir la API desde `frontend/src/services/`.
- No leer usuarios desde claves sueltas de `localStorage`; usar `butaca-auth-user`.
- Normalizar la respuesta de auth en `authService.normalizeAuthUser`.
- Mantener `VITE_API_BASE_URL` configurable y con fallback a `http://localhost:8080/api`.
- Usar componentes y estilos existentes antes de introducir librerias nuevas.
- Usar `pnpm` como unico gestor de paquetes del frontend.
- Mantener `frontend/pnpm-lock.yaml` como lockfile versionable.
- No recrear ni versionar `package-lock.json`, `yarn.lock` ni locks de otros gestores.
- Si Vite falla por bindings nativos de Rolldown en WSL, ejecutar `pnpm install` dentro de `frontend/`.

Comandos esperados:

```bash
cd frontend
pnpm install
pnpm run dev
pnpm run build
```

## Dependencias

- Backend: Java 17, Spring Boot, Spring Web MVC, Spring Data JPA, Validation, PostgreSQL, BCrypt, Lombok, Spring Mail (SMTP) y ZXing Core (QR de adjuntos).
- Demo local: perfil `demo` con H2 en memoria y datos semilla de `DataLoader`.
- Frontend: React, Vite, React Router DOM, gestionados con `pnpm`.
- No agregar dependencias para resolver problemas que pueden resolverse con APIs nativas o codigo existente.

## Editor Y Archivos Locales

- Usar Visual Studio Code como editor del proyecto.
- Mantener solo `.vscode/settings.json` y `.vscode/extensions.json` como configuracion versionable de editor.
- No versionar ni recrear carpetas de otros editores o agentes: `.idea/`, `.opencode/`, `.agents/`, `.codex/`.
- No versionar salidas generadas: `frontend/dist/`, `target/`, `node_modules/`.
- No versionar bundles en `src/main/resources/static/`. El build de Vite se genera en `frontend/dist/` y Maven lo incorpora como recursos bajo `static/`.
- Los lanzadores `iniciar-windows.bat` e `iniciar-mac.command` delegan en `scripts/demo.ps1` y `scripts/demo.sh`; `-Dev`/`--dev` agrega Vite. El backend usa 8081 por defecto; los puertos se eligen explicitamente.
- Si se necesita limpiar el workspace, eliminar artefactos generados y luego reinstalar dependencias con `pnpm install`.

## Documentacion

- No incluir nombres reales del equipo, teléfonos, correos, dominios personales ni identificadores reales en archivos versionados o datos de prueba.
- Usar roles genéricos y datos sintéticos; los correos de prueba nuevos deben usar el dominio reservado `example.invalid`.
- Configurar remitente, destinatario de pruebas y credenciales SMTP mediante variables de entorno o `secrets/mail.properties`, ignorado por Git.
- Usar identidad genérica del proyecto para nuevos commits. Reescribir historial compartido requiere autorización explícita y coordinación entre equipos.

Al cambiar contratos, actualizar:

- `README.md`
- `srs_proyecto.md` si cambia un requisito
- este `AGENTS.MD` si cambia una regla de trabajo

## Verificacion Minima Antes De Entregar

- Backend compila con `./mvnw compile`.
- Frontend compila con `pnpm run build`.
- Demo integral levanta con `./scripts/demo.sh` cuando Java 17, pnpm y dependencias Maven ya estan disponibles.
- Login, registro, listado de eventos, compra, panel organizador y panel cliente siguen consumiendo endpoints reales.
- No commitear secretos, `.env`, certificados, `node_modules`, `target` ni `frontend/dist`.
