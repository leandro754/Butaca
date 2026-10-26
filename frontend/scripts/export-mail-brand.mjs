// Development-only renderer; the application reads the exported PNG resources.
// Install tooling with: pnpm --dir target/brand-tools add @resvg/resvg-js
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { Resvg } from "../../target/brand-tools/node_modules/@resvg/resvg-js/index.js";
const destination = new URL("../../src/main/resources/mail/", import.meta.url);
await mkdir(destination, { recursive: true });
for (const [source, output] of [["butaca-logo.svg", "logo-light.png"], ["butaca-logo-on-dark.svg", "logo-dark.png"]]) {
  const svg = await readFile(new URL(`../public/brand/${source}`, import.meta.url));
  const png = new Resvg(svg, { fitTo: { mode: "width", value: 1160 } }).render().asPng();
  await writeFile(new URL(output, destination), png);
}
console.log("Logos de correo exportados desde los SVG originales.");
