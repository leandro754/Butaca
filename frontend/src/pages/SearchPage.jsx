import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import EventGrid from "../components/events/EventGrid";
import { getAllEvents, getCategories } from "../services/eventService";
import {
  isInDateRange,
  normalizeText,
  sortByDate,
} from "../utils/collectionFilters";

function SearchPage() {
  const [params, setParams] = useSearchParams();
  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const search = params.get("q") || "";
  const category = params.get("categoria") || "";
  const from = params.get("desde") || "";
  const to = params.get("hasta") || "";
  const price = params.get("precio") || "";
  const sort = params.get("orden") || "asc";
  function update(key, value) {
    const next = new URLSearchParams(params);
    value ? next.set(key, value) : next.delete(key);
    setParams(next, { replace: true });
  }
  useEffect(() => {
    let active = true;
    Promise.all([getAllEvents(), getCategories()])
      .then(([events, categories]) => {
        if (active) {
          setEvents(events);
          setCategories(categories);
        }
      })
      .catch((err) => {
        if (active) setError(err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);
  const filtered = useMemo(() => {
    const matches = events.filter(
      (event) =>
        normalizeText(
          `${event.nombre} ${event.recinto} ${event.ciudad}`,
        ).includes(normalizeText(search)) &&
        (!category || event.categoria === category) &&
        (!price || event.precioMinimo <= Number(price)) &&
        isInDateRange(event.fecha, from, to),
    );
    return sort === "precio"
      ? [...matches].sort((a, b) => a.precioMinimo - b.precioMinimo)
      : sortByDate(matches, (event) => `${event.fecha}T${event.hora}`, sort);
  }, [events, search, category, price, from, to, sort]);
  return (
    <section className="container page-block search-page">
      <div className="catalog-heading">
        <div>
          <p className="section-eyebrow">Argentina / Eventos</p>
          <h1>Buscá tu próxima salida.</h1>
        </div>
      </div>
      <div className="search-layout">
        <aside className="search-panel">
          <div className="panel-heading">
            <h2>Encontrar eventos</h2>
            <button
              className="text-link"
              type="button"
              onClick={() => setParams({})}
            >
              Limpiar
            </button>
          </div>
          <label>
            Evento, artista o lugar
            <input
              type="search"
              placeholder="¿Qué querés ver?"
              value={search}
              onChange={(e) => update("q", e.target.value)}
            />
          </label>
          <label>
            Categoría
            <select
              value={category}
              onChange={(e) => update("categoria", e.target.value)}
            >
              <option value="">Todas las categorías</option>
              {categories.map((item) => (
                <option key={item.id} value={item.nombre}>
                  {item.nombre}
                </option>
              ))}
            </select>
          </label>
          <div className="date-filter-pair">
            <label>
              Desde
              <input
                type="date"
                value={from}
                max={to || undefined}
                onChange={(e) => update("desde", e.target.value)}
              />
            </label>
            <label>
              Hasta
              <input
                type="date"
                value={to}
                min={from || undefined}
                onChange={(e) => update("hasta", e.target.value)}
              />
            </label>
          </div>
          <label>
            Precio máximo (ARS)
            <input
              type="number"
              min="0"
              step="500"
              value={price}
              placeholder="Sin límite"
              onChange={(e) => update("precio", e.target.value)}
            />
          </label>
          <p className="quiet-note">
            Solo eventos publicados con entradas disponibles.
          </p>
        </aside>
        <div className="search-results">
          <div className="results-toolbar">
            <p className="results-count" aria-live="polite">
              {loading
                ? "Cargando cartelera…"
                : `${filtered.length} eventos para vos`}
            </p>
            <label className="inline-sort">
              Ordenar
              <select
                value={sort}
                onChange={(e) => update("orden", e.target.value)}
              >
                <option value="asc">Fecha más cercana</option>
                <option value="desc">Fecha más lejana</option>
                <option value="precio">Menor precio</option>
              </select>
            </label>
          </div>
          {error ? (
            <div className="auth-error" role="alert">
              {error}
            </div>
          ) : null}
          {loading ? (
            <div className="event-grid">
              {[1, 2, 3].map((id) => (
                <div key={id} className="skeleton skeleton-card" />
              ))}
            </div>
          ) : (
            <EventGrid events={filtered} />
          )}
        </div>
      </div>
    </section>
  );
}
export default SearchPage;
