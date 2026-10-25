import { useEffect, useId, useRef, useState } from "react";
import { formatDate } from "../../utils/formatters";
import QRCodeImage, { downloadTicketAsImage } from "../ui/QRCodeImage";
import StatusBadge from "../ui/StatusBadge";

function TicketPass({ ticket, eventTitle, venue, date, time, buyerName }) {
  const [showModal, setShowModal] = useState(false);
  const dialog = useRef(null);
  const headingId = useId();
  const qrData = ticket.codigo_unico_entrada || ticket.codigoUnicoEntrada;
  const titular =
    ticket.nombre_asistente || ticket.nombreAsistente || buyerName || "Titular";
  const dni = ticket.dni_asistente || ticket.dniAsistente || "";
  const status = ticket.estado_entrada || "CONFIRMADA";
  const usable = status === "CONFIRMADA";
  useEffect(() => {
    if (showModal) dialog.current?.showModal();
    else dialog.current?.close();
  }, [showModal]);
  function download() {
    downloadTicketAsImage({
      ticket,
      eventTitle,
      venue,
      date: date ? formatDate(date) : "Fecha a confirmar",
      time,
      buyerName: dni ? `${titular} (DNI ${dni})` : titular,
    });
  }
  return (
    <>
      <div className={`ticket-pass ${usable ? "" : "ticket-inactive"}`}>
        <div className="ticket-pass-main">
          <div className="ticket-pass-header">
            <h3 className="ticket-pass-title">
              {eventTitle || "Entrada Butaca"}
            </h3>
            <StatusBadge status={status} />
          </div>
          <div className="ticket-pass-meta">
            {date ? (
              <span>
                {formatDate(date)} {time ? `· ${time} h` : ""}
              </span>
            ) : (
              <span>Fecha del evento a confirmar</span>
            )}
            {venue ? <span>· {venue}</span> : null}
          </div>
          <p className="ticket-holder">
            {titular}
            {dni ? <span> · DNI {dni}</span> : null}
          </p>
          <div className="row-actions">
            <button
              type="button"
              className="btn-copy"
              onClick={() => setShowModal(true)}
              disabled={!qrData}
            >
              Ver entrada
            </button>
            <button
              type="button"
              className="btn-copy"
              onClick={download}
              disabled={!qrData || !usable}
            >
              Descargar entrada ↓
            </button>
          </div>
          {!usable ? (
            <p className="quiet-note">
              {status === "USADA"
                ? "Esta entrada ya se utilizó."
                : "Esta entrada fue anulada y no habilita el ingreso."}
            </p>
          ) : null}
        </div>
        <button
          className="ticket-pass-side"
          type="button"
          onClick={() => setShowModal(true)}
          disabled={!qrData}
          aria-label={`Ampliar entrada de ${titular}`}
        >
          {qrData && usable ? (
            <QRCodeImage value={qrData} size={90} />
          ) : (
            <StatusBadge status={status} />
          )}
        </button>
      </div>
      <dialog
        className="ticket-dialog"
        ref={dialog}
        onCancel={() => setShowModal(false)}
        onClose={() => setShowModal(false)}
        aria-labelledby={headingId}
      >
        <button
          type="button"
          className="dialog-close"
          aria-label="Cerrar entrada"
          onClick={() => setShowModal(false)}
        >
          ×
        </button>
        <p className="section-eyebrow">Butaca / Tu entrada</p>
        <h2 id={headingId}>{eventTitle}</h2>
        <p>
          {date ? formatDate(date) : "Fecha a confirmar"}{" "}
          {time ? `· ${time} h` : ""}
        </p>
        <p>{venue}</p>
        <StatusBadge status={status} />
        <div className="dialog-qr">
          {qrData && usable ? (
            <QRCodeImage value={qrData} size={220} />
          ) : (
            <p>Esta entrada no habilita el ingreso.</p>
          )}
        </div>
        <strong>{titular}</strong>
        <p>DNI {dni || "Sin informar"}</p>
        {usable ? (
          <>
            <p className="quiet-note">
              Presentá este QR junto con tu DNI físico en puerta.
            </p>
            <button
              type="button"
              className="button button-primary"
              onClick={download}
            >
              Descargar entrada ↓
            </button>
          </>
        ) : null}
      </dialog>
    </>
  );
}
export default TicketPass;
