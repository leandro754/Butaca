import { request } from "./apiClient";

const fallbackImage =
  "https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&w=1200&q=80";

const categoryImages = {
  conciertos:
    "https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&w=1200&q=80",
  teatro:
    "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=1200&q=80",
  deportes:
    "https://images.unsplash.com/photo-1504450758481-7338eba7524a?auto=format&fit=crop&w=1200&q=80",
  festivales:
    "https://images.unsplash.com/photo-1511192336575-5a79af67a629?auto=format&fit=crop&w=1200&q=80",
};

function resolveEventImage(evento, categoria) {
  const custom = evento.imagen_url || evento.imagenUrl;
  if (custom && String(custom).trim().length > 5) {
    return custom;
  }
  const titulo = String(
    evento.titulo_evento || evento.tituloEvento || "",
  ).toLowerCase();
  if (titulo.includes("gaming") || titulo.includes("expo")) {
    return "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80";
  }
  if (titulo.includes("jazz")) {
    return "https://images.unsplash.com/photo-1511192336575-5a79af67a629?auto=format&fit=crop&w=1200&q=80";
  }
  if (titulo.includes("stand up") || titulo.includes("comedia")) {
    return "https://images.unsplash.com/photo-1585699324551-f6c309eedeca?auto=format&fit=crop&w=1200&q=80";
  }
  if (titulo.includes("electronica") || titulo.includes("techno")) {
    return "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80";
  }
  const catKey = String(categoria || "").toLowerCase();
  return categoryImages[catKey] || fallbackImage;
}

function toNumber(value, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function mapSpringEvent(evento) {
  const id = evento.id_evento ?? evento.idEvento ?? evento.id;
  const precio = toNumber(
    evento.precio_entrada_evento ?? evento.precioEntradaEvento,
  );
  const cupos = toNumber(
    evento.cupos_disponibles ??
      evento.cupos_disponible_evento ??
      evento.cuposDisponibleEvento,
  );
  const categoria =
    evento.categoria_nombre || evento.categoriaNombre || "General";
  const ubicacionNombre =
    evento.ubicacion_nombre ||
    evento.ubicacion_evento ||
    evento.ubicacionEvento ||
    "Lugar sin definir";
  const ubicacionDireccion =
    evento.ubicacion_direccion ||
    evento.detalles_ubicacion_evento ||
    evento.detallesUbicacionEvento ||
    "";

  return {
    raw: evento,
    id,
    slug: `evento-${id}`,
    nombre: evento.titulo_evento || evento.tituloEvento,
    artista: evento.titulo_evento || evento.tituloEvento,
    categoria,
    categoriaId: evento.categoria_id ?? evento.categoriaId,
    ciudad: ubicacionDireccion || ubicacionNombre,
    recinto: ubicacionNombre,
    ubicacionId: evento.ubicacion_id ?? evento.ubicacionId,
    ubicacionDireccion,
    fecha: evento.fecha_evento || evento.fechaEvento,
    hora: String(evento.hora_evento || evento.horaEvento || "20:00").slice(
      0,
      5,
    ),
    precioMinimo: precio,
    precioMaximo: precio,
    disponibilidad: cupos,
    cuposTotales: toNumber(
      evento.cupos_totales_evento ?? evento.cuposTotalesEvento,
    ),
    estado: evento.estado_publicacion_evento || evento.estadoPublicacionEvento,
    version: evento.version,
    organizadorId: evento.organizador_id_evento,
    entradasVendidas: toNumber(evento.entradas_vendidas),
    totalVendido: toNumber(evento.total_vendido),
    ocupacion: toNumber(evento.porcentaje_ocupacion),
    imagen: resolveEventImage(evento, categoria),
    descripcion:
      evento.descripcion_evento ||
      evento.descripcionEvento ||
      "Evento disponible en Butaca.",
    etiquetas: [
      categoria,
      evento.estado_publicacion_evento ||
        evento.estadoPublicacionEvento ||
        "Disponible",
    ],
    tiposEntrada: evento.tipos_entrada
      ? evento.tipos_entrada
          .filter((tipo) => tipo.activo)
          .map((tipo) => ({
            id: tipo.id_tipo_entrada,
            nombre: tipo.nombre,
            precio: toNumber(tipo.precio),
          }))
      : [{ id: "general", nombre: "Entrada general", precio }],
  };
}

export async function getAllEvents(params = {}) {
  const query = params.categoriaId ? `?categoriaId=${params.categoriaId}` : "";
  const eventos = await request(`/eventos${query}`);
  return eventos.map(mapSpringEvent);
}

export async function getEventById(id) {
  const evento = await request(`/eventos/${id}`);
  return mapSpringEvent(evento);
}

export async function getCategories() {
  const categorias = await request("/admin/categorias");
  return categorias.map((categoria) => ({
    id: categoria.id_categoria,
    nombre: categoria.nombre,
    descripcion: categoria.descripcion,
  }));
}

export async function getLocations() {
  const ubicaciones = await request("/admin/ubicaciones");
  return ubicaciones.map((ubicacion) => ({
    id: ubicacion.id_ubicacion,
    nombre: ubicacion.nombre_lugar,
    direccion: ubicacion.direccion,
    capacidadMaxima: ubicacion.capacidad_maxima,
    activo: ubicacion.activo,
  }));
}

export async function createCategory(data) {
  return request("/admin/categorias", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function createLocation(data) {
  return request("/admin/ubicaciones", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function getOrganizerEvents(organizerId) {
  const eventos = await request(`/eventos/organizador/${organizerId}`);
  return eventos.map(mapSpringEvent);
}

export async function createOrganizerEvent(eventData) {
  return request("/eventos", {
    method: "POST",
    body: JSON.stringify(eventData),
  });
}

export async function publishEvent(eventId) {
  return request(`/eventos/${eventId}/publicar`, { method: "PUT" });
}

export async function cancelEvent(eventId, confirmation) {
  return request(`/eventos/${eventId}/cancelar`, {
    method: "PUT",
    body: JSON.stringify(confirmation),
  });
}

export async function updateOrganizerEvent(eventId, data) {
  return request(`/eventos/${eventId}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

export async function getAdminStats() {
  return request("/admin/dashboard");
}

export async function getAdminAuditLogs() {
  return request("/admin/auditoria");
}

export async function getAdminUsers() {
  return request("/admin/usuarios");
}

export async function updateUserRole(userId, rol) {
  return request(
    `/admin/usuarios/${userId}/rol?rol=${encodeURIComponent(rol)}`,
    {
      method: "PUT",
    },
  );
}

export async function getAdminPurchases() {
  return request("/admin/compras");
}

export async function validateTicketByCode(codigo) {
  return request(`/entradas/validar/${encodeURIComponent(codigo.trim())}`);
}

export async function useTicketById(entradaId) {
  return request(`/entradas/${entradaId}/usar`, {
    method: "PUT",
  });
}
