import { useState } from "react";

export default function EventEditorForm({
  event,
  categories,
  locations,
  organizerId,
  busy,
  onSave,
  onCancel,
}) {
  const [form, setForm] = useState(() => ({
    titulo_evento: event?.nombre || "",
    descripcion_evento: event?.descripcion || "",
    fecha_evento: event?.fecha || "",
    hora_evento: event?.hora || "20:00",
    cupos_totales_evento: event?.cuposTotales || "",
    categoria_id: event?.categoriaId || categories[0]?.id || "",
    ubicacion_id:
      event?.ubicacionId || locations.find((item) => item.activo)?.id || "",
    imagen_url: event?.raw?.imagen_url || "",
  }));
  const [types, setTypes] = useState(
    () =>
      event?.raw?.tipos_entrada?.map((item) => ({ ...item })) || [
        { nombre: "General", descripcion: "", precio: "", activo: true },
      ],
  );
  const [error, setError] = useState("");
  const location = locations.find(
    (item) => String(item.id) === String(form.ubicacion_id),
  );
  const occupied = Math.max(
    0,
    (event?.cuposTotales || 0) - (event?.disponibilidad || 0),
  );
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
  function field(change) {
    setForm((value) => ({
      ...value,
      [change.target.name]: change.target.value,
    }));
  }
  function changeType(index, key, value) {
    setTypes((items) =>
      items.map((item, i) => (i === index ? { ...item, [key]: value } : item)),
    );
  }
  async function submit(e) {
    e.preventDefault();
    setError("");
    const active = types.filter((type) => type.activo);
    if (!active.length) {
      setError("Dejá al menos un tipo de entrada activo.");
      return;
    }
    try {
      await onSave({
        ...form,
        categoria_id: Number(form.categoria_id),
        ubicacion_id: Number(form.ubicacion_id),
        cupos_totales_evento: Number(form.cupos_totales_evento),
        organizador_id_evento: organizerId,
        precio_entrada_evento: Math.min(
          ...active.map((type) => Number(type.precio)),
        ),
        tipos_entrada: types.map((type) => ({
          ...type,
          nombre: type.nombre.trim(),
          precio: Number(type.precio),
        })),
        ...(event
          ? { version: event.version }
          : { estado_publicacion_evento: "BORRADOR" }),
      });
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <form className="event-editor" onSubmit={submit}>
      <fieldset disabled={busy}>
        <section className="dashboard-panel" id="detalles-evento">
          <div className="panel-heading">
            <div>
              <p className="section-eyebrow">Fecha, lugar y afiche</p>
              <h2>Detalles del evento</h2>
            </div>
          </div>
          <div className="form-grid two-columns">
            <label className="span-2">
              Nombre del evento
              <input
                name="titulo_evento"
                value={form.titulo_evento}
                onChange={field}
                maxLength={200}
                required
              />
            </label>
            <label className="span-2">
              Descripción
              <textarea
                name="descripcion_evento"
                value={form.descripcion_evento}
                onChange={field}
                rows={4}
                required
              />
            </label>
            <label>
              Fecha
              <input
                type="date"
                name="fecha_evento"
                min={today}
                value={form.fecha_evento}
                onChange={field}
                required
              />
            </label>
            <label>
              Hora
              <input
                type="time"
                name="hora_evento"
                value={form.hora_evento}
                onChange={field}
                required
              />
            </label>
            <label>
              Categoría
              <select
                name="categoria_id"
                value={form.categoria_id}
                onChange={field}
                required
              >
                <option value="" disabled>
                  Elegí una categoría
                </option>
                {categories.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.nombre}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Recinto
              <select
                name="ubicacion_id"
                value={form.ubicacion_id}
                onChange={field}
                required
              >
                <option value="" disabled>
                  Elegí un recinto
                </option>
                {locations
                  .filter(
                    (item) => item.activo || item.id === event?.ubicacionId,
                  )
                  .map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.nombre} · máx. {item.capacidadMaxima}
                    </option>
                  ))}
              </select>
            </label>
            <label>
              Cantidad total de entradas
              <input
                type="number"
                name="cupos_totales_evento"
                min={Math.max(1, occupied)}
                max={location?.capacidadMaxima}
                step="1"
                value={form.cupos_totales_evento}
                onChange={field}
                required
              />
              <small>
                {occupied ? `${occupied} entradas ya ocupadas. ` : ""}Capacidad
                del recinto: {location?.capacidadMaxima ?? "—"}
              </small>
            </label>
            <label>
              URL del afiche
              <input
                type="url"
                name="imagen_url"
                value={form.imagen_url}
                onChange={field}
                placeholder="https://…"
              />
            </label>
          </div>
          {event ? (
            <p className="quiet-note">
              Si cambiás la fecha o la hora, avisaremos a quienes ya compraron
              entradas.
            </p>
          ) : null}
        </section>
        <section className="dashboard-panel" id="tarifas-evento">
          <div className="panel-heading">
            <div>
              <p className="section-eyebrow">Tarifas en pesos argentinos</p>
              <h2>Entradas y precios</h2>
            </div>
            <button
              type="button"
              className="btn-copy"
              disabled={types.length >= 20}
              onClick={() =>
                setTypes((items) => [
                  ...items,
                  { nombre: "", descripcion: "", precio: "", activo: true },
                ])
              }
            >
              + Agregar tipo
            </button>
          </div>
          <p className="quiet-note">
            Los cambios de precio se aplican a próximas compras. Las entradas
            emitidas conservan su valor.
          </p>
          <div className="ticket-type-editor">
            {types.map((type, index) => (
              <div
                className="ticket-type-row"
                key={type.id_tipo_entrada ?? `new-${index}`}
              >
                <label>
                  Tipo de entrada
                  <input
                    value={type.nombre}
                    maxLength={100}
                    onChange={(e) =>
                      changeType(index, "nombre", e.target.value)
                    }
                    placeholder="General, VIP, anticipada…"
                    required
                  />
                </label>
                <label>
                  Precio (ARS)
                  <input
                    type="number"
                    value={type.precio}
                    min="0"
                    max="9999999999.99"
                    step="0.01"
                    onChange={(e) =>
                      changeType(index, "precio", e.target.value)
                    }
                    required
                  />
                </label>
                <label className="type-active">
                  <input
                    type="checkbox"
                    checked={type.activo}
                    onChange={(e) =>
                      changeType(index, "activo", e.target.checked)
                    }
                  />{" "}
                  En venta
                </label>
                {!type.id_tipo_entrada && types.length > 1 ? (
                  <button
                    type="button"
                    className="text-link"
                    onClick={() =>
                      setTypes((items) => items.filter((_, i) => i !== index))
                    }
                  >
                    Quitar
                  </button>
                ) : (
                  <span className="quiet-note">
                    {type.id_tipo_entrada
                      ? "Desactivá para retirar de venta"
                      : ""}
                  </span>
                )}
              </div>
            ))}
          </div>
        </section>
        {error ? (
          <p className="auth-error" role="alert">
            {error}
          </p>
        ) : null}
        <div className="editor-save-bar">
          <button type="button" className="text-link" onClick={onCancel}>
            Volver a mis eventos
          </button>
          <button
            className="button button-primary"
            type="submit"
            disabled={busy || !categories.length || !locations.length}
          >
            {busy
              ? "Guardando…"
              : event
                ? "Guardar cambios"
                : "Guardar borrador"}
          </button>
        </div>
      </fieldset>
    </form>
  );
}
