import "./check-env.js";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

// La demo sirve la web y la API desde el mismo origen, incluso en otro puerto.
const vite = fileURLToPath(new URL("../node_modules/vite/bin/vite.js", import.meta.url));
const result = spawnSync(process.execPath, [vite, "build"], {
  stdio: "inherit",
  env: { ...process.env, VITE_API_BASE_URL: "/api" }
});

if (result.error) console.error(result.error.message);
process.exit(result.status ?? 1);
