import { useEffect, useRef, useState } from "react";
import { formatPrice } from "../../utils/formatters";

export default function CancelEventDialog({ event, onClose, onConfirm }) {
  const dialog = useRef(null);
  const [step, setStep] = useState(1);
  const [accepted, setAccepted] = useState(false);
  const [title, setTitle] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    const opener = document.activeElement;
    dialog.current.showModal();
    return () => opener?.focus?.();
  }, []);
  useEffect(() => {
    if (step === 2)
      dialog.current.querySelector("input:not([type=checkbox])")?.focus();
  }, [step]);
  async function confirm(e) {
    e.preventDefault();
    if (!accepted || title !== event.nombre) return;
    setBusy(true);
    setError("");
    try {
      await onConfirm({
        confirmar_reembolsos: accepted,
        titulo_confirmacion: title,
        version: event.version,
      });
      onClose();
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }
  return (
    <dialog
      className="cancel-event-dialog"
      ref={dialog}
      onCancel={(e) => {
        if (busy) e.preventDefault();
        else onClose();
      }}
      aria-labelledby="cancel-event-title"
    >
      <form onSubmit={confirm}>
        <p className="section-eyebrow">Confirmación {step} de 2</p>
        <h2 id="cancel-event-title">
          {step === 1 ? "Antes de cancelar" : "Confirmá el nombre del evento"}
        </h2>
        <p>
          <strong>{event.nombre}</strong>
        </p>
        {step === 1 ? (
          <>
            <p>
              El evento saldrá de la cartelera. Se anularán sus entradas
              vigentes y se registrarán los reembolsos de las compras.
            </p>
            <div className="cancel-impact">
              <span>{event.entradasVendidas} entradas vendidas</span>
              <strong>{formatPrice(event.totalVendido)}</strong>
            </div>
            <p className="quiet-note">
              La pasarela es de prueba: el reembolso se registra en Butaca y no
              mueve dinero real. El aviso por correo sí se enviará cuando el
              correo esté configurado.
            </p>
            <label className="confirm-check">
              <input
                type="checkbox"
                checked={accepted}
                onChange={(e) => setAccepted(e.target.checked)}
              />
              Entiendo que esta acción cancela el evento y sus entradas.
            </label>
          </>
        ) : (
          <label>
            Escribí exactamente «{event.nombre}»
            <input
              autoComplete="off"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={busy}
              required
            />
          </label>
        )}
        {error ? (
          <p className="auth-error" role="alert">
            {error}
          </p>
        ) : null}
        <div className="form-actions-row">
          <button
            type="button"
            className="button button-secondary"
            disabled={busy}
            onClick={onClose}
          >
            Conservar evento
          </button>
          {step === 1 ? (
            <button
              type="button"
              className="button button-primary"
              disabled={!accepted}
              onClick={() => setStep(2)}
            >
              Continuar
            </button>
          ) : (
            <button
              type="submit"
              className="button cancel-confirm-button"
              disabled={busy || title !== event.nombre}
            >
              {busy ? "Cancelando…" : "Cancelar evento y registrar reembolsos"}
            </button>
          )}
        </div>
      </form>
    </dialog>
  );
}
