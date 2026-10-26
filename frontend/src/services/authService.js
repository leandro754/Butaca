import { request } from "./apiClient";

export async function loginUser(credentials) {
  const payload = {
    email_usuario: credentials.email_usuario || credentials.email,
    password_usuario: credentials.password_usuario || credentials.password
  };

  const response = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  return normalizeAuthUser(response);
}

export async function registerUser(userData) {
  const payload = {
    nombre_usuario: userData.nombre_usuario || userData.nombre,
    apellido_usuario: userData.apellido_usuario || userData.apellido,
    dni_usuario: userData.dni_usuario || userData.dni || "",
    email_usuario: userData.email_usuario || userData.email,
    password_usuario: userData.password_usuario || userData.password,
    rol_usuario: normalizeRole(userData.rol_usuario || userData.rol || "CLIENTE"),
    telefono: userData.telefono || ""
  };

  const response = await request("/auth/register", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  return normalizeAuthUser(response);
}

export async function verifyUserIdentity({
  usuarioId,
  emailUsuario,
  nombreUsuario,
  apellidoUsuario,
  dniUsuario,
  numeroTramite,
  metodoVerificacion,
  fotoDni,
  selfie
}) {
  if (!fotoDni || !selfie) {
    throw new Error("Necesitás adjuntar la foto del DNI y la selfie para verificar tu identidad");
  }

  const formData = new FormData();
  formData.append("usuario_id", String(usuarioId));
  formData.append("email_usuario", emailUsuario || "");
  formData.append("nombre_usuario", nombreUsuario || "");
  formData.append("apellido_usuario", apellidoUsuario || "");
  formData.append("dni_usuario", dniUsuario);
  formData.append("numero_tramite", numeroTramite || "");
  formData.append("metodo_verificacion", metodoVerificacion || "BIOMETRIA_RENAPER");
  formData.append("foto_dni", fotoDni);
  formData.append("selfie", selfie);

  return request("/verificaciones", {
    method: "POST",
    body: formData
  });
}

export async function getUserVerificationStatus(usuarioId) {
  return request(`/verificaciones/usuario/${usuarioId}`);
}

export function normalizeRole(role) {
  const normalized = String(role || "CLIENTE").trim().toUpperCase();
  return normalized === "CLIENT" ? "CLIENTE" : normalized;
}

export function normalizeAuthUser(user) {
  const rol = normalizeRole(user.rol_usuario || user.rolUsuario || user.rol);
  const dni = user.dni_usuario ?? user.dniUsuario ?? user.dni ?? "";
  const identidadVerificada = Boolean(user.identidad_verificada ?? user.identidadVerificada ?? false);
  const estadoVerificacion = user.estado_verificacion ?? user.estadoVerificacion ?? (identidadVerificada ? "VERIFICADA" : "PENDIENTE");

  return {
    ...user,
    id: user.id_usuario ?? user.idUsuario ?? user.id,
    id_usuario: user.id_usuario ?? user.idUsuario ?? user.id,
    nombre: user.nombre_usuario ?? user.nombreUsuario ?? user.nombre,
    apellido: user.apellido_usuario ?? user.apellidoUsuario ?? user.apellido,
    dni,
    dni_usuario: dni,
    email: user.email_usuario ?? user.emailUsuario ?? user.email,
    rol,
    rol_usuario: rol,
    identidad_verificada: identidadVerificada,
    estado_verificacion: estadoVerificacion
  };
}
