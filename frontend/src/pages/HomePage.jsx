import { useEffect, useMemo, useState } from "react";
import HeroSection from "../components/home/HeroSection";
import CategoryTabs from "../components/home/CategoryTabs";
import EventGrid from "../components/events/EventGrid";
import SectionTitle from "../components/ui/SectionTitle";
import { categoryOptions as fallbackCategoryOptions } from "../data/categories";
import { getAllEvents, getCategories } from "../services/eventService";

function HomePage({ currentUser }) {
  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState(fallbackCategoryOptions);
  const [activeCategory, setActiveCategory] = useState("Todos");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      getAllEvents().catch(() => []),
      getCategories().catch(() => [])
    ])
      .then(([eventsData, categoriesData]) => {
        setEvents(eventsData || []);
        if (categoriesData && categoriesData.length > 0) {
          setCategories(["Todos", ...categoriesData.map((category) => category.nombre)]);
        }
      })
      .finally(() => setLoading(false));
  }, []);

  const featuredEvent = events.length > 0 ? (events.find((event) => event.destacados) || events[0]) : null;
  const popularEvents = events.filter((event) => event.popular).slice(0, 3);

  const visibleEvents = useMemo(() => {
    if (activeCategory === "Todos") {
      return events;
    }

    return events.filter((event) => event.categoria === activeCategory);
  }, [activeCategory, events]);

  return (
    <>
      <HeroSection featuredEvent={featuredEvent} events={events} />

      <section className="container home-section" id="colecciones">
        <SectionTitle
          eyebrow="Explorá la cartelera"
          title="Próximos espectáculos"
          text="Elegí una categoría y asegurá tus entradas digitales al instante."
        />
        <CategoryTabs
          categories={categories}
          activeCategory={activeCategory}
          onChange={setActiveCategory}
        />
        {loading ? (
          <div className="event-grid">
            <div className="skeleton skeleton-card" />
            <div className="skeleton skeleton-card" />
            <div className="skeleton skeleton-card" />
          </div>
        ) : (
          <EventGrid events={visibleEvents} />
        )}
      </section>

      <section className="container highlight-strip">
        <div>
          <p className="section-eyebrow">Experiencia Butaca</p>
          <h3>Comprá de forma segura, recibí tus entradas digitales al instante y accedé sin filas con tu código QR.</h3>
        </div>
        <div className="highlight-stats">
          <span>🔒 Compra 100% Protegida</span>
          <span>🎟️ Pases Digitales QR</span>
          <span>🪪 Entradas Nominales sin Reventa</span>
          <span>💳 Tarjetas y Billeteras Virtuales</span>
        </div>
      </section>

      <section className="container home-section">
        <SectionTitle
          eyebrow="Destacados"
          title="Populares de la semana"
          text="Los espectáculos más elegidos con venta de entradas habilitada."
        />
        {loading ? (
          <div className="event-grid">
            <div className="skeleton skeleton-card" />
            <div className="skeleton skeleton-card" />
            <div className="skeleton skeleton-card" />
          </div>
        ) : (
          <EventGrid events={popularEvents.length > 0 ? popularEvents : visibleEvents.slice(0, 3)} />
        )}
      </section>
    </>
  );
}

export default HomePage;

