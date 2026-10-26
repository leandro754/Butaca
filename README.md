# Butaca

Plataforma académica de gestión de eventos y venta simulada de entradas nominales. Desarrollada por **MLSN Group** con Java 17, Spring Boot, React y una API REST bajo `/api`.

El circuito principal incluye registro, verificación de identidad, compra, pago simulado, emisión de tickets QR y consulta por rol. Se conservan las tarifas por evento, los reembolsos simulados y la cola de correo SMTP opcional. El modelo relacional tiene **16 entidades**; cada entrada conserva su precio histórico y los datos de su asistente.

**Estado:** proyecto en desarrollo, con un circuito de demostración funcional. Los pendientes para la presentación y la evolución del sistema se registran en [estado y próximos pasos](docs/estado-del-proyecto.md).

La verificación de identidad y la pasarela de pago son simuladas. La autenticación local permite demostrar las vistas por rol; no implementa sesiones ni autorización de producción. Ver [alcance técnico](docs/arquitectura.md).

## Requisitos

| Herramienta | Versión / uso |
| --- | --- |
| JDK | Java 17 como versión objetivo; configurar `JAVA_HOME` |
| Node.js | `.nvmrc` fija 22.20.0; también se admiten 20.19+, 22.12+ y versiones posteriores a 22 |
| pnpm | 9 o superior, según `frontend/package.json` |
| Maven | Incluido mediante `mvnw` / `mvnw.cmd` |
| PostgreSQL | Opcional; la demo usa H2 en memoria |

La primera ejecución necesita Internet para resolver dependencias de Maven y pnpm. Si está disponible, los scripts usan `corepack pnpm`; también aceptan pnpm instalado. En macOS/Linux se requieren Bash, curl y pgrep.

## Ejecutar la demo

Desde la raíz del repositorio, en Windows:

```powershell
.\iniciar-windows.bat
```

En macOS/Linux:

```bash
bash ./scripts/demo.sh
```

En macOS también se puede usar `bash ./iniciar-mac.command` o abrir ese archivo desde Finder si tiene permiso de ejecución.

Los scripts verifican los puertos, instalan dependencias con el lockfile, compilan el frontend y limpian la salida anterior de Maven. Spring Boot sirve la web y la API en **http://localhost:8081**. Esperan una respuesta de los servidores antes de anunciar que la demo está lista. **Ctrl+C** detiene los procesos iniciados por el script; los logs quedan en `target/demo-logs/`.

La base H2 se recrea en cada arranque. No hace falta importar SQL ni configurar PostgreSQL.

| Rol | Email | Contraseña de demo |
| --- | --- | --- |
| Administrador | `admin@butaca.com` | `admin123` |
| Organizador | `org@butaca.com` | `org123` |
| Cliente | `cliente@butaca.com` | `cliente123` |

Estas cuentas tienen identidad verificada. Para mostrar KYC desde cero, registrar otra cuenta. Se pueden usar imágenes de prueba: el proveedor es simulado y las fotos no se persisten.

Consola H2: `http://localhost:8081/h2-console`, JDBC `jdbc:h2:mem:butaca_demo`, usuario `sa`, contraseña vacía.

## Desarrollo con Vite

Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo.ps1 -Dev
```

macOS/Linux:

```bash
bash ./scripts/demo.sh --dev
```

La web queda en `http://localhost:5173` y Vite envía `/api` al backend en `8081`. Para cambiar los puertos:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo.ps1 -Dev -BackendPort 8099 -FrontendPort 5174
```

```bash
SERVER_PORT=8099 FRONTEND_PORT=5174 bash ./scripts/demo.sh --dev
```

Un puerto ocupado genera un error explícito; no se cambia silenciosamente. Las opciones de backend también sirven para la demo sin Vite. Ver [desarrollo y configuración](docs/desarrollo.md).

## Verificación

Windows:

```powershell
.\mvnw.cmd compile
.\mvnw.cmd test
cd frontend
corepack pnpm run build
```

macOS/Linux:

```bash
./mvnw compile
./mvnw test
cd frontend
pnpm run build
```

Las pruebas de integración comprueban catálogo, compra nominal, verificación KYC, rechazos de pago, auditoría persistente e integridad del comprobante. No incluyen una prueba de carga ni una prueba concurrente de sobreventa.

## Estructura

```text
src/main/java/com/demo/butaca/  Backend: controladores, servicios, dominio e integraciones
src/main/resources/           Configuración de Spring Boot y perfiles
src/test/                     Pruebas de integración
frontend/src/                 Interfaz React y servicios HTTP
scripts/                      Arranque de demo y desarrollo
docs/                         Arquitectura, API, modelo y material de exposición
butaca.dbml                   Modelo relacional de referencia
docs/drawdb-butaca-modelado-datos.json  Diagrama DrawDB de 16 entidades
srs_proyecto.md               Especificación de requisitos
```

El frontend compilado se genera en `frontend/dist/`; Maven lo copia a `target/classes/static/` al procesar recursos. Los bundles no se versionan ni se copian manualmente a `src/`. Para generar un JAR con la web incluida, ver [la guía de desarrollo](docs/desarrollo.md#empaquetar-la-aplicación).

## Documentación

- [Índice y fuentes de referencia](docs/README.md).
- [Requisitos del sistema](srs_proyecto.md).
- [Arquitectura e integraciones simuladas](docs/arquitectura.md).
- [API REST](docs/api.md).
- [Correo SMTP y configuración de eventos](docs/correo-real-y-eventos.md).
- [Desarrollo, PostgreSQL y solución de problemas](docs/desarrollo.md).
- [Modelo de datos y normalización](docs/normalizacion-base-datos.md).
- [Guía para la demo y consultas SQL](docs/guia-presentacion.md).
- [Plan de exposición de diez minutos](docs/plan-exposicion-10-minutos.md).

## Equipo

Mariano Piovesan · Leandro Gramajo Gargiulo · Santiago González Fernández · Ignacio Molina Tula.

## Trabajo desde dos equipos

Antes de empezar, usar `git switch fix/kyc-pago-auditoria` y `git pull --ff-only` para actualizar la misma rama. Al terminar, crear un commit y ejecutar `git push`. Si la rama base cambia, integrar `origin/master` en la rama de trabajo y resolver los conflictos antes de completar el PR. Los cambios sin commit y los secretos locales no se sincronizan con Git.
