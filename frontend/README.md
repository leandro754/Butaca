# Frontend de Butaca

Interfaz React + Vite conectada a la API Spring Boot. Los comandos de arranque integral están en el [README del proyecto](../README.md).

## Ejecución independiente

Con el backend en `http://localhost:8081`, ejecutar desde esta carpeta:

```bash
cp .env.example .env
pnpm install --frozen-lockfile
pnpm run dev
```

Se requiere Node compatible con [`.nvmrc`](../.nvmrc) y pnpm 9 o superior. Si pnpm no está en el PATH y está disponible Corepack, usar `corepack pnpm` en los comandos.

`VITE_API_BASE_URL` permite elegir la API; el fallback es `http://localhost:8080/api`. Se puede copiar `.env.example` a `.env` para configurarlo localmente. Vite necesita reiniciarse después de cambiar esa variable.

Los scripts integrales usan `VITE_API_BASE_URL=/api` y el proxy de Vite. `BUTACA_BACKEND_URL` configura el destino del proxy; por defecto es `http://localhost:8081`.

## Comandos

| Comando | Resultado |
| --- | --- |
| `pnpm run dev` | Servidor de desarrollo en el puerto 5173 |
| `pnpm run build` | Compilación en `dist/` usando la configuración local de API |
| `pnpm run build:demo` | Compilación en `dist/` con API relativa `/api`, para Spring Boot |
| `pnpm run preview` | Vista previa local del build; requiere configurar la API o un proxy aparte si el build usa `/api` |

La salida `dist/` está ignorada por Git. Maven la incorpora como recursos estáticos al compilar/empaquetar el backend. No se versionan bundles ni `node_modules/`.

## Organización

- `src/pages/`: catálogo, autenticación, compra y paneles por rol.
- `src/components/`: componentes compartidos.
- `src/services/`: acceso a la API y normalización de respuestas.
- `src/utils/`: autenticación local, selección de compra, formatos y QR.
- `src/styles/`: estilos de la aplicación.

La clave de autenticación local es `butaca-auth-user`. Las cuentas y compras se resuelven contra endpoints reales; los adaptadores KYC y de pago del backend son simulados.

Ante errores de bindings nativos de Rolldown en WSL, ejecutar `pnpm install` desde WSL dentro de esta carpeta y volver a compilar. No mezclar instalaciones de dependencias entre Windows y WSL.
