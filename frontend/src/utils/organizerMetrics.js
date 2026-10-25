export function organizerTotals(events, now = new Date()) {
  return events.reduce(
    (total, event) => ({
      active:
        total.active +
        (event.estado === "PUBLICADO" &&
        new Date(`${event.fecha}T${event.hora || "23:59"}`) >= now
          ? 1
          : 0),
      drafts: total.drafts + (event.estado === "BORRADOR" ? 1 : 0),
      sold: total.sold + Number(event.entradasVendidas || 0),
      revenue: total.revenue + Number(event.totalVendido || 0),
    }),
    { active: 0, drafts: 0, sold: 0, revenue: 0 },
  );
}
