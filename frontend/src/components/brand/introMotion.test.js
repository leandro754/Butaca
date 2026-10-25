import test from "node:test";
import assert from "node:assert/strict";
import { introTiming, logoFlightFrames } from "./introMotion.js";
import { brandGeometry } from "./brandGeometry.js";

test("la marca lleva cinco pochoclos y solo utaca después de la B", () => {
  assert.equal(brandGeometry.kernels.length, 5);
  assert.equal(
    brandGeometry.letters.map((letter) => letter.character).join(""),
    "utaca",
  );
});

test("la página aparece antes del lema, último acto, dentro del límite de escritorio", () => {
  const timing = introTiming();
  assert.equal(timing.duration, 3120);
  assert.ok(timing.bucketStart < timing.popcornStart);
  assert.ok(timing.popcornStart < timing.lettersStart);
  assert.ok(timing.pageStart < timing.taglineStart);
  assert.ok(timing.pageStart + timing.pageDuration < timing.duration);
  assert.equal(timing.taglineStart + timing.taglineDuration, timing.duration);
});

test("el móvil termina la firma en menos de 2.4 segundos", () => {
  const timing = introTiming(true);
  assert.ok(timing.duration >= 2000 && timing.duration <= 2400);
  assert.ok(timing.taglineStart > timing.pageStart);
});

test("FLIP centra primero la B y termina exactamente en el header reservado", () => {
  const rect = { left: 70, top: 15, width: 230, height: 90 };
  const mark = { left: 74, top: 40, width: 47, height: 44 };
  const viewport = { width: 1440, height: 900 };
  const frames = logoFlightFrames(rect, mark, viewport);
  const match = frames[0].transform.match(
    /translate\(([-\d.]+)px, ([-\d.]+)px\) scale\(([-\d.]+)\)/,
  );
  const [, x, y, scale] = match.map(Number);
  assert.ok(
    Math.abs(
      rect.left +
        x +
        (mark.left - rect.left + mark.width / 2) * scale -
        viewport.width / 2,
    ) < 0.001,
  );
  assert.ok(
    Math.abs(
      rect.top +
        y +
        (mark.top - rect.top + mark.height / 2) * scale -
        viewport.height / 2,
    ) < 0.001,
  );
  assert.equal(frames.at(-1).transform, "translate(0px, 0px) scale(1)");
});

test("la marca centrada cabe en un móvil estrecho", () => {
  const frames = logoFlightFrames(
    { left: 18, top: 9, width: 153, height: 62 },
    { left: 20, top: 26, width: 32, height: 29 },
    { width: 320, height: 568 },
  );
  const scale = Number(frames[2].transform.match(/scale\(([-\d.]+)\)/)[1]);
  assert.ok(153 * scale <= 272.001);
  assert.ok(
    frames.every(
      (frame, index) => index === 0 || frame.offset >= frames[index - 1].offset,
    ),
  );
});
import { introPalette } from "./introMotion.js";

test("la intro oscura mantiene letras marfil visibles sobre carbón", () => {
  const palette = introPalette("dark");
  assert.equal(palette.backdrop, "#111115");
  assert.equal(palette.letters, "#FFF9F0");
  assert.notEqual(palette.backdrop, palette.letters);
});

test("la intro clara conserva fondo blanco y letras oscuras", () => {
  assert.equal(introPalette("light").backdrop, "#FFFFFF");
  assert.equal(introPalette("light").letters, "#17191B");
});
