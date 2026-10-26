import { useEffect, useId, useRef } from "react";
import { Link } from "react-router-dom";
import { dateParts, formatDate, formatPrice } from "../../utils/formatters";
import { useCarousel, CAROUSEL_INTERVAL } from "../../hooks/useCarousel";

function HeroSection({ events = [], loading }) {
  const panelId = useId();
  const showcase = events.slice(0, 4);
  const carousel = useCarousel(showcase.length);
  const { activeIndex, step, playing } = carousel;
  const touchStart = useRef(null);
  const selectorRef = useRef(null);
  const current = showcase[activeIndex];
  const nextImage = showcase[(activeIndex + 1) % showcase.length]?.imagen;
  useEffect(() => {
    if (nextImage && showcase.length > 1) {
      const image = new Image();
      image.src = nextImage;
    }
  }, [nextImage, showcase.length]);
  useEffect(() => {
    const selector = selectorRef.current;
    const button = selector?.children[activeIndex];
    if (button && selector.scrollWidth > selector.clientWidth) {
      selector.scrollTo({
        left: button.offsetLeft - selector.children[0].offsetLeft,
        behavior: carousel.reducedMotion ? "instant" : "smooth",
      });
    }
  }, [activeIndex, carousel.reducedMotion]);
  if (!current)
    return (
      <section className="billboard-section billboard-section--empty">
        <div className="container billboard-empty">
          <p className="section-eyebrow">En vivo / Argentina</p>
          <h1>
            {loading
              ? "Se prepara la función."
              : "La próxima salida está por llegar."}
          </h1>
          <p>
            {loading
              ? "Estamos cargando la cartelera…"
              : "Por ahora no hay eventos con entradas disponibles."}
          </p>
          <Link to="/buscar" className="button button-primary">
            Explorar cartelera ↗
          </Link>
        </div>
      </section>
    );
  const parts = dateParts(current.fecha);
  return (
    <section
      className="billboard-section"
      aria-label="Recomendados en Butaca"
      aria-roledescription="carrusel"
      {...carousel.interactionProps}
    >
      <div
        className="billboard"
        id={panelId}
        onTouchStart={(event) => {
          const touch = event.touches[0];
          touchStart.current = { x: touch.clientX, y: touch.clientY };
        }}
        onTouchEnd={(event) => {
          if (!touchStart.current || showcase.length < 2) return;
          const touch = event.changedTouches[0];
          const dx = touch.clientX - touchStart.current.x;
          const dy = touch.clientY - touchStart.current.y;
          if (Math.abs(dx) > 55 && Math.abs(dx) > Math.abs(dy) * 1.5)
            step(dx > 0 ? -1 : 1);
          touchStart.current = null;
        }}
        onTouchCancel={() => {
          touchStart.current = null;
        }}
      >
        <img
          key={current.imagen}
          className="billboard-image"
          src={current.imagen}
          alt=""
          fetchPriority="high"
        />
        <div className="stage-curtain stage-curtain--left" aria-hidden="true" />
        <div
          className="stage-curtain stage-curtain--right"
          aria-hidden="true"
        />
        <div className="container billboard-inner">
          <div
            className="billboard-content"
            key={current.id}
            aria-live={playing ? "off" : "polite"}
            aria-atomic="true"
          >
            <p className="billboard-category">
              <span className="hero-category-name">{current.categoria}</span>
              <svg
                className="mini-ticket"
                viewBox="0 0 28 20"
                fill="none"
                aria-hidden="true"
              >
                <path
                  d="M2 3h24v4a3 3 0 0 0 0 6v4H2v-4a3 3 0 0 0 0-6V3Z"
                  stroke="currentColor"
                  strokeWidth="2"
                />
                <path
                  d="M9 6v8M19 6v8"
                  stroke="currentColor"
                  strokeDasharray="2 2"
                />
              </svg>
              <span className="hero-category-divider" aria-hidden="true">
                —
              </span>
              Recomendados
            </p>
            <h1>{current.nombre}</h1>
            <p className="billboard-venue">{current.recinto}</p>
            <p className="billboard-date">
              {formatDate(current.fecha)} · {current.hora} h
            </p>
            <div className="billboard-actions">
              <Link
                to={`/eventos/${current.id}`}
                className="button button-primary"
              >
                Ver entradas <span aria-hidden="true">↗</span>
              </Link>
              <span>Desde {formatPrice(current.precioMinimo)}</span>
            </div>
          </div>
          <div className="billboard-date-stamp" aria-hidden="true">
            <span>{parts.month}</span>
            <strong>{parts.day}</strong>
            <small>{current.hora} h</small>
          </div>
          <div className="billboard-controls">
            <span
              className="billboard-number"
              aria-label={`Evento ${activeIndex + 1} de ${showcase.length}`}
            >
              {String(activeIndex + 1).padStart(2, "0")} /{" "}
              {String(showcase.length).padStart(2, "0")}
            </span>
            {showcase.length > 1 ? (
              <>
                <button
                  type="button"
                  onClick={() => step(-1)}
                  aria-label="Evento anterior"
                  aria-controls={panelId}
                >
                  <svg
                    viewBox="0 0 24 24"
                    width="20"
                    height="20"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.6"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <path d="m14 6-6 6 6 6" />
                  </svg>
                </button>
                <button
                  type="button"
                  onClick={() => step(1)}
                  aria-label="Evento siguiente"
                  aria-controls={panelId}
                >
                  <svg
                    viewBox="0 0 24 24"
                    width="20"
                    height="20"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.6"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <path d="m10 6 6 6-6 6" />
                  </svg>
                </button>
              </>
            ) : null}
          </div>
        </div>
      </div>
      {showcase.length > 1 ? (
        <div className="featured-strip">
          <div
            className="container billboard-selector"
            ref={selectorRef}
            aria-label="Elegir evento destacado"
          >
            {showcase.map((event, index) => (
              <button
                type="button"
                key={event.id}
                aria-pressed={index === activeIndex}
                aria-controls={panelId}
                onClick={() => carousel.select(index)}
              >
                <span>{String(index + 1).padStart(2, "0")}</span>
                <strong>{event.nombre}</strong>
                <small>
                  {dateParts(event.fecha).day} {dateParts(event.fecha).month}
                </small>
                {index === activeIndex && !carousel.reducedMotion ? (
                  <i
                    key={current.id}
                    className="carousel-progress"
                    aria-hidden="true"
                    style={{
                      animationDuration: `${CAROUSEL_INTERVAL}ms`,
                      animationPlayState: playing ? "running" : "paused",
                    }}
                  />
                ) : null}
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </section>
  );
}
export default HeroSection;
