import { useState } from "react";
import { createCategory, createLocation } from "../../services/eventService";

function CatalogManager({ categories, locations, onUpdated }) {
  const [creating, setCreating] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [values, setValues] = useState({
    name: "",
    description: "",
    address: "",
    capacity: "",
  });
  function open(type) {
    setCreating(type);
    setError("");
    setValues({ name: "", description: "", address: "", capacity: "" });
  }
  function change(event) {
    setValues({ ...values, [event.target.name]: event.target.value });
  }
  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (creating === "categoria")
        await createCategory({
          nombre: values.name.trim(),
          descripcion: values.description.trim(),
        });
      else
        await createLocation({
          nombre_lugar: values.name.trim(),
          direccion: values.address.trim(),
          capacidad_maxima: Number(values.capacity),
          activo: true,
        });
      setCreating("");
      await onUpdated();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }
  return (
    <>
      {creating ? (
        <form className="dashboard-panel catalog-form" onSubmit={submit}>
          <div className="panel-heading">
            <h2>
              {creating === "categoria" ? "Nueva categoría" : "Nuevo recinto"}
            </h2>
            <button
              className="text-link"
              type="button"
              onClick={() => setCreating("")}
              disabled={saving}
            >
              Cerrar
            </button>
          </div>
          <div className="form-grid two-columns">
            <label>
              Nombre
              <input
                name="name"
                value={values.name}
                onChange={change}
                maxLength={creating === "categoria" ? 100 : 150}
                required
              />
            </label>
            {creating === "categoria" ? (
              <label>
                Descripción
                <input
                  name="description"
                  value={values.description}
                  onChange={change}
                  required
                />
              </label>
            ) : (
              <>
                <label>
                  Capacidad máxima
                  <input
                    name="capacity"
                    type="number"
                    min="1"
                    max="2147483647"
                    step="1"
                    value={values.capacity}
                    onChange={change}
                    required
                  />
                </label>
                <label className="span-2">
                  Dirección
                  <input
                    name="address"
                    value={values.address}
                    onChange={change}
                    maxLength={255}
                    required
                  />
                </label>
              </>
            )}
          </div>
          {error ? (
            <div className="auth-error" role="alert">
              {error}
            </div>
          ) : null}
          <div className="form-actions-row">
            <button
              type="submit"
              className="button button-primary"
              disabled={saving}
            >
              {saving ? "Guardando…" : "Guardar"}
            </button>
          </div>
        </form>
      ) : null}
      <div className="dashboard-grid">
        <section className="dashboard-panel">
          <div className="panel-heading">
            <h2>Categorías · {categories.length}</h2>
            <button
              type="button"
              className="btn-copy"
              onClick={() => open("categoria")}
            >
              + Agregar
            </button>
          </div>
          <div className="summary-list">
            {categories.map((item) => (
              <div key={item.id}>
                <span>
                  <strong>{item.nombre}</strong>
                  <small>{item.descripcion}</small>
                </span>
              </div>
            ))}
          </div>
        </section>
        <section className="dashboard-panel">
          <div className="panel-heading">
            <h2>Recintos activos · {locations.length}</h2>
            <button
              type="button"
              className="btn-copy"
              onClick={() => open("recinto")}
            >
              + Agregar
            </button>
          </div>
          <div className="summary-list">
            {locations.map((item) => (
              <div key={item.id}>
                <span>
                  <strong>{item.nombre}</strong>
                  <small>{item.direccion}</small>
                  <small>Capacidad: {item.capacidadMaxima} personas</small>
                </span>
                <span className="badge-status badge-success">Activo</span>
              </div>
            ))}
          </div>
        </section>
      </div>
    </>
  );
}
export default CatalogManager;
