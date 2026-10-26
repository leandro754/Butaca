import test from "node:test";
import assert from "node:assert/strict";
import { feedDateRange, recommendEvents } from "./homeFeed.js";
import { nextSlide } from "../hooks/useCarousel.js";

test("recommendations favor different categories, then fill without duplicates", () => {
  const events = [
    { id: 1, categoria: "Cine", fecha: "2026-11-02", hora: "20:00" },
    { id: 2, categoria: "Cine", fecha: "2026-11-01", hora: "20:00" },
    { id: 3, categoria: "Teatro", fecha: "2026-11-03", hora: "20:00" },
    { id: 4, categoria: "Deportes", fecha: "2026-11-04", hora: "20:00" },
  ];
  assert.deepEqual(
    recommendEvents(events, 3).map((event) => event.id),
    [2, 3, 4],
  );
  assert.deepEqual(
    recommendEvents(events).map((event) => event.id),
    [2, 3, 4, 1],
  );
  assert.equal(events[0].id, 1);
  assert.deepEqual(recommendEvents([]), []);
});

test("date shortcuts use local days and handle month/year boundaries", () => {
  assert.deepEqual(feedDateRange("week", new Date(2026, 11, 29)), {
    from: "2026-12-29",
    to: "2027-01-04",
  });
  assert.deepEqual(feedDateRange("month", new Date(2028, 1, 20)), {
    from: "2028-02-20",
    to: "2028-02-29",
  });
  assert.deepEqual(feedDateRange("all"), { from: "", to: "" });
});

test("weekend includes Friday through Sunday and keeps the remaining current weekend", () => {
  for (const day of [23, 24, 25]) {
    assert.deepEqual(feedDateRange("weekend", new Date(2026, 9, day)), {
      from: `2026-10-${day}`,
      to: "2026-10-25",
    });
  }
  assert.deepEqual(feedDateRange("weekend", new Date(2026, 9, 26)), {
    from: "2026-10-30",
    to: "2026-11-01",
  });
});

test("carousel wraps in either direction, including empty and single slide", () => {
  assert.equal(nextSlide(3, 4), 0);
  assert.equal(nextSlide(0, 4, -1), 3);
  assert.equal(nextSlide(0, 1), 0);
  assert.equal(nextSlide(0, 0), 0);
});
