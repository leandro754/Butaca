import test from "node:test";
import assert from "node:assert/strict";
import { generateQRMatrix } from "./qrCode.js";
test("el UUID completo se codifica en un QR distinto para cada entrada", () => {
  const first = generateQRMatrix("0b301781-145d-4b3e-a083-93326023ac12");
  const second = generateQRMatrix("0b301781-145d-4b3e-a083-93326023ac13");
  assert.equal(first.length, 29);
  assert.notDeepEqual(first, second);
  assert.ok(first.every((row) => row.every((bit) => typeof bit === "boolean")));
});
test("rechaza contenidos largos en vez de truncar el código silenciosamente", () => {
  assert.throws(() => generateQRMatrix("x".repeat(107)), RangeError);
});
