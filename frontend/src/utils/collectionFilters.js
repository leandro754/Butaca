export function normalizeText(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

// API dates are local dates/times. Comparing the date part includes the whole end day.
export function isInDateRange(value, from = "", to = "") {
  if (!from && !to) return true;
  if (!value) return false;
  const day = String(value).slice(0, 10);
  return (!from || day >= from) && (!to || day <= to);
}

export function sortByDate(items, getDate, direction = "asc") {
  return [...items].sort((a, b) => {
    const left = getDate(a);
    const right = getDate(b);
    if (!left && !right) return 0;
    if (!left) return 1;
    if (!right) return -1;
    const comparison = String(left).localeCompare(String(right));
    return direction === "desc" ? -comparison : comparison;
  });
}

export function approvedRevenue(orders) {
  return orders
    .filter(
      (order) =>
        order.estado_compra === "CONFIRMADA" &&
        order.pago?.estado_pago === "APROBADO",
    )
    .reduce((sum, order) => sum + Number(order.total || 0), 0);
}
