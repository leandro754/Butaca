import { useMemo, useState } from "react";
import { formatDateTime, formatPrice } from "../../utils/formatters";
import {
  approvedRevenue,
  isInDateRange,
  normalizeText,
  sortByDate,
} from "../../utils/collectionFilters";
import RecordFilters from "../ui/RecordFilters";
import Pagination from "../ui/Pagination";
import StatusBadge from "../ui/StatusBadge";

function PurchaseList({ orders, technical = false, onValidate }) {
  const [filters, setFilters] = useState({
    search: "",
    from: "",
    to: "",
    sort: "desc",
    event: "",
    status: "",
  });
  const [page, setPage] = useState(1);
  const eventOptions = [
    ...new Map(
      orders.map((order) => [String(order.evento_id), order.evento_titulo]),
    ).entries(),
  ];
  const filtered = useMemo(
    () =>
      sortByDate(
        orders.filter((order) => {
          const matchesSearch = normalizeText(
            [
              order.id,
              order.evento_titulo,
              order.cliente_email,
              order.cliente_dni,
              ...(order.entradas || []).flatMap((ticket) => [
                ticket.nombre_asistente,
                ticket.dni_asistente,
              ]),
            ].join(" "),
          ).includes(normalizeText(filters.search));
          return (
            matchesSearch &&
            isInDateRange(order.fecha_compra, filters.from, filters.to) &&
            (!filters.event || String(order.evento_id) === filters.event) &&
            (!filters.status || order.pago?.estado_pago === filters.status)
          );
        }),
        (order) => order.fecha_compra,
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
    <>
      <RecordFilters
        value={filters}
        onChange={updateFilters}
        dateLabel="Compra"
        placeholder="Evento, comprador, asistente o DNI"
      >
        <label>
          Evento
          <select
            value={filters.event}
            onChange={(e) =>
              updateFilters({ ...filters, event: e.target.value })
            }
          >
            <option value="">Todos los eventos</option>
            {eventOptions.map(([id, title]) => (
              <option key={id} value={id}>
                {title}
              </option>
            ))}
          </select>
        </label>
        <label>
          Pago
          <select
            value={filters.status}
            onChange={(e) =>
              updateFilters({ ...filters, status: e.target.value })
            }
          >
            <option value="">Todos los estados</option>
            {[
              "APROBADO",
              "PENDIENTE",
              "RECHAZADO",
              "REEMBOLSADO",
              "CANCELADO",
            ].map((status) => (
              <option key={status}>{status}</option>
            ))}
          </select>
        </label>
      </RecordFilters>
      <div className="list-summary" aria-live="polite">
        <span>{filtered.length} compras</span>
        <span>
          Pagos aprobados en esta selección{" "}
          <strong>{formatPrice(approvedRevenue(filtered))}</strong>
        </span>
      </div>
      <div className="purchase-list">
        {filtered.slice((visiblePage - 1) * 8, visiblePage * 8).map((order) => (
          <details className="purchase-row" key={order.id}>
            <summary>
              <span className="record-id">
                #{String(order.id).padStart(4, "0")}
              </span>
              <span className="record-main">
                <strong>{order.evento_titulo || "Compra de entradas"}</strong>
                <small>
                  {order.cliente_email} · {order.cantidad} entrada
                  {order.cantidad === 1 ? "" : "s"}
                </small>
              </span>
              <time className="record-date">
                {formatDateTime(order.fecha_compra)}
              </time>
              <StatusBadge
                status={order.pago?.estado_pago || order.estado_compra}
              />
              <strong className="record-amount">
                {formatPrice(order.total)}
              </strong>
              <span className="disclosure-mark" aria-hidden="true">
                +
              </span>
            </summary>
            <div className="purchase-detail">
              <p>
                Comprador: {order.cliente_email} · DNI{" "}
                {order.cliente_dni || "Sin informar"} · Compra{" "}
                {order.estado_compra?.toLowerCase()}
              </p>
              {technical ? (
                <dl className="technical-detail">
                  <div>
                    <dt>Pasarela / medio</dt>
                    <dd>
                      {order.pago?.pasarela_pago || "—"} ·{" "}
                      {order.pago?.medio_pago || "—"}{" "}
                      {order.pago?.ultimos_cuatro_digitos
                        ? `· **** ${order.pago.ultimos_cuatro_digitos}`
                        : ""}
                    </dd>
                  </div>
                  <div>
                    <dt>Referencia del pago</dt>
                    <dd>
                      <code>{order.pago?.referencia_externa_pago || "—"}</code>
                    </dd>
                  </div>
                  <div>
                    <dt>Comprobante</dt>
                    <dd>
                      {order.comprobante?.numero_comprobante ||
                        "Sin comprobante"}{" "}
                      · Envío demo:{" "}
                      {order.comprobante?.estado_envio_comprobante || "—"}
                    </dd>
                  </div>
                  <div>
                    <dt>Firma de integridad SHA-256</dt>
                    <dd>
                      <code>
                        {order.comprobante?.hash_cruce_seguridad || "—"}
                      </code>
                    </dd>
                  </div>
                </dl>
              ) : (
                <p>
                  Comprobante{" "}
                  {order.comprobante?.numero_comprobante || "no disponible"}
                </p>
              )}
              <div className="attendee-list">
                {(order.entradas || []).map((ticket) => (
                  <div key={ticket.id_entrada}>
                    <span>
                      <strong>{ticket.nombre_asistente}</strong>
                      <small>DNI {ticket.dni_asistente}</small>
                    </span>
                    <StatusBadge status={ticket.estado_entrada} />
                    {onValidate && ticket.estado_entrada === "CONFIRMADA" ? (
                      <button
                        type="button"
                        className="btn-copy"
                        onClick={() => onValidate(ticket.codigo_unico_entrada)}
                      >
                        Verificar en puerta
                      </button>
                    ) : null}
                  </div>
                ))}
              </div>
            </div>
          </details>
        ))}
        {!filtered.length ? (
          <div className="empty-state">No hay compras para estos filtros.</div>
        ) : null}
      </div>
      <Pagination
        count={filtered.length}
        page={visiblePage}
        onChange={setPage}
      />
    </>
  );
}
export default PurchaseList;
