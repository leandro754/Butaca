import { Link } from "react-router-dom";
import { dateParts, formatDate, formatPrice } from "../../utils/formatters";

function EventCard({ event, entranceDelay = 0 }) {
  const parts = dateParts(event.fecha);
  return (
    <article
      className="event-card"
      style={{ "--card-delay": `${entranceDelay}ms` }}
    >
      <Link
        to={`/eventos/${event.id}`}
        className="event-poster"
        aria-label={`Ver entradas para ${event.nombre}`}
      >
        <img
          src={event.imagen}
          alt=""
          className="event-card-image"
          loading="lazy"
        />
        <span className="poster-category">{event.categoria}</span>
        <span className="poster-arrow" aria-hidden="true">
          ↗
        </span>
      </Link>
      <div className="event-card-body">
        <div className="card-date">
          <strong>{parts.day}</strong>
          <span>{parts.month}</span>
        </div>
        <div className="card-info">
          <p className="event-date-line">
            {formatDate(event.fecha)} · {event.hora} h
          </p>
          <h3>
            <Link to={`/eventos/${event.id}`}>{event.nombre}</Link>
          </h3>
          <p className="event-meta">{event.recinto || event.ciudad}</p>
          <div className="event-card-bottom">
            <span className="event-price">
              Desde <strong>{formatPrice(event.precioMinimo)}</strong>
            </span>
            {event.disponibilidad <= 50 ? (
              <span className="low-stock">Últimas {event.disponibilidad}</span>
            ) : null}
          </div>
          <Link to={`/eventos/${event.id}`} className="event-card-link">
            Ver entradas <span aria-hidden="true">↗</span>
          </Link>
        </div>
      </div>
    </article>
  );
}
export default EventCard;
