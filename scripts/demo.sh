#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_PORT="${SERVER_PORT:-8081}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"
MODE=demo
case "${1:-}" in
  "") ;;
  --dev) MODE=dev ;;
  -h|--help)
    echo "Uso: ./scripts/demo.sh [--dev]"
    echo "Demo: web + API en SERVER_PORT (8081). --dev: Vite en FRONTEND_PORT (5173)."
    exit 0 ;;
  *) echo "Opcion desconocida: $1. Usa --help." >&2; exit 1 ;;
esac
if [ "$#" -gt 1 ]; then echo "Demasiados argumentos. Usa --help." >&2; exit 1; fi

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Falta $1. $2" >&2; exit 1
  fi
}
require_command java "Instala un JDK compatible con Java 17 y configura JAVA_HOME."
require_command node "Instala la version de .nvmrc o una compatible."
require_command curl "Instala curl para comprobar el inicio de los servidores."
require_command pgrep "Instala las herramientas de procesos de tu sistema."
if command -v corepack >/dev/null 2>&1; then
  PNPM=(corepack pnpm)
elif command -v pnpm >/dev/null 2>&1; then
  PNPM=(pnpm)
else
  echo "Falta pnpm. Instala la version indicada en frontend/package.json." >&2; exit 1
fi

check_port() {
  if ! [[ "$1" =~ ^[0-9]+$ ]] || [ "$1" -lt 1 ] || [ "$1" -gt 65535 ]; then
    echo "Puerto invalido: $1" >&2; exit 1
  fi
  if (echo >/dev/tcp/127.0.0.1/"$1") >/dev/null 2>&1; then
    echo "Puerto $1 ocupado. Cambia SERVER_PORT/FRONTEND_PORT o cerra el servidor anterior." >&2
    exit 1
  fi
}
check_port "$BACKEND_PORT"
if [ "$MODE" = dev ]; then
  check_port "$FRONTEND_PORT"
  if [ "$BACKEND_PORT" = "$FRONTEND_PORT" ]; then
    echo "Backend y frontend deben usar puertos distintos." >&2; exit 1
  fi
fi

cd "$ROOT_DIR/frontend"
node scripts/check-env.js
"${PNPM[@]}" install --frozen-lockfile
if [ "$MODE" = demo ]; then "${PNPM[@]}" run build:demo; fi

cd "$ROOT_DIR"
MAVEN_ARGS=(-B)
if [ -n "${MAVEN_REPO_LOCAL:-}" ]; then
  MAVEN_ARGS+=("-Dmaven.repo.local=$MAVEN_REPO_LOCAL")
fi
bash ./mvnw "${MAVEN_ARGS[@]}" clean
LOG_DIR="$ROOT_DIR/target/demo-logs"
mkdir -p "$LOG_DIR"
pids=()

stop_tree() {
  local pid="$1" child
  for child in $(pgrep -P "$pid" || true); do stop_tree "$child"; done
  kill "$pid" >/dev/null 2>&1 || true
}
cleanup() {
  trap - EXIT INT TERM
  echo; echo "Cerrando Butaca..."
  for pid in "${pids[@]}"; do stop_tree "$pid"; done
  for pid in "${pids[@]}"; do wait "$pid" 2>/dev/null || true; done
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

wait_endpoint() {
  local url="$1" pid="$2" name="$3" attempt
  for ((attempt=0; attempt<120; attempt++)); do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "$name termino. Revisa $LOG_DIR." >&2; return 1
    fi
    if curl --fail --silent --max-time 2 "$url" >/dev/null; then return 0; fi
    sleep 1
  done
  echo "$name no respondio. Revisa $LOG_DIR." >&2; return 1
}

echo "Iniciando backend... Logs: $LOG_DIR/backend.log"
if [ "$MODE" = dev ]; then PUBLIC_PORT="$FRONTEND_PORT"; else PUBLIC_PORT="$BACKEND_PORT"; fi
export BUTACA_PUBLIC_URL="${BUTACA_PUBLIC_URL:-http://localhost:$PUBLIC_PORT}"
bash ./mvnw "${MAVEN_ARGS[@]}" spring-boot:run -Dspring-boot.run.profiles=demo \
  "-Dspring-boot.run.arguments=--server.port=$BACKEND_PORT" >"$LOG_DIR/backend.log" 2>&1 &
pids+=("$!")
wait_endpoint "http://127.0.0.1:$BACKEND_PORT/api/eventos" "${pids[0]}" Backend
WEB_URL="http://localhost:$BACKEND_PORT"
if [ "$MODE" = dev ]; then
  (
    cd "$ROOT_DIR/frontend"
    export VITE_API_BASE_URL=/api
    export BUTACA_BACKEND_URL="http://127.0.0.1:$BACKEND_PORT"
    exec "${PNPM[@]}" run dev --host 127.0.0.1 --port "$FRONTEND_PORT" --strictPort
  ) >"$LOG_DIR/frontend.log" 2>&1 &
  pids+=("$!")
  WEB_URL="http://localhost:$FRONTEND_PORT"
  wait_endpoint "http://127.0.0.1:$FRONTEND_PORT" "${pids[1]}" Frontend
else
  wait_endpoint "http://127.0.0.1:$BACKEND_PORT" "${pids[0]}" Web
fi

echo "Butaca listo: $WEB_URL"
echo "API: http://localhost:$BACKEND_PORT/api | H2: http://localhost:$BACKEND_PORT/h2-console"
echo "H2: jdbc:h2:mem:butaca_demo | usuario sa | sin password"
echo "Cuentas: admin@butaca.com / admin123; org@butaca.com / org123; cliente@butaca.com / cliente123"
echo "Logs: $LOG_DIR. Ctrl+C detiene los servidores."
# Compatible con Bash 3.2 de macOS: no requiere wait -n.
while true; do
  for pid in "${pids[@]}"; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "Un servidor termino inesperadamente. Revisa $LOG_DIR." >&2; exit 1
    fi
  done
  sleep 1
done
