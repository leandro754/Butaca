# Desarrollo y configuración

## Arranque y cierre

Los scripts se pueden ejecutar desde cualquier directorio: resuelven la raíz usando su propia ubicación. El modo predeterminado es la demo completa en un solo puerto; `-Dev` (PowerShell) y `--dev` (Bash) habilitan Vite. Las cuentas y las URLs están en el [README](../README.md).

| Opción | Windows | macOS/Linux |
| --- | --- | --- |
| Desarrollo | `scripts/demo.ps1 -Dev` | `bash scripts/demo.sh --dev` |
| Puerto backend | `-BackendPort 8099` | `SERVER_PORT=8099` |
| Puerto frontend | `-FrontendPort 5174` | `FRONTEND_PORT=5174` |
| Ayuda | `Get-Help .\scripts\demo.ps1` | `bash scripts/demo.sh --help` |

Se ejecuta `pnpm install --frozen-lockfile` y, en presentación, `pnpm run build:demo`. Después se limpia `target/` con Maven, se inicia el backend con el perfil `demo` y se verifica `/api/eventos`. En desarrollo se inicia además Vite con puerto estricto y proxy hacia el backend elegido. Un fallo de inicio termina los procesos que haya iniciado el script.

Mantener la terminal abierta durante la demo. Usar Ctrl+C para cerrar. Los logs quedan en `target/demo-logs/`; la siguiente ejecución los reemplaza al limpiar Maven. H2 en memoria pierde sus datos al terminar el backend.

## Ejecutar servicios por separado

Backend en Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Backend en macOS/Linux:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

En otra terminal, desde `frontend/`, ejecutar `pnpm install --frozen-lockfile` y `pnpm run dev`. Sin un build previo en `frontend/dist/`, Maven sirve la API y Vite sirve la web. El arranque integral prepara ambos automáticamente.

## Empaquetar la aplicación

Desde la raíz, en Windows:

```powershell
Push-Location frontend
corepack pnpm install --frozen-lockfile
corepack pnpm run build:demo
Pop-Location
.\mvnw.cmd clean verify
java -jar target/Butaca-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

En macOS/Linux:

```bash
(cd frontend && pnpm install --frozen-lockfile && pnpm run build:demo)
./mvnw clean verify
java -jar target/Butaca-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Maven copia `frontend/dist/` a `target/classes/static/` e incorpora esos recursos en el JAR. `clean` evita arrastrar bundles anteriores. Maven no ejecuta pnpm automáticamente: compilar primero la web cuando se necesite incluirla.

## PostgreSQL

El perfil predeterminado es `demo`. Para usar una base externa, crear una base PostgreSQL y pasar las variables al proceso. `.env.example` es una referencia: Spring Boot y los scripts no cargan automáticamente un `.env` de la raíz.

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/butaca'
$env:DB_USERNAME = 'tu_usuario'
$env:DB_PASSWORD = 'tu_password'
.\mvnw.cmd spring-boot:run
```

Bash:

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://localhost:5432/butaca \
DB_USERNAME=tu_usuario DB_PASSWORD=tu_password ./mvnw spring-boot:run
```

`prod` usa las propiedades base de PostgreSQL; no existe un archivo `application-prod.properties`. La configuración actual usa `ddl-auto=update`. `DataLoader` también se ejecuta con este perfil: es una configuración académica, no un despliegue de producción. Los scripts de demo siempre fuerzan `demo`.

## Problemas habituales

| Síntoma | Revisión |
| --- | --- |
| Falta Java o falla Maven | Instalar JDK, revisar `java -version` y `JAVA_HOME` |
| No se encuentra pnpm | Usar Corepack si está disponible o instalar la versión de `package.json` |
| Puerto ocupado | Cerrar la instancia anterior o elegir otro puerto explícitamente |
| La web no aparece en 8081 | Ejecutar el script integral o `build:demo` antes de Maven |
| API inaccesible desde Vite | Revisar backend, `VITE_API_BASE_URL` y `BUTACA_BACKEND_URL` |
| Fallo de dependencias Maven/pnpm | Revisar conexión; la primera ejecución descarga dependencias |
| Bindings Rolldown en WSL | Reinstalar con `pnpm install` dentro de WSL |
| Imágenes o fuentes no cargan | El diseño usa recursos externos; comprobar Internet antes de exponer |

## Preparar un commit

Revisar `git status --short` y `git diff --check`, compilar backend y frontend, ejecutar las pruebas y comprobar el arranque integral. Versionar código, documentación, wrappers Maven y `frontend/pnpm-lock.yaml`. Mantener fuera los secretos, dependencias, builds y respaldos. No borrar `.git/`: contiene el historial del proyecto.
