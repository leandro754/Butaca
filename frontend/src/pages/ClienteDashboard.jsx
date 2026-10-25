import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getUserOrders } from "../services/orderService";
import { formatDateTime, formatPrice } from "../utils/formatters";
import {
  isInDateRange,
  normalizeText,
  sortByDate,
} from "../utils/collectionFilters";
import TicketPass from "../components/events/TicketPass";
import RecordFilters from "../components/ui/RecordFilters";
import Pagination from "../components/ui/Pagination";

function ClienteDashboard({ currentUser }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filters, setFilters] = useState({
    search: "",
    from: "",
    to: "",
    sort: "asc",
    basis: "evento",
    status: "",
  });
  const [page, setPage] = useState(1);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    getUserOrders(currentUser.id)
      .then((orders) => {
        if (active) setOrders(orders || []);
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
  }, [currentUser.id]);
  const filtered = useMemo(
    () =>
      sortByDate(
        orders.filter((order) => {
          const date =
            filters.basis === "evento"
              ? order.evento?.fecha
              : order.fecha_compra;
          return (
            normalizeText(
              `${order.evento_titulo} ${order.comprobante?.numero_comprobante || ""}`,
            ).includes(normalizeText(filters.search)) &&
            isInDateRange(date, filters.from, filters.to) &&
            (!filters.status ||
              order.entradas.some(
                (ticket) => ticket.estado_entrada === filters.status,
              ))
          );
        }),
        (order) =>
          filters.basis === "evento" ? order.evento?.fecha : order.fecha_compra,
        filters.sort,
      ),
    [orders, filters],
  );
  const visiblePage = Math.min(
    page,
    Math.max(1, Math.ceil(filtered.length / 8)),
  );
  function updateFilters(next) {
    setFilters(next);
    setPage(1);
  }
  return (
    <section className="container page-block workspace-page">
      <div className="dashboard-header">
        <div>
          <p className="section-eyebrow">Tu cuenta / {currentUser.nombre}</p>
          <h1>Mis entradas.</h1>
          <p className="section-text">
            Tu QR y tu DNI. Todo lo que necesitás para estar ahí.
          </p>
        </div>
        <Link className="button button-primary" to="/buscar">
          Buscar otra salida ↗
        </Link>
      </div>
      {error ? (
        <div className="auth-error" role="alert">
          {error}
        </div>
      ) : null}
      {loading ? (
        <div
          className="skeleton skeleton-card"
          aria-label="Cargando tus entradas"
        />
      ) : orders.length ? (
        <>
          <RecordFilters
            value={filters}
            onChange={updateFilters}
            dateLabel={filters.basis === "evento" ? "Evento" : "Compra"}
            placeholder="Evento o comprobante"
          >
            <label>
              Fecha de
              <select
                value={filters.basis}
                onChange={(e) =>
                  updateFilters({ ...filters, basis: e.target.value })
                }
              >
                <option value="evento">Evento</option>
                <option value="compra">Compra</option>
              </select>
            </label>
            <label>
              Entradas
              <select
                value={filters.status}
                onChange={(e) =>
                  updateFilters({ ...filters, status: e.target.value })
                }
              >
                <option value="">Todas</option>
                <option value="CONFIRMADA">Para usar</option>
                <option value="USADA">Utilizadas</option>
                <option value="ANULADA">Anuladas</option>
              </select>
            </label>
          </RecordFilters>
          <p className="results-count" aria-live="polite">
            {filtered.length} compras en esta selección
          </p>
          <div className="client-orders">
            {filtered
              .slice((visiblePage - 1) * 8, visiblePage * 8)
              .map((order) => (
                <article className="dashboard-panel" key={order.id}>
                  <div className="client-order-heading">
                    <div>
                      <h2>{order.evento_titulo}</h2>
                      <p>
                        Comprada el {formatDateTime(order.fecha_compra)} ·{" "}
                        {order.comprobante?.numero_comprobante ||
                          `Compra #${order.id}`}
                      </p>
                    </div>
                    <strong>{formatPrice(order.total)}</strong>
                  </div>
                  {!order.evento ? (
                    <p className="quiet-note">
                      No pudimos consultar la fecha y el lugar del evento. Podés
                      seguir viendo tus códigos.
                    </p>
                  ) : null}
                  {order.entradas
                    .filter(
                      (ticket) =>
                        !filters.status ||
                        ticket.estado_entrada === filters.status,
                    )
                    .map((ticket) => (
                      <TicketPass
                        key={ticket.id_entrada}
                        ticket={ticket}
                        eventTitle={
                          ticket.evento?.nombre || order.evento_titulo
                        }
                        venue={ticket.evento?.recinto}
                        date={ticket.evento?.fecha}
                        time={ticket.evento?.hora}
                        buyerName={
                          ticket.nombre_asistente || currentUser.nombre
                        }
                      />
                    ))}
                </article>
              ))}
          </div>
          {!filtered.length ? (
            <p className="empty-state">No hay entradas para estos filtros.</p>
          ) : null}
          <Pagination
            count={filtered.length}
            page={visiblePage}
            onChange={setPage}
          />
        </>
      ) : !error ? (
        <div className="empty-state">
          <p>Todavía no tenés entradas. Tu próxima salida te está esperando.</p>
          <Link className="text-link" to="/buscar">
            Explorar cartelera ↗
          </Link>
        </div>
      ) : null}
    </section>
  );
}
export default ClienteDashboard;
