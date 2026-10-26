import { Link } from "react-router-dom";
import Button from "../components/ui/Button";
import { getSelectedOrder } from "../utils/storage";
import { formatPrice } from "../utils/formatters";
import TicketPass from "../components/events/TicketPass";

function ConfirmationPage() {
  const order = getSelectedOrder();
  const confirmation = order?.confirmation;

  if (!order || !confirmation) {
    return (
      <section className="container page-block">
        <div className="empty-state">
          <p>No hay una compra reciente para mostrar.</p>
          <Link to="/" className="pill-link" style={{ marginTop: 12 }}>
            Ir a la cartelera
          </Link>
        </div>
      </section>
    );
  }

  const entradas = confirmation.entradas || [];
  const pago = confirmation.pago || confirmation.raw?.pago || null;
  const comprobante =
    confirmation.comprobante || confirmation.raw?.comprobante || null;
  const compradorNombre = confirmation.comprador?.nombre || "Cliente";

  const medioLegible =
    pago?.medio_pago === "TARJETA_CREDITO"
      ? `Tarjeta terminada en ${pago?.ultimos_cuatro_digitos || "4242"}`
      : pago?.medio_pago === "TARJETA_DEBITO"
        ? `Débito terminada en ${pago?.ultimos_cuatro_digitos || "4242"}`
        : pago?.pasarela_pago || "Billetera virtual";

  return (
    <section className="container page-block confirmation-page">
      <div className="confirmation-card">
        <p className="section-eyebrow" style={{ color: "var(--accent)" }}>
          Compra confirmada
        </p>
        <h1>¡Listo! Ya tenés tus entradas</h1>

        <div
          style={{
            background: "rgba(78, 205, 196, 0.06)",
            border: "1px solid rgba(78, 205, 196, 0.2)",
            borderRadius: "var(--r-md)",
            padding: "16px 20px",
            margin: "20px auto 28px",
            maxWidth: 680,
            textAlign: "left",
            fontSize: "0.92rem",
            color: "var(--text)",
            lineHeight: 1.6,
          }}
        >
          <strong>Tus entradas ya están disponibles para descargar.</strong>
          <div style={{ color: "var(--text-2)", marginTop: 4 }}>
            Podés volver a verlas desde Mis entradas. Tu comprobante es{" "}
            <strong>{comprobante?.numero_comprobante || "RECIBO"}</strong>.
            Recordá llevar tu DNI físico el día del evento para el ingreso.
          </div>
        </div>

        <div className="confirmation-details">
          <div>
            <span>Espectáculo</span>
            <strong>{order.event.nombre}</strong>
          </div>
          <div>
            <span>Medio de pago</span>
            <strong>{medioLegible}</strong>
          </div>
          <div>
            <span>Comprobante</span>
            <strong>{comprobante?.numero_comprobante || "Emitido"}</strong>
          </div>
          <div>
            <span>Total abonado</span>
            <strong style={{ color: "var(--primary-2)" }}>
              {formatPrice(order.total)}
            </strong>
          </div>
        </div>

        <div style={{ margin: "32px 0", textAlign: "left" }}>
          <h3 style={{ fontSize: "1.15rem", marginBottom: 16 }}>
            Tus entradas digitales
          </h3>
          {entradas.length ? (
            entradas.map((entrada, idx) => (
              <TicketPass
                key={entrada.id_entrada || idx}
                ticket={entrada}
                eventTitle={order.event.nombre}
                venue={order.event.recinto || order.event.ciudad}
                date={order.event.fecha}
                time={order.event.hora}
                buyerName={entrada.nombre_asistente || compradorNombre}
              />
            ))
          ) : (
            <TicketPass
              ticket={{
                id: confirmation.codigoReserva || "TICKET-1",
                precio_pagado: order.total,
              }}
              eventTitle={order.event.nombre}
              venue={order.event.recinto || order.event.ciudad}
              date={order.event.fecha}
              time={order.event.hora}
              buyerName={compradorNombre}
            />
          )}
        </div>

        <div
          className="hero-actions"
          style={{ justifyContent: "center", gap: 14 }}
        >
          <Link to="/dashboard">
            <Button>Ir a Mis Entradas</Button>
          </Link>
          <Link to="/">
            <Button variant="secondary">Explorar cartelera</Button>
          </Link>
        </div>
      </div>
    </section>
  );
}

export default ConfirmationPage;
