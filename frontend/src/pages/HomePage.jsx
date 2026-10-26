import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import HeroSection from "../components/home/HeroSection";
import CategoryTabs from "../components/home/CategoryTabs";
import EventGrid from "../components/events/EventGrid";
import { getAllEvents, getCategories } from "../services/eventService";
import { isInDateRange, sortByDate } from "../utils/collectionFilters";
import { feedDateRange, recommendEvents } from "../utils/homeFeed";
import ButacaLogo from "../components/brand/ButacaLogo";

function HomePage() {
  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState(["Todos"]);
  const [category, setCategory] = useState("Todos");
  const [direction, setDirection] = useState("asc");
  const [period, setPeriod] = useState("all");
  const [shown, setShown] = useState(9);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    Promise.all([getAllEvents(), getCategories()])
      .then(([events, categories]) => {
        if (active) {
          setEvents(events);
          setCategories(["Todos", ...categories.map((item) => item.nombre)]);
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
  }, [attempt]);
  const upcoming = useMemo(() => recommendEvents(events), [events]);
  const visible = useMemo(() => {
    const { from, to } = feedDateRange(period);
    return sortByDate(
      events.filter(
        (event) =>
          (category === "Todos" || event.categoria === category) &&
          isInDateRange(event.fecha, from, to),
      ),
      (event) => `${event.fecha}T${event.hora}`,
      direction,
    );
  }, [events, category, direction, period]);
  function changeCategory(value) {
    setCategory(value);
    setShown(9);
  }
  return (
    <>
      <HeroSection events={upcoming} loading={loading} />
      <section className="container home-section" id="colecciones">
        <p className="section-eyebrow home-catalog-eyebrow">
          Tu próxima salida
        </p>
        <div className="catalog-heading">
          <div>
            <h2 className="cinema-section-title">La cartelera.</h2>
          </div>
          <Link to="/buscar" className="text-link">
            Ver todos los eventos ↗
          </Link>
        </div>
        <div className="catalog-toolbar">
          <CategoryTabs
            categories={categories}
            activeCategory={category}
            onChange={changeCategory}
          />
          <label className="inline-sort">
            Ordenar
            <select
              value={direction}
              onChange={(e) => {
                setDirection(e.target.value);
                setShown(9);
              }}
            >
              <option value="asc">Fecha más cercana</option>
              <option value="desc">Fecha más lejana</option>
            </select>
          </label>
        </div>
        <div className="feed-toolbar">
          <div
            className="feed-periods"
            role="group"
            aria-label="Filtrar por fecha"
          >
            {[
              ["all", "Todas las fechas"],
              ["week", "Próximos 7 días"],
              ["weekend", "Este finde"],
              ["month", "Este mes"],
            ].map(([value, label]) => (
              <button
                type="button"
                key={value}
                aria-pressed={period === value}
                onClick={() => {
                  setPeriod(value);
                  setShown(9);
                }}
              >
                {label}
              </button>
            ))}
          </div>
          <p className="feed-count" role="status">
            {!loading && !error
              ? `${visible.length} ${visible.length === 1 ? "evento para elegir" : "eventos para elegir"}`
              : ""}
          </p>
        </div>
        {error ? (
          <div className="auth-error" role="alert">
            <p>No se pudo cargar la cartelera. {error}</p>
            <button
              type="button"
              className="btn-copy"
              onClick={() => setAttempt((current) => current + 1)}
            >
              Reintentar
            </button>
          </div>
        ) : null}
        {loading ? (
          <div className="event-grid">
            {[1, 2, 3].map((id) => (
              <div className="skeleton skeleton-card" key={id} />
            ))}
          </div>
        ) : (
          <div
            key={`${category}-${period}-${direction}`}
            className="feed-results"
          >
            {visible.length ? (
              <EventGrid events={visible.slice(0, shown)} />
            ) : !error ? (
              <div className="empty-state feed-empty">
                <h3>Probemos con otra fecha.</h3>
                <p>No hay eventos para esta combinación de filtros.</p>
                <button
                  type="button"
                  className="button button-secondary"
                  onClick={() => {
                    changeCategory("Todos");
                    setPeriod("all");
                  }}
                >
                  Ver toda la cartelera
                </button>
              </div>
            ) : null}
            {visible.length > shown ? (
              <div className="feed-more">
                <p>
                  Estás viendo {Math.min(shown, visible.length)} de{" "}
                  {visible.length} eventos
                </p>
                <button
                  type="button"
                  className="button button-secondary"
                  onClick={() => setShown((value) => value + 6)}
                >
                  Más planes para vos <span aria-hidden="true">↓</span>
                </button>
              </div>
            ) : null}
          </div>
        )}
      </section>
      <section className="container ticket-note">
        <span className="ticket-note-logo">
          <ButacaLogo theme="onLight" showTagline={false} decorative />
        </span>
        <div>
          <h2>Tu entrada, a mano.</h2>
          <p>
            Descargala desde tu cuenta y presentá el QR junto con tu DNI el día
            del evento.
          </p>
        </div>
        <Link to="/dashboard" className="text-link">
          Ir a mis entradas ↗
        </Link>
      </section>
    </>
  );
}
export default HomePage;
