import { mkdir, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { brandGeometry as geometry } from "../src/components/brand/brandGeometry.js";

const directory = fileURLToPath(new URL("../public/brand/", import.meta.url));
await mkdir(directory, { recursive: true });
const paths = (items) => items.map((path) => `<path d="${path}"/>`).join("");
function svg({ markOnly = false, tagline = true, dark = false, mono = false }) {
  const ink = dark ? "#FFF9F0" : "#17191B";
  const mark = `<g fill="${mono ? ink : "#E6003E"}">${paths(geometry.bucketPaths)}</g><g fill="${mono ? ink : "#FFC36B"}">${geometry.kernels.map((kernel) => `<path transform="translate(${kernel.x} ${kernel.y}) rotate(${kernel.rotation}) scale(${kernel.scale})" d="${geometry.kernelPath}"/>`).join("")}</g>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${markOnly ? "-12 -8 162 200" : `0 0 580 ${tagline ? 225 : 190}`}" role="img" aria-label="Butaca. Más eventos para vos"><title>Butaca</title>${mark}${markOnly ? "" : `<g fill="${ink}">${paths(geometry.letters.map((letter) => letter.path))}</g>${tagline ? `<g fill="${mono ? ink : dark ? "#F7EBDD" : "#62666A"}">${paths(geometry.taglinePaths)}</g>` : ""}`}</svg>\n`;
}
for (const [name, options] of Object.entries({
  "butaca-logo.svg": {},
  "butaca-logo-on-dark.svg": { dark: true },
  "butaca-logo-no-tagline.svg": { tagline: false },
  "butaca-mark.svg": { markOnly: true },
  "butaca-one-ink.svg": { mono: true },
})) {
  await writeFile(`${directory}/${name}`, svg(options));
}
console.log(
  "SVG de Butaca exportados: horizontal, oscuro, sin lema, isotipo y una tinta.",
);
