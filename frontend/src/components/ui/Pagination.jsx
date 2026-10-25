function Pagination({ count, page, onChange, pageSize = 8 }) {
  const pages = Math.max(1, Math.ceil(count / pageSize));
  if (count <= pageSize) return null;
  return (
    <nav className="pagination" aria-label="Páginas de resultados">
      <span>
        {(page - 1) * pageSize + 1}–{Math.min(page * pageSize, count)} de{" "}
        {count}
      </span>
      <div className="row-actions">
        <button
          type="button"
          className="btn-copy"
          disabled={page <= 1}
          onClick={() => onChange(page - 1)}
        >
          Anterior
        </button>
        <span>
          Página {page} de {pages}
        </span>
        <button
          type="button"
          className="btn-copy"
          disabled={page >= pages}
          onClick={() => onChange(page + 1)}
        >
          Siguiente
        </button>
      </div>
    </nav>
  );
}
export default Pagination;
