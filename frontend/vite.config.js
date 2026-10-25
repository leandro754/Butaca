import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "VITE_");
  return {
    // Instancias de demo en otros puertos no deben reescribir la caché de la app principal.
    cacheDir: `node_modules/.vite-butaca-${process.env.FRONTEND_PORT || "5173"}`,
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        "/api": {
          target:
            process.env.VITE_API_PROXY_TARGET ||
            env.VITE_API_PROXY_TARGET ||
            "http://localhost:8080",
          changeOrigin: true,
        },
      },
    },
  };
});
