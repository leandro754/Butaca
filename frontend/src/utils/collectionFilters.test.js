import test from "node:test";
import assert from "node:assert/strict";
import {
  approvedRevenue,
  isInDateRange,
  normalizeText,
  sortByDate,
} from "./collectionFilters.js";

test("el rango incluye ambas fechas completas y admite un solo límite", () => {
  assert.equal(
    isInDateRange("2026-10-24T23:59:59.999", "2026-10-24", "2026-10-24"),
    true,
  );
  assert.equal(
    isInDateRange("2026-10-25T00:00:00", "2026-10-24", "2026-10-24"),
    false,
  );
  assert.equal(isInDateRange("2026-10-24T00:00:00", "2026-10-24", ""), true);
  assert.equal(isInDateRange("2026-10-23T23:59:59", "2026-10-24", ""), false);
  assert.equal(isInDateRange(undefined, "2026-10-24", ""), false);
  assert.equal(isInDateRange(undefined), true);
});

test("ordena por fecha y hora sin mutar la lista; los registros sin fecha quedan al final", () => {
  const rows = [
    { id: 1, date: "2026-11-01T20:30" },
    { id: 2 },
    { id: 3, date: "2026-11-01T18:00" },
    { id: 4, date: "2026-12-01T12:00" },
  ];
  assert.deepEqual(
    sortByDate(rows, (item) => item.date).map((item) => item.id),
    [3, 1, 4, 2],
  );
  assert.deepEqual(
    sortByDate(rows, (item) => item.date, "desc").map((item) => item.id),
    [4, 1, 3, 2],
  );
  assert.deepEqual(
    rows.map((item) => item.id),
    [1, 2, 3, 4],
  );
});

test("la búsqueda coincide con tildes y mayúsculas", () => {
  assert.equal(normalizeText("  BÁSQUET  "), "basquet");
});

test("la recaudación excluye pagos reembolsados, rechazados y compras pendientes", () => {
  assert.equal(
    approvedRevenue([
      {
        estado_compra: "CONFIRMADA",
        pago: { estado_pago: "APROBADO" },
        total: "12000",
      },
      {
        estado_compra: "CONFIRMADA",
        pago: { estado_pago: "REEMBOLSADO" },
        total: 8000,
      },
      {
        estado_compra: "PENDIENTE",
        pago: { estado_pago: "APROBADO" },
        total: 5000,
      },
      {
        estado_compra: "CONFIRMADA",
        pago: { estado_pago: "RECHAZADO" },
        total: 7000,
      },
    ]),
    12000,
  );
});
