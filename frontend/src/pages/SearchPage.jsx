import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import EventGrid from "../components/events/EventGrid";
import SectionTitle from "../components/ui/SectionTitle";
import { categoryOptions as fallbackCategoryOptions } from "../data/categories";
import { getAllEvents, getCategories } from "../services/eventService";

function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialCategory = searchParams.get("categoria") || "Todos";

  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState(fallbackCategoryOptions);
  const [searchText, setSearchText] = useState("");
  const [category, setCategory] = useState(initialCategory);
  const [maxPrice, setMaxPrice] = useState(50000);
  const [sortBy, setSortBy] = useState("fecha");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const paramCat = searchParams.get("categoria");
    if (paramCat) {
      setCategory(paramCat);
    } else {
      setCategory("Todos");
    }
  }, [searchParams]);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      getAllEvents().catch(() => []),
      getCategories().catch(() => [])
    ])
      .then(([eventsData, categoriesData]) => {
        setEvents(eventsData || []);
        if (categoriesData && categoriesData.length > 0) {
          setCategories(["Todos", ...categoriesData.map((item) => item.nombre)]);
        }
      })
      .finally(() => setLoading(false));
  }, []);

  const filteredEvents = useMemo(() => {
    let result = [...events];

    if (searchText.trim()) {
      const term = searchText.toLowerCase();
      result = result.filter(
        (event) =>
          (event.nombre && event.nombre.toLowerCase().includes(term)) ||
          (event.artista && event.artista.toLowerCase().includes(term)) ||
          (event.ciudad && event.ciudad.toLowerCase().includes(term)) ||
          (event.recinto && event.recinto.toLowerCase().includes(term))
      );
    }

    if (category !== "Todos") {
      result = result.filter((event) => event.categoria === category);
    }

    result = result.filter((event) => event.precioMinimo <= maxPrice);

    if (sortBy === "precio") {
      result.sort((a, b) => a.precioMinimo - b.precioMinimo);
    } else if (sortBy === "popularidad") {
      result.sort((a, b) => b.disponibilidad - a.disponibilidad);
    } else {
      result.sort((a, b) => new Date(a.fecha) - new Date(b.fecha));
    }

    return result;
  }, [category, events, maxPrice, searchText, sortBy]);

  function handleReset() {
    setSearchText("");
    setCategory("Todos");
    setMaxPrice(50000);
    setSortBy("fecha");
  }

  return (
    <section className="container page-block">
      <SectionTitle
        eyebrow="Búsqueda de eventos"
        title="Encontrá tu próximo espectáculo"
        text="Filtrá la cartelera oficial por nombre, ciudad, categoría o rango de precios."
      />

      <div className="search-layout">
        <aside className="search-panel">
          <h2 style={{ fontSize: "1.1rem", marginBottom: 16 }}>Filtros</h2>

          <label>
            Buscar por espectáculo, sala o artista
            <input
              type="text"
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
              placeholder="Ej: Rock, Teatro, Vorterix..."
            />
          </label>

          <label>
            Categoría
            <select value={category} onChange={(event) => setCategory(event.target.value)}>
              {categories.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>

          <label>
            Ordenar por
            <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
              <option value="fecha">Fecha (más cercanos)</option>
              <option value="precio">Precio (menor a mayor)</option>
              <option value="popularidad">Disponibilidad (más cupos)</option>
            </select>
          </label>

          <label>
            Precio máximo: ${maxPrice.toLocaleString("es-AR")}
            <input
              type="range"
              min="1000"
              max="50000"
              step="1000"
              value={maxPrice}
              onChange={(event) => setMaxPrice(Number(event.target.value))}
            />
          </label>

          <button
            type="button"
            className="text-link"
            onClick={handleReset}
            style={{ width: "100%", textAlign: "center", marginTop: 12 }}
          >
            Limpiar filtros
          </button>
        </aside>

        <div className="search-results">
          <p className="results-count">
            {loading ? "Buscando eventos..." : `${filteredEvents.length} resultados encontrados`}
          </p>

          {loading ? (
            <div className="event-grid">
              <div className="skeleton skeleton-card" />
              <div className="skeleton skeleton-card" />
              <div className="skeleton skeleton-card" />
            </div>
          ) : (
            <EventGrid events={filteredEvents} />
          )}
        </div>
      </div>
    </section>
  );
}

export default SearchPage;

