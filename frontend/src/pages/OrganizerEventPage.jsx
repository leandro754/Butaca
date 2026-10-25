import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  cancelEvent,
  getCategories,
  getEventById,
  getLocations,
  publishEvent,
  updateOrganizerEvent,
} from "../services/eventService";
import { formatDate, formatPrice } from "../utils/formatters";
import EventEditorForm from "../components/events/EventEditorForm";
import CancelEventDialog from "../components/events/CancelEventDialog";
import StatusBadge from "../components/ui/StatusBadge";

export default function OrganizerEventPage({ currentUser }) {
  const { eventId } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [categories, setCategories] = useState([]);
  const [locations, setLocations] = useState([]);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const allowed = ["ORGANIZADOR", "ADMIN"].includes(currentUser?.rol);
  useEffect(() => {
    if (!allowed) return;
    let active = true;
    setEvent(null);
    setError("");
    Promise.all([getEventById(eventId), getCategories(), getLocations()])
      .then(([data, cats, places]) => {
        if (!active) return;
        if (
          currentUser.rol !== "ADMIN" &&
          String(data.organizadorId) !== String(currentUser.id)
        )
          throw new Error("Este evento no pertenece a tu organización.");
        setEvent(data);
        setCategories(cats);
        setLocations(places);
      })
      .catch((err) => {
        if (active) setError(err.message);
      });
    return () => {
      active = false;
    };
  }, [eventId, currentUser?.id, allowed]);
  async function reload() {
    setEvent(await getEventById(eventId));
  }
  async function save(data) {
    setBusy(true);
    setNotice("");
    try {
      await updateOrganizerEvent(eventId, data);
      await reload();
      setNotice(
        "Cambios guardados. Los precios históricos de las entradas se conservaron.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function publish() {
    setBusy(true);
    setError("");
    try {
      await publishEvent(eventId);
      await reload();
      setNotice("El evento ya está publicado en la cartelera.");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }
  if (!allowed)
    return (
      <section className="container page-block">
        <p>Ingresá con tu cuenta de organizador para configurar eventos.</p>
        <Link className="text-link" to="/login">
          Ingresar ↗
        </Link>
      </section>
    );
  return (
    <section className="container page-block workspace-page event-settings-page">
      <Link to="/dashboard" className="text-link">
        ← Mis eventos
      </Link>
      {error ? (
        <p className="auth-error" role="alert">
          {error}
        </p>
      ) : null}
      {!event && !error ? (
        <div
          className="skeleton skeleton-card"
          aria-label="Cargando configuración"
        />
      ) : null}
      {event ? (
        <>
          <header className="event-settings-header">
            <img src={event.imagen} alt="" />
            <div>
              <p className="section-eyebrow">Configuración del evento</p>
              <h1>{event.nombre}</h1>
              <p>
                {formatDate(event.fecha)} · {event.hora} h · {event.recinto}
              </p>
              <StatusBadge status={event.estado} />
            </div>
            <div className="row-actions">
              {event.estado === "BORRADOR" ? (
                <button
                  type="button"
                  className="button button-primary"
                  disabled={busy}
                  onClick={publish}
                >
                  Publicar evento
                </button>
              ) : event.estado === "PUBLICADO" ? (
                <Link className="btn-copy" to={`/eventos/${event.id}`}>
                  Ver publicación ↗
                </Link>
              ) : null}
            </div>
          </header>
          {notice ? (
            <p className="success-notice" role="status">
              {notice}
            </p>
          ) : null}
          <div className="stats-grid event-metrics">
            <div className="stat-card">
              <span>Entradas vendidas</span>
              <strong>{event.entradasVendidas}</strong>
              <small>Excluye entradas reembolsadas</small>
            </div>
            <div className="stat-card">
              <span>Total vendido</span>
              <strong>{formatPrice(event.totalVendido)}</strong>
              <small>Importes históricos · ARS</small>
            </div>
            <div className="stat-card">
              <span>Ocupación</span>
              <strong>{event.ocupacion}%</strong>
              <small>
                {event.cuposTotales - event.disponibilidad} de{" "}
                {event.cuposTotales} lugares
              </small>
              <progress
                value={event.ocupacion}
                max="100"
                aria-label="Porcentaje de ocupación"
              />
            </div>
            <div className="stat-card">
              <span>Entradas disponibles</span>
              <strong>
                {event.estado === "CANCELADO" ? 0 : event.disponibilidad}
              </strong>
              <small>{event.cuposTotales} de capacidad total</small>
            </div>
          </div>
          {event.estado === "CANCELADO" ? (
            <div className="dashboard-panel">
              <h2>Evento cancelado</h2>
              <p>
                Las entradas se anularon y los reembolsos quedaron registrados.
                La configuración permanece disponible para consulta.
              </p>
            </div>
          ) : (
            <>
              <nav className="event-settings-nav" aria-label="Configuración">
                <a href="#detalles-evento">Detalles y fecha</a>
                <a href="#tarifas-evento">Entradas y precios</a>
                <a href="#cancelar-evento">Cancelación</a>
              </nav>
              <EventEditorForm
                key={`${event.id}-${event.version}`}
                event={event}
                categories={categories}
                locations={locations}
                organizerId={event.organizadorId}
                busy={busy}
                onSave={save}
                onCancel={() => navigate("/dashboard")}
              />
              <section
                className="dashboard-panel event-danger-zone"
                id="cancelar-evento"
              >
                <div>
                  <h2>Cancelar evento</h2>
                  <p>
                    Retirá el evento de la cartelera y registrá la devolución de
                    sus compras.
                  </p>
                </div>
                <button
                  type="button"
                  className="danger-action"
                  disabled={busy}
                  onClick={() => setCancelOpen(true)}
                >
                  Cancelar evento
                </button>
              </section>
            </>
          )}
          {cancelOpen ? (
            <CancelEventDialog
              event={event}
              onClose={() => setCancelOpen(false)}
              onConfirm={async (confirmation) => {
                await cancelEvent(eventId, confirmation);
                await reload();
                setNotice(
                  "Evento cancelado. Se registraron los reembolsos y los avisos quedaron pendientes de envío.",
                );
              }}
            />
          ) : null}
        </>
      ) : null}
    </section>
  );
}
