import { useEffect, useMemo, useState } from "react";
import {
  getAdminAuditLogs,
  getAdminPurchases,
  getAdminStats,
  getAdminUsers,
  getCategories,
  getLocations,
  updateUserRole,
} from "../services/eventService";
import { formatDateTime, formatPrice } from "../utils/formatters";
import {
  approvedRevenue,
  isInDateRange,
  normalizeText,
  sortByDate,
} from "../utils/collectionFilters";
import PurchaseList from "../components/events/PurchaseList";
import CatalogManager from "../components/events/CatalogManager";
import RecordFilters from "../components/ui/RecordFilters";
import Pagination from "../components/ui/Pagination";
import StatusBadge from "../components/ui/StatusBadge";

const tabs = [
  { id: "resumen", label: "Resumen" },
  { id: "usuarios", label: "Usuarios" },
  { id: "ventas", label: "Ventas" },
  { id: "catalogos", label: "Catálogos" },
  { id: "auditoria", label: "Actividad" },
];

function AdminDashboard() {
  const [data, setData] = useState({
    stats: {},
    users: [],
    purchases: [],
    categories: [],
    locations: [],
    auditLogs: [],
  });
  const [tab, setTab] = useState("resumen");
  const [loading, setLoading] = useState(true);
  const [roleUpdatingId, setRoleUpdatingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [userSearch, setUserSearch] = useState("");
  const [userRole, setUserRole] = useState("");
  const [userPage, setUserPage] = useState(1);
  const [auditFilters, setAuditFilters] = useState({
    search: "",
    from: "",
    to: "",
    sort: "desc",
    status: "",
  });
  const [auditPage, setAuditPage] = useState(1);
  const { stats, users, purchases, categories, locations, auditLogs } = data;

  async function loadAll() {
    setLoading(true);
    setError("");
    try {
      const [stats, users, purchases, categories, locations, auditLogs] =
        await Promise.all([
          getAdminStats(),
          getAdminUsers(),
          getAdminPurchases(),
          getCategories(),
          getLocations(),
          getAdminAuditLogs(),
        ]);
      setData({
        stats: stats || {},
        users: users || [],
        purchases: purchases || [],
        categories: categories || [],
        locations: locations || [],
        auditLogs: auditLogs || [],
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    loadAll();
  }, []);

  async function handleToggleOrganizer(user) {
    const nextRole =
      user.rol_usuario === "ORGANIZADOR" ? "CLIENTE" : "ORGANIZADOR";
    if (
      !window.confirm(
        `¿Cambiar a ${user.email_usuario} al rol ${nextRole.toLowerCase()}?`,
      )
    )
      return;
    setRoleUpdatingId(user.id_usuario);
    setError("");
    setNotice("");
    try {
      const updated = await updateUserRole(user.id_usuario, nextRole);
      setData((current) => ({
        ...current,
        users: current.users.map((item) =>
          item.id_usuario === user.id_usuario
            ? { ...item, ...updated, rol_usuario: nextRole }
            : item,
        ),
      }));
      setNotice(`Se actualizó el rol de ${user.email_usuario}.`);
    } catch (err) {
      setError(err.message || "No se pudo actualizar el rol.");
    } finally {
      setRoleUpdatingId(null);
    }
  }

  const filteredUsers = users.filter(
    (user) =>
      (!userRole || user.rol_usuario === userRole) &&
      normalizeText(
        `${user.nombre_usuario} ${user.apellido_usuario} ${user.email_usuario} ${user.dni_usuario}`,
      ).includes(normalizeText(userSearch)),
  );
  const visibleUserPage = Math.min(
    userPage,
    Math.max(1, Math.ceil(filteredUsers.length / 8)),
  );
  const filteredLogs = useMemo(
    () =>
      sortByDate(
        auditLogs.filter(
          (log) =>
            isInDateRange(
              log.fecha_auditoria,
              auditFilters.from,
              auditFilters.to,
            ) &&
            (!auditFilters.status ||
              (auditFilters.status === "INCIDENCIAS"
                ? log.resultado_auditoria !== "EXITOSO"
                : log.resultado_auditoria === auditFilters.status)) &&
            normalizeText(
              `${log.accion_auditoria} ${log.email_usuario} ${log.detalle_tracking}`,
            ).includes(normalizeText(auditFilters.search)),
        ),
        (log) => log.fecha_auditoria,
        auditFilters.sort,
      ),
    [auditLogs, auditFilters],
  );
  const visibleAuditPage = Math.min(
    auditPage,
    Math.max(1, Math.ceil(filteredLogs.length / 8)),
  );
  const needsReview = auditLogs.filter(
    (log) => log.resultado_auditoria !== "EXITOSO",
  );
  const pendingReceipts = purchases.filter(
    (order) =>
      order.comprobante &&
      order.comprobante.estado_envio_comprobante !== "ENVIADO",
  );
  const latest = sortByDate(
    purchases,
    (order) => order.fecha_compra,
    "desc",
  ).slice(0, 4);

  return (
    <section className="container page-block workspace-page">
      <div className="dashboard-header">
        <div>
          <p className="section-eyebrow">Butaca / Administración</p>
          <h1>Administración</h1>
          <p className="section-text">
            Ventas, personas y lo que necesita tu atención.
          </p>
        </div>
        <button
          type="button"
          className="btn-copy"
          onClick={loadAll}
          disabled={loading}
        >
          {loading ? "Actualizando…" : "Actualizar datos"}
        </button>
      </div>
      <nav className="workspace-tabs" aria-label="Secciones de administración">
        {tabs.map((item) => (
          <button
            type="button"
            key={item.id}
            aria-current={tab === item.id ? "page" : undefined}
            onClick={() => setTab(item.id)}
          >
            {item.label}
          </button>
        ))}
      </nav>
      {error ? (
        <div className="auth-error" role="alert">
          {error}
        </div>
      ) : null}
      {notice ? (
        <p className="success-notice" role="status">
          {notice}
        </p>
      ) : null}
      {loading ? (
        <div
          className="skeleton skeleton-card"
          aria-label="Cargando administración"
        />
      ) : (
        <>
          {tab === "resumen" ? (
            <>
              <div className="stats-grid focused-stats">
                <div className="stat-card">
                  <span>Pagos aprobados</span>
                  <strong>{formatPrice(approvedRevenue(purchases))}</strong>
                  <small>Compras confirmadas · total histórico</small>
                </div>
                <div className="stat-card">
                  <span>Eventos en venta</span>
                  <strong>{stats.eventos_publicados ?? "—"}</strong>
                  <small>De {stats.eventos ?? "—"} eventos creados</small>
                </div>
                <div className="stat-card">
                  <span>Entradas emitidas</span>
                  <strong>{stats.entradas ?? "—"}</strong>
                  <small>{purchases.length} compras registradas</small>
                </div>
                <div className="stat-card">
                  <span>Usuarios</span>
                  <strong>{users.length}</strong>
                  <small>
                    {
                      users.filter((user) => user.rol_usuario === "ORGANIZADOR")
                        .length
                    }{" "}
                    organizadores
                  </small>
                </div>
              </div>
              <div className="overview-grid">
                <section className="dashboard-panel">
                  <div className="panel-heading">
                    <h2>Últimas compras</h2>
                    <button
                      type="button"
                      className="text-link"
                      onClick={() => setTab("ventas")}
                    >
                      Ver ventas ↗
                    </button>
                  </div>
                  <div className="summary-list">
                    {latest.map((order) => (
                      <div key={order.id}>
                        <span>
                          <strong>{order.evento_titulo}</strong>
                          <small>
                            {formatDateTime(order.fecha_compra)} ·{" "}
                            {order.cantidad} entrada
                            {order.cantidad === 1 ? "" : "s"}
                          </small>
                        </span>
                        <strong>{formatPrice(order.total)}</strong>
                      </div>
                    ))}
                    {!latest.length ? (
                      <p className="empty-inline">
                        Las compras van a aparecer acá.
                      </p>
                    ) : null}
                  </div>
                </section>
                <section className="dashboard-panel attention-panel">
                  <p className="section-eyebrow">Para revisar</p>
                  <h2>
                    {needsReview.length + pendingReceipts.length
                      ? "Hay tareas pendientes"
                      : "Sin alertas registradas"}
                  </h2>
                  <button
                    className="attention-link"
                    type="button"
                    onClick={() => {
                      setAuditFilters({
                        search: "",
                        from: "",
                        to: "",
                        sort: "desc",
                        status: "INCIDENCIAS",
                      });
                      setAuditPage(1);
                      setTab("auditoria");
                    }}
                  >
                    <span>
                      Operaciones bloqueadas o rechazadas
                      <small>Entre las últimas 50 actividades</small>
                    </span>
                    <strong>{needsReview.length} ↗</strong>
                  </button>
                  <button
                    className="attention-link"
                    type="button"
                    onClick={() => setTab("ventas")}
                  >
                    <span>
                      Comprobantes pendientes
                      <small>Estado de envío registrado en demo</small>
                    </span>
                    <strong>{pendingReceipts.length} ↗</strong>
                  </button>
                  <p className="quiet-note">
                    El envío por correo todavía es simulado. Los estados de esta
                    demo no confirman una entrega en Gmail.
                  </p>
                </section>
              </div>
              <div className="workspace-shortcuts">
                <button type="button" onClick={() => setTab("usuarios")}>
                  <span>01</span>
                  <strong>Gestionar usuarios</strong>
                  <small>Permisos de organizador ↗</small>
                </button>
                <button type="button" onClick={() => setTab("catalogos")}>
                  <span>02</span>
                  <strong>Consultar catálogos</strong>
                  <small>
                    {categories.length} categorías · {locations.length} recintos
                    ↗
                  </small>
                </button>
                <button type="button" onClick={() => setTab("auditoria")}>
                  <span>03</span>
                  <strong>Revisar actividad</strong>
                  <small>Trazabilidad de operaciones ↗</small>
                </button>
              </div>
            </>
          ) : null}
          {tab === "usuarios" ? (
            <section className="dashboard-panel">
              <div className="panel-heading">
                <div>
                  <h2>Personas y permisos</h2>
                  <p>
                    Revisá los permisos de cada cuenta y habilitá organizadores
                    cuando corresponda.
                  </p>
                </div>
                <span className="count-label">
                  {filteredUsers.length} usuarios
                </span>
              </div>
              <div className="record-filters">
                <label className="filter-search">
                  Buscar usuario
                  <input
                    type="search"
                    placeholder="Nombre, email o DNI"
                    value={userSearch}
                    onChange={(e) => {
                      setUserSearch(e.target.value);
                      setUserPage(1);
                    }}
                  />
                </label>
                <label>
                  Rol
                  <select
                    value={userRole}
                    onChange={(e) => {
                      setUserRole(e.target.value);
                      setUserPage(1);
                    }}
                  >
                    <option value="">Todos los roles</option>
                    <option>CLIENTE</option>
                    <option>ORGANIZADOR</option>
                    <option>ADMIN</option>
                  </select>
                </label>
              </div>
              <div className="user-list">
                {filteredUsers
                  .slice((visibleUserPage - 1) * 8, visibleUserPage * 8)
                  .map((user) => (
                    <div className="user-row" key={user.id_usuario}>
                      <span className="user-avatar" aria-hidden="true">
                        {user.nombre_usuario?.[0]}
                      </span>
                      <span className="record-main">
                        <strong>
                          {user.nombre_usuario} {user.apellido_usuario}
                        </strong>
                        <small>{user.email_usuario}</small>
                      </span>
                      <span className="user-verification">
                        {user.identidad_verificada ||
                        user.estado_verificacion === "VERIFICADA"
                          ? "Identidad verificada"
                          : "Identidad pendiente"}
                        <small>{user.estado_usuario?.toLowerCase()}</small>
                      </span>
                      <StatusBadge status={user.rol_usuario} />
                      {user.rol_usuario !== "ADMIN" ? (
                        <button
                          type="button"
                          className="btn-copy"
                          disabled={roleUpdatingId !== null}
                          onClick={() => handleToggleOrganizer(user)}
                        >
                          {roleUpdatingId === user.id_usuario
                            ? "Guardando…"
                            : user.rol_usuario === "ORGANIZADOR"
                              ? "Pasar a cliente"
                              : "Habilitar organizador"}
                        </button>
                      ) : (
                        <span className="quiet-note">
                          Cuenta administradora
                        </span>
                      )}
                    </div>
                  ))}
              </div>
              {!filteredUsers.length ? (
                <p className="empty-state">No se encontraron usuarios.</p>
              ) : null}
              <Pagination
                count={filteredUsers.length}
                page={visibleUserPage}
                onChange={setUserPage}
              />
            </section>
          ) : null}
          {tab === "ventas" ? (
            <section className="dashboard-panel">
              <div className="panel-heading">
                <div>
                  <h2>Ventas y comprobantes</h2>
                  <p>
                    Abrí una compra para ver asistentes y detalles de la
                    transacción.
                  </p>
                </div>
              </div>
              <PurchaseList orders={purchases} technical />
            </section>
          ) : null}
          {tab === "catalogos" ? (
            <CatalogManager
              categories={categories}
              locations={locations}
              onUpdated={loadAll}
            />
          ) : null}
          {tab === "auditoria" ? (
            <section className="dashboard-panel">
              <div className="panel-heading">
                <div>
                  <h2>Actividad del sistema</h2>
                  <p>
                    Se muestran las últimas 50 operaciones que devuelve la API.
                    Las fechas filtran esta selección.
                  </p>
                </div>
              </div>
              <RecordFilters
                value={auditFilters}
                onChange={(next) => {
                  setAuditFilters(next);
                  setAuditPage(1);
                }}
                dateLabel="Actividad"
                placeholder="Acción o usuario"
              >
                <label>
                  Resultado
                  <select
                    value={auditFilters.status}
                    onChange={(e) => {
                      setAuditFilters({
                        ...auditFilters,
                        status: e.target.value,
                      });
                      setAuditPage(1);
                    }}
                  >
                    <option value="">Todos</option>
                    <option value="INCIDENCIAS">Solo incidencias</option>
                    <option>EXITOSO</option>
                    <option>BLOQUEADO</option>
                    <option>RECHAZADO</option>
                  </select>
                </label>
              </RecordFilters>
              <p className="results-count" aria-live="polite">
                {filteredLogs.length} actividades
              </p>
              <div className="activity-list">
                {filteredLogs
                  .slice((visibleAuditPage - 1) * 8, visibleAuditPage * 8)
                  .map((log) => (
                    <details className="activity-row" key={log.id_auditoria}>
                      <summary>
                        <span className="activity-dot" />
                        <span className="record-main">
                          <strong>
                            {log.accion_auditoria
                              ?.replaceAll("_", " ")
                              .toLowerCase()}
                          </strong>
                          <small>{log.email_usuario || "Sin usuario"}</small>
                        </span>
                        <time>{formatDateTime(log.fecha_auditoria)}</time>
                        <StatusBadge status={log.resultado_auditoria} />
                        <span className="disclosure-mark" aria-hidden="true">
                          +
                        </span>
                      </summary>
                      <div className="purchase-detail">
                        <p>{log.detalle_tracking}</p>
                        <p className="quiet-note">
                          {log.entidad_afectada} #
                          {log.id_registro_afectado ?? "—"} · IP registrada:{" "}
                          {log.ip_origen || "Sin datos"}
                        </p>
                      </div>
                    </details>
                  ))}
              </div>
              {!filteredLogs.length ? (
                <p className="empty-state">
                  No hay actividad para estos filtros.
                </p>
              ) : null}
              <Pagination
                count={filteredLogs.length}
                page={visibleAuditPage}
                onChange={setAuditPage}
              />
            </section>
          ) : null}
        </>
      )}
    </section>
  );
}
export default AdminDashboard;
