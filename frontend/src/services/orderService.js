import { request } from "./apiClient";

export const useMockOrders = false;

const AUTH_STORAGE_KEY = "butaca-auth-user";

function getStoredUser() {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  return raw ? JSON.parse(raw) : null;
}

export async function createOrder(orderData) {
  if (useMockOrders) {
    return {
      codigoReserva: `BTC-${Math.random().toString(36).slice(2, 8).toUpperCase()}`,
      total: orderData.total,
      comprador: orderData.comprador,
      entradas: []
    };
  }

  const storedUser = getStoredUser();
  const usuarioId = orderData.usuarioId || storedUser?.id_usuario || storedUser?.id;
  const payload = {
    evento_id: orderData.eventoId,
    usuario_id: usuarioId,
    email_usuario: orderData.comprador?.email || storedUser?.email_usuario || storedUser?.email || "",
    nombre_usuario: storedUser?.nombre_usuario || storedUser?.nombre || orderData.comprador?.nombre || "Cliente",
    apellido_usuario: storedUser?.apellido_usuario || storedUser?.apellido || "Butaca",
    dni_usuario: orderData.comprador?.documento || storedUser?.dni_usuario || storedUser?.dni || "",
    cantidad: orderData.cantidad || 1,
    pasarela_pago: orderData.pasarelaPago || "MERCADOPAGO",
    medio_pago: orderData.medioPago || "TARJETA_CREDITO",
    ultimos_cuatro_digitos: orderData.ultimosCuatroDigitos || "4242",
    tipo_comprobante: orderData.tipoComprobante || "RECIBO_DIGITAL",
    asistentes: orderData.asistentes || []
  };

  const response = await request("/compras", {
    method: "POST",
    body: JSON.stringify(payload)
  });

  if (storedUser && response.cliente_id && response.cliente_id !== storedUser.id) {
    const updatedUser = {
      ...storedUser,
      id: response.cliente_id,
      id_usuario: response.cliente_id
    };
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(updatedUser));
  }

  const entradas = response.entradas || [];
  return {
    idCompra: response.id,
    codigoReserva: entradas[0]?.codigo_unico_entrada || `COMPRA-${response.id}`,
    total: Number(response.total ?? orderData.total),
    comprador: orderData.comprador,
    pago: response.pago || null,
    comprobante: response.comprobante || null,
    entradas,
    raw: response
  };
}

export async function getUserOrders(userId) {
  return request(`/compras/usuario/${userId}`);
}

export async function verifyPurchaseCrossCheck(compraId) {
  return request(`/compras/${compraId}/verificar-cruce`);
}

export async function getOrganizerOrders(organizerId) {
  return request(`/compras/organizador/${organizerId}`);
}

export async function getUserNotifications(userId) {
  return request(`/notificaciones/usuario/${userId}`);
}

export async function markNotificationRead(notificationId) {
  return request(`/notificaciones/${notificationId}/leida`, { method: "PUT" });
}
