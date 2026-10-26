import { sortByDate } from "./collectionFilters.js";

function localDay(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function feedDateRange(period, now = new Date()) {
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const end = new Date(start);
  if (period === "week") end.setDate(end.getDate() + 6);
  else if (period === "weekend") {
    // Friday through Sunday, including the remaining days of the current weekend.
    start.setDate(start.getDate() + ((5 - start.getDay() + 7) % 7));
    if (now.getDay() === 6 || now.getDay() === 0) start.setTime(end.getTime());
    end.setTime(start.getTime());
    end.setDate(end.getDate() + ((7 - end.getDay()) % 7));
  } else if (period === "month") end.setMonth(end.getMonth() + 1, 0);
  else return { from: "", to: "" };
  return { from: localDay(start), to: localDay(end) };
}

export function recommendEvents(events, limit = 4) {
  const upcoming = sortByDate(
    events,
    (event) => `${event.fecha}T${event.hora}`,
  );
  const selected = [];
  const categories = new Set();
  for (const event of upcoming) {
    if (categories.has(event.categoria)) continue;
    selected.push(event);
    categories.add(event.categoria);
    if (selected.length === limit) return selected;
  }
  for (const event of upcoming) {
    if (selected.some((item) => item.id === event.id)) continue;
    selected.push(event);
    if (selected.length === limit) break;
  }
  return selected.slice(0, limit);
}
