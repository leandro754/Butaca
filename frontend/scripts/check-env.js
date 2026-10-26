const [major, minor] = process.versions.node.split(".").map(Number);

const isSupported =
  (major === 20 && minor >= 19) ||
  (major === 22 && minor >= 12) ||
  major > 22;

if (!isSupported) {
  console.error("Este frontend requiere Node.js 20.19+, 22.12+ o superior.");
  console.error(`Version detectada: ${process.version}`);
  console.error("Actualiza Node antes de ejecutar pnpm install, pnpm run dev o pnpm run build.");
  process.exit(1);
}

console.log(`Node.js ${process.version} detectado. Entorno compatible.`);
