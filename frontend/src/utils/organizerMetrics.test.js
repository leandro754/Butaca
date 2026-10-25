import test from "node:test";
import assert from "node:assert/strict";
import { organizerTotals } from "./organizerMetrics.js";

test("organizer counts active future events, drafts and backend net sales separately", () => {
  assert.deepEqual(
    organizerTotals(
      [
        {
          estado: "PUBLICADO",
          fecha: "2026-11-01",
          hora: "20:00",
          entradasVendidas: 3,
          totalVendido: 4500,
        },
        {
          estado: "PUBLICADO",
          fecha: "2026-01-01",
          hora: "20:00",
          entradasVendidas: 2,
          totalVendido: 2000,
        },
        { estado: "BORRADOR", entradasVendidas: 0, totalVendido: 0 },
        { estado: "CANCELADO", entradasVendidas: 0, totalVendido: 0 },
      ],
      new Date(2026, 9, 25),
    ),
    { active: 1, drafts: 1, sold: 5, revenue: 6500 },
  );
  assert.deepEqual(organizerTotals([]), {
    active: 0,
    drafts: 0,
    sold: 0,
    revenue: 0,
  });
});
