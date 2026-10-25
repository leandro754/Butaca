function RecordFilters({
  value,
  onChange,
  placeholder = "Buscar…",
  dateLabel = "Fecha",
  children,
}) {
  function update(key, next) {
    onChange({ ...value, [key]: next });
  }
  return (
    <div className="record-filters">
      <label className="filter-search">
        Buscar
        <input
          type="search"
          value={value.search}
          placeholder={placeholder}
          onChange={(e) => update("search", e.target.value)}
        />
      </label>
      {children}
      <label>
        {dateLabel}: desde
        <input
          type="date"
          value={value.from}
          max={value.to || undefined}
          onChange={(e) => update("from", e.target.value)}
        />
      </label>
      <label>
        Hasta
        <input
          type="date"
          value={value.to}
          min={value.from || undefined}
          onChange={(e) => update("to", e.target.value)}
        />
      </label>
      <label>
        Ordenar por fecha
        <select
          value={value.sort}
          onChange={(e) => update("sort", e.target.value)}
        >
          <option value="asc">Más antiguas primero</option>
          <option value="desc">Más recientes primero</option>
        </select>
      </label>
      <button
        className="text-link reset-filters"
        type="button"
        onClick={() =>
          onChange({
            ...value,
            search: "",
            from: "",
            to: "",
            status: "",
            event: "",
          })
        }
      >
        Limpiar
      </button>
    </div>
  );
}

export default RecordFilters;
