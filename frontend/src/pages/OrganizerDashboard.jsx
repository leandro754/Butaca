import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  createOrganizerEvent,
  getCategories,
  getLocations,
  getOrganizerEvents,
} from "../services/eventService";
import { dateParts, formatDate, formatPrice } from "../utils/formatters";
import {
  isInDateRange,
  normalizeText,
  sortByDate,
} from "../utils/collectionFilters";
import EventEditorForm from "../components/events/EventEditorForm";
import RecordFilters from "../components/ui/RecordFilters";
import Pagination from "../components/ui/Pagination";
import StatusBadge from "../components/ui/StatusBadge";
import { organizerTotals } from "../utils/organizerMetrics";

export default function OrganizerDashboard({ currentUser }) {
  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState([]);
  const [locations, setLocations] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [filters, setFilters] = useState({
    search: "",
    from: "",
    to: "",
    sort: "asc",
    status: "",
  });
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const organizerId = currentUser?.id;
  async function loadDashboard() {
    if (!organizerId) return;
    setLoading(true);
    setError("");
    try {
      const [data, cats, places] = await Promise.all([
        getOrganizerEvents(organizerId),
        getCategories(),
        getLocations(),
      ]);
      setEvents(data);
      setCategories(cats);
      setLocations(places);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    loadDashboard();
  }, [organizerId]);
  const totals = organizerTotals(events);
  const filtered = useMemo(
    () =>
      sortByDate(
        events.filter(
          (event) =>
            (!filters.status || event.estado === filters.status) &&
            normalizeText(`${event.nombre} ${event.recinto}`).includes(
              normalizeText(filters.search),
            ) &&
            isInDateRange(event.fecha, filters.from, filters.to),
        ),
        (event) => `${event.fecha}T${event.hora}`,
        filters.sort,
      ),
    [events, filters],
  );
  const visiblePage = Math.min(
    page,
    Math.max(1, Math.ceil(filtered.length / 8)),
  );
  function changeFilters(next) {
    setFilters(next);
    setPage(1);
  }
  async function create(data) {
    setBusy(true);
    try {
      await createOrganizerEvent(data);
      setShowForm(false);
      await loadDashboard();
      setNotice(
        "Borrador guardado. Abrilo desde la agenda para revisarlo y publicarlo.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="container page-block workspace-page">
      <div className="dashboard-header">
        <div>
          <p className="section-eyebrow">Butaca / Organizador</p>
          <h1>Mis eventos.</h1>
          <p className="section-text">
            Tu agenda, tus entradas y la configuración de cada evento.
          </p>
        </div>
        <div className="row-actions">
          <button
            type="button"
            className="btn-copy"
            onClick={loadDashboard}
            disabled={loading}
          >
            Actualizar
          </button>
          <button
            type="button"
            className="button button-primary"
            onClick={() => setShowForm((value) => !value)}
          >
            {showForm ? "Cerrar formulario" : "+ Crear evento"}
          </button>
        </div>
      </div>
      {error ? (
        <p className="auth-error" role="alert">
          {error}
        </p>
      ) : null}
      {notice ? (
        <p className="success-notice" role="status">
          {notice}
        </p>
      ) : null}
      <div className="stats-grid organizer-stats">
        <div className="stat-card">
          <span>Eventos</span>
          <strong>{loading ? "—" : totals.active}</strong>
          <small>Publicados con fecha vigente</small>
        </div>
        <div className="stat-card">
          <span>Borradores</span>
          <strong>{loading ? "—" : totals.drafts}</strong>
          <small>En preparación</small>
        </div>
        <div className="stat-card">
          <span>Entradas vendidas</span>
          <strong>{loading ? "—" : totals.sold}</strong>
          <small>Sin entradas reembolsadas</small>
        </div>
        <div className="stat-card">
          <span>Total vendido</span>
          <strong>{loading ? "—" : formatPrice(totals.revenue)}</strong>
          <small>Ventas vigentes · ARS</small>
        </div>
      </div>
      {showForm ? (
        <EventEditorForm
          categories={categories}
          locations={locations}
          organizerId={organizerId}
          busy={busy}
          onSave={create}
          onCancel={() => setShowForm(false)}
        />
      ) : null}
      {loading ? (
        <div className="skeleton skeleton-card" aria-label="Cargando eventos" />
      ) : (
        <section className="dashboard-panel">
          <div className="panel-heading">
            <div>
              <p className="section-eyebrow">Mis eventos</p>
              <h2>Agenda de eventos</h2>
            </div>
            <span className="count-label">{filtered.length} eventos</span>
          </div>
          <p className="quiet-note">
            Elegí un evento para editar sus detalles, reprogramar la fecha o
            configurar sus entradas.
          </p>
          <RecordFilters
            value={filters}
            onChange={changeFilters}
            dateLabel="Evento"
            placeholder="Nombre o recinto"
          >
            <label>
              Estado
              <select
                value={filters.status}
                onChange={(e) =>
                  changeFilters({ ...filters, status: e.target.value })
                }
              >
                <option value="">Todos</option>
                <option value="PUBLICADO">Publicados</option>
                <option value="BORRADOR">Borradores</option>
                <option value="CANCELADO">Cancelados</option>
              </select>
            </label>
          </RecordFilters>
          <div className="organizer-events">
            {filtered
              .slice((visiblePage - 1) * 8, visiblePage * 8)
              .map((event) => {
                const parts = dateParts(event.fecha);
                return (
                  <Link
                    className="organizer-event organizer-event-link"
                    to={`/dashboard/eventos/${event.id}`}
                    key={event.id}
                    aria-label={`Configurar ${event.nombre}`}
                  >
                    <span className="date-stamp">
                      <strong>{parts.day}</strong>
                      <span>{parts.month}</span>
                    </span>
                    <img src={event.imagen} alt="" loading="lazy" />
                    <span className="record-main">
                      <strong>{event.nombre}</strong>
                      <small>
                        {event.recinto} · {event.hora} h
                      </small>
                      <small>{formatDate(event.fecha)}</small>
                    </span>
                    <span className="event-occupancy">
                      <span>
                        {event.entradasVendidas} / {event.cuposTotales} entradas
                      </span>
                      <progress
                        value={event.ocupacion}
                        max="100"
                        aria-label={`Ocupación de ${event.nombre}`}
                      />
                      <small>{event.ocupacion}% de ocupación</small>
                    </span>
                    <StatusBadge status={event.estado} />
                    <span className="event-config-link">
                      Configurar <span aria-hidden="true">↗</span>
                    </span>
                  </Link>
                );
              })}
          </div>
          {!filtered.length ? (
            <p className="empty-state">No hay eventos con estos filtros.</p>
          ) : null}
          <Pagination
            page={visiblePage}
            count={filtered.length}
            pageSize={8}
            onChange={setPage}
          />
        </section>
      )}
    </section>
  );
}
