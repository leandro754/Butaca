const labels = {
  PUBLICADO: "En venta",
  BORRADOR: "Borrador",
  CANCELADO: "Cancelado",
  CONFIRMADA: "Confirmada",
  USADA: "Utilizada",
  ANULADA: "Anulada",
  APROBADO: "Aprobado",
  REEMBOLSADO: "Reembolsado",
  PENDIENTE: "Pendiente",
  ENVIADO: "Aceptado por correo",
  ENVIADA: "Aceptado por correo",
  FALLIDA: "Error de envío",
  ERROR_ENVIO: "Error de envío",
  EXITOSO: "Correcto",
  RECHAZADO: "Rechazado",
  BLOQUEADO: "Bloqueado",
  CLIENTE: "Cliente",
  ORGANIZADOR: "Organizador",
  ADMIN: "Administrador",
};
function StatusBadge({ status }) {
  const tone = ["PUBLICADO", "CONFIRMADA", "APROBADO", "EXITOSO"].includes(
    status,
  )
    ? "success"
    : [
          "CANCELADO",
          "ANULADA",
          "ERROR_ENVIO",
          "RECHAZADO",
          "BLOQUEADO",
        ].includes(status)
      ? "danger"
      : ["BORRADOR", "PENDIENTE"].includes(status)
        ? "warning"
        : "info";
  return (
    <span className={`badge-status badge-${tone}`}>
      {labels[status] || status || "Sin estado"}
    </span>
  );
}
export default StatusBadge;
