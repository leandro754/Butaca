import { request } from "./apiClient";
import { getEventById } from "./eventService";

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
      entradas: [],
    };
  }

  const storedUser = getStoredUser();
  const usuarioId =
    orderData.usuarioId || storedUser?.id_usuario || storedUser?.id;
  const payload = {
    evento_id: orderData.eventoId,
    tipo_entrada_id: /^\d+$/.test(String(orderData.tipoEntrada))
      ? Number(orderData.tipoEntrada)
      : undefined,
    usuario_id: usuarioId,
    email_usuario:
      orderData.comprador?.email ||
      storedUser?.email_usuario ||
      storedUser?.email ||
      "",
    nombre_usuario:
      storedUser?.nombre_usuario ||
      storedUser?.nombre ||
      orderData.comprador?.nombre ||
      "Cliente",
    apellido_usuario:
      storedUser?.apellido_usuario || storedUser?.apellido || "Butaca",
    dni_usuario:
      orderData.comprador?.documento ||
      storedUser?.dni_usuario ||
      storedUser?.dni ||
      "",
    cantidad: orderData.cantidad || 1,
    pasarela_pago: orderData.pasarelaPago || "MERCADOPAGO",
    medio_pago: orderData.medioPago || "TARJETA_CREDITO",
    ultimos_cuatro_digitos: orderData.ultimosCuatroDigitos || "4242",
    tipo_comprobante: orderData.tipoComprobante || "RECIBO_DIGITAL",
    asistentes: orderData.asistentes || [],
  };

  const response = await request("/compras", {
    method: "POST",
    body: JSON.stringify(payload),
  });

  if (
    storedUser &&
    response.cliente_id &&
    response.cliente_id !== storedUser.id
  ) {
    const updatedUser = {
      ...storedUser,
      id: response.cliente_id,
      id_usuario: response.cliente_id,
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
    raw: response,
  };
}

export async function getUserOrders(userId) {
  const orders = await request(`/compras/usuario/${userId}`);
  const eventIds = [
    ...new Set(
      orders
        .flatMap((order) =>
          (order.entradas || []).map((ticket) => ticket.evento_id),
        )
        .filter(Boolean),
    ),
  ];
  const events = new Map(
    await Promise.all(
      eventIds.map(async (id) => {
        try {
          return [id, await getEventById(id)];
        } catch {
          return [id, null];
        }
      }),
    ),
  );
  return orders.map((order) => ({
    ...order,
    evento: events.get(order.evento_id) || null,
    entradas: (order.entradas || []).map((ticket) => ({
      ...ticket,
      evento: events.get(ticket.evento_id) || null,
    })),
  }));
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
