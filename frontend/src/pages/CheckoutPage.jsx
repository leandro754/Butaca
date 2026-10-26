import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Button from "../components/ui/Button";
import OrderSummary from "../components/checkout/OrderSummary";
import { getUserVerificationStatus, verifyUserIdentity } from "../services/authService";
import { createOrder } from "../services/orderService";
import { saveAuthUser } from "../utils/authStorage";
import { clearSelectedOrder, getSelectedOrder, saveSelectedOrder } from "../utils/storage";

function formatCardNumber(value) {
  const digits = value.replace(/\D/g, "").slice(0, 16);
  return digits.replace(/(\d{4})(?=\d)/g, "$1 ");
}

function formatExpiry(value) {
  const digits = value.replace(/\D/g, "").slice(0, 4);
  if (digits.length >= 3) {
    return `${digits.slice(0, 2)}/${digits.slice(2, 4)}`;
  }
  return digits;
}

function CheckoutPage({ currentUser }) {
  const navigate = useNavigate();
  const order = getSelectedOrder();
  const quantity = Math.max(1, Number(order?.quantity || 1));

  const buyerFullName = currentUser ? `${currentUser.nombre} ${currentUser.apellido || ""}`.trim() : "";
  const initialDni = currentUser?.dni || currentUser?.dni_usuario || "";

  const [loading, setLoading] = useState(false);
  const [paymentStepText, setPaymentStepText] = useState("");
  const [kycLoading, setKycLoading] = useState(false);
  const [kycStepText, setKycStepText] = useState("");
  const [kycData, setKycData] = useState({
    verificado: Boolean(currentUser?.identidad_verificada),
    estado: currentUser?.estado_verificacion || "PENDIENTE",
    dni: initialDni,
    numeroTramite: "00548291034",
    metodo: "BIOMETRIA_RENAPER",
    fotoDni: null,
    selfie: null,
    observacion: ""
  });

  const [paymentMode, setPaymentMode] = useState("TARJETA"); // TARJETA | BILLETERA
  const [cardForm, setCardForm] = useState({
    numero: "4509 5612 8901 4242",
    titular: buyerFullName.toUpperCase() || "TITULAR DE LA TARJETA",
    vencimiento: "09/28",
    cvv: "842",
    cuotas: "1",
    tipoTarjeta: "TARJETA_CREDITO",
    procesador: "MERCADOPAGO",
    billetera: "MERCADOPAGO"
  });

  const [asistentes, setAsistentes] = useState(() =>
    Array.from({ length: quantity }, (_, idx) => ({
      nombre_asistente: idx === 0 ? buyerFullName : "",
      dni_asistente: idx === 0 ? initialDni : ""
    }))
  );

  useEffect(() => {
    if (!currentUser?.id) return;
    getUserVerificationStatus(currentUser.id)
      .then((res) => {
        const isVerified = res.estado_verificacion === "VERIFICADA";
        const dniVerificado = res.dni_usuario || initialDni;
        setKycData((prev) => ({
          ...prev,
          verificado: isVerified,
          estado: res.estado_verificacion || "PENDIENTE",
          dni: dniVerificado,
          metodo: res.metodo_verificacion || prev.metodo
        }));
        if (dniVerificado) {
          setAsistentes((prev) =>
            prev.map((item, idx) =>
              idx === 0 && !item.dni_asistente ? { ...item, dni_asistente: dniVerificado } : item
            )
          );
        }
      })
      .catch(() => {});
  }, [currentUser?.id, initialDni]);

  if (!order) {
    return <section className="container page-block">No hay una compra seleccionada todavía.</section>;
  }

  if (!currentUser) {
    return (
      <section className="container page-block">
        <div className="empty-state">
          <p>Necesitás iniciar sesión para finalizar tu compra.</p>
          <Link to="/login" className="pill-link">Ingresar</Link>
        </div>
      </section>
    );
  }

  async function handleVerifyIdentity() {
    const cleanDni = (kycData.dni || "").replace(/\D/g, "");
    if (cleanDni.length < 7 || cleanDni.length > 8) {
      window.alert("Ingresá un número de DNI válido (7 u 8 dígitos).");
      return;
    }
    if (!kycData.numeroTramite || kycData.numeroTramite.replace(/\D/g, "").length < 8) {
      window.alert("Ingresá los dígitos del N° de trámite que figura al frente de tu DNI.");
      return;
    }
    if (!validarArchivo(kycData.fotoDni, "la foto del frente del DNI")) {
      return;
    }
    if (!validarArchivo(kycData.selfie, "la selfie de prueba de vida")) {
      return;
    }

    setKycLoading(true);
    try {
      setKycStepText("Validando documento y selfie con el proveedor simulado...");
      await new Promise((r) => setTimeout(r, 500));

      const verified = await verifyUserIdentity({
        usuarioId: currentUser.id,
        emailUsuario: currentUser.email,
        nombreUsuario: currentUser.nombre,
        apellidoUsuario: currentUser.apellido,
        dniUsuario: cleanDni,
        numeroTramite: kycData.numeroTramite.replace(/\D/g, ""),
        metodoVerificacion: kycData.metodo,
        fotoDni: kycData.fotoDni,
        selfie: kycData.selfie
      });

      const isVerified = verified.estado_verificacion === "VERIFICADA";
      const resolvedUserId = verified.usuario_id || currentUser.id;
      setKycData((prev) => ({
        ...prev,
        verificado: isVerified,
        estado: verified.estado_verificacion || "RECHAZADA",
        dni: verified.dni_usuario || cleanDni,
        observacion: verified.observacion_verificacion || ""
      }));

      if (!isVerified) {
        window.alert(verified.observacion_verificacion || "No pudimos verificar tu identidad. Revisá los datos y las imágenes.");
        return;
      }

      saveAuthUser({
        ...currentUser,
        id: resolvedUserId,
        id_usuario: resolvedUserId,
        dni: verified.dni_usuario,
        dni_usuario: verified.dni_usuario,
        identidad_verificada: true,
        estado_verificacion: "VERIFICADA"
      });
      setAsistentes((prev) =>
        prev.map((item, idx) =>
          idx === 0
            ? { ...item, dni_asistente: verified.dni_usuario, nombre_asistente: item.nombre_asistente || buyerFullName }
            : item
        )
      );
    } catch (error) {
      window.alert(error.message);
    } finally {
      setKycLoading(false);
      setKycStepText("");
    }
  }

  function validarArchivo(archivo, etiqueta) {
    if (!archivo) {
      window.alert(`Seleccioná ${etiqueta}.`);
      return false;
    }
    if (!archivo.type?.startsWith("image/")) {
      window.alert(`${etiqueta} debe ser una imagen (JPG, PNG o WebP).`);
      return false;
    }
    if (archivo.size > 5 * 1024 * 1024) {
      window.alert(`${etiqueta} no puede superar los 5 MB.`);
      return false;
    }
    return true;
  }

  function handleAsistenteChange(index, field, value) {
    setAsistentes((prev) =>
      prev.map((item, idx) => (idx === index ? { ...item, [field]: value } : item))
    );
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (!kycData.verificado) {
      window.alert("Por tu seguridad, primero validá tu identidad antes de continuar con el pago.");
      return;
    }

    let pasarelaPago = "MERCADOPAGO";
    let medioPago = "TARJETA_CREDITO";
    let ultimosCuatroDigitos = "4242";

    if (paymentMode === "TARJETA") {
      const digits = cardForm.numero.replace(/\D/g, "");
      if (digits.length < 13) {
        window.alert("Ingresá un número de tarjeta válido.");
        return;
      }
      if (!cardForm.vencimiento || cardForm.vencimiento.length < 5) {
        window.alert("Ingresá la fecha de vencimiento (MM/AA).");
        return;
      }
      if (!cardForm.cvv || cardForm.cvv.replace(/\D/g, "").length < 3) {
        window.alert("Ingresá el código de seguridad (CVV).");
        return;
      }
      pasarelaPago = cardForm.procesador;
      medioPago = cardForm.tipoTarjeta;
      ultimosCuatroDigitos = digits.slice(-4);
    } else {
      pasarelaPago = cardForm.billetera;
      medioPago = cardForm.billetera === "MODO" ? "TRANSFERENCIA_CVU" : "DINERO_CUENTA";
      ultimosCuatroDigitos = (kycData.dni || "0000").slice(-4);
    }

    setLoading(true);
    try {
      setPaymentStepText("Conectando de forma segura con la pasarela de pago...");
      await new Promise((r) => setTimeout(r, 550));
      setPaymentStepText("Autorizando operación y generando tus entradas...");
      await new Promise((r) => setTimeout(r, 550));

      const result = await createOrder({
        comprador: {
          nombre: buyerFullName,
          email: currentUser.email,
          documento: kycData.dni
        },
        eventoId: order.event.id,
        usuarioId: currentUser.id,
        tipoEntrada: order.ticketType.id,
        cantidad: quantity,
        pasarelaPago,
        medioPago,
        ultimosCuatroDigitos,
        tipoComprobante: "RECIBO_DIGITAL",
        asistentes
      });

      saveSelectedOrder({
        ...order,
        confirmation: result
      });
      navigate("/confirmacion");
    } catch (error) {
      window.alert(error.message);
    } finally {
      setLoading(false);
      setPaymentStepText("");
    }
  }

  function handleCancel() {
    clearSelectedOrder();
    navigate("/");
  }

  const maskedCardPreview = cardForm.numero || "•••• •••• •••• 4242";

  return (
    <section className="container page-block">
      <div className="checkout-layout">
        <form className="checkout-form" onSubmit={handleSubmit}>
          <p className="section-eyebrow">Finalizar compra</p>
          <h1>Tus entradas y medio de pago</h1>

          {/* 1. Validación de Identidad del Comprador */}
          <div
            style={{
              background: kycData.verificado ? "rgba(34, 197, 94, 0.07)" : "rgba(245, 158, 11, 0.08)",
              border: `1px solid ${kycData.verificado ? "rgba(34, 197, 94, 0.3)" : "rgba(245, 158, 11, 0.35)"}`,
              borderRadius: "var(--r-md)",
              padding: "18px 20px",
              marginBottom: 22
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 8 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                <span style={{ fontSize: "1.2rem" }}>{kycData.verificado ? "✓" : "🪪"}</span>
                <strong>Identidad del titular de la cuenta</strong>
              </div>
              <span className={`badge-status ${kycData.verificado ? "badge-success" : "badge-warning"}`}>
                {kycData.verificado ? "Identidad verificada" : "Verificación requerida"}
              </span>
            </div>

            {kycData.verificado ? (
              <p style={{ margin: "8px 0 0", fontSize: "0.88rem", color: "var(--text-2)" }}>
                Cuenta validada a nombre de <strong>{buyerFullName}</strong> · DNI <strong>{kycData.dni}</strong>
              </p>
            ) : (
              <div style={{ marginTop: 14 }}>
                <p style={{ margin: "0 0 14px", fontSize: "0.86rem", color: "var(--text-2)", lineHeight: 1.5 }}>
                  Para evitar la reventa y proteger tu compra, necesitamos validar tu identidad por única vez con tu DNI y una selfie. En esta demo se usa un proveedor simulado; no se envía nada a RENAPER real.
                </p>

                <div className="form-grid two-columns" style={{ marginBottom: 14 }}>
                  <label>
                    Número de DNI
                    <input
                      value={kycData.dni}
                      onChange={(e) => setKycData((prev) => ({ ...prev, dni: e.target.value }))}
                      placeholder="Ej: 42850123"
                      required
                    />
                  </label>
                  <label>
                    N° de Trámite del DNI (11 dígitos al frente)
                    <input
                      value={kycData.numeroTramite}
                      onChange={(e) => setKycData((prev) => ({ ...prev, numeroTramite: e.target.value }))}
                      placeholder="Ej: 00548291034"
                      required
                    />
                  </label>
                </div>

                <div className="form-grid two-columns" style={{ marginBottom: 14 }}>
                  <label>
                    Foto del frente del DNI
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(e) => setKycData((prev) => ({ ...prev, fotoDni: e.target.files?.[0] || null }))}
                    />
                    <span style={{ fontSize: "0.75rem", color: "var(--text-3)" }}>
                      {kycData.fotoDni?.name || "JPG, PNG o WebP · máximo 5 MB"}
                    </span>
                  </label>
                  <label>
                    Selfie de prueba de vida
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(e) => setKycData((prev) => ({ ...prev, selfie: e.target.files?.[0] || null }))}
                    />
                    <span style={{ fontSize: "0.75rem", color: "var(--text-3)" }}>
                      {kycData.selfie?.name || "Selfie mirando de frente · máximo 5 MB"}
                    </span>
                  </label>
                </div>

                {/* Simulador realista de captura biométrica */}
                <div
                  style={{
                    background: "rgba(0,0,0,0.22)",
                    border: "1px dashed var(--border)",
                    borderRadius: "var(--r-sm)",
                    padding: "14px 16px",
                    marginBottom: 14,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    flexWrap: "wrap",
                    gap: 12
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <div
                      style={{
                        width: 44,
                        height: 44,
                        borderRadius: "50%",
                        background: "rgba(78, 205, 196, 0.14)",
                        border: "2px solid var(--accent)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: "1.2rem"
                      }}
                    >
                      📷
                    </div>
                    <div>
                      <strong style={{ display: "block", fontSize: "0.88rem" }}>
                        Verificación de identidad (simulada)
                      </strong>
                      <span style={{ fontSize: "0.8rem", color: "var(--text-2)" }}>
                        {kycStepText || "La foto del DNI y la selfie se procesan en memoria y no se almacenan"}
                      </span>
                    </div>
                  </div>

                  <Button type="button" onClick={handleVerifyIdentity} disabled={kycLoading}>
                    {kycLoading ? "Validando..." : "Validar identidad"}
                  </Button>
                </div>

                {kycData.observacion ? (
                  <p style={{ margin: 0, fontSize: "0.82rem", color: "var(--text-2)" }}>
                    {kycData.observacion}
                  </p>
                ) : null}
              </div>
            )}
          </div>

          {/* 2. Datos de los asistentes (Entradas Nominales) */}
          <div
            style={{
              background: "var(--surface)",
              border: "1px solid var(--border)",
              borderRadius: "var(--r-md)",
              padding: "18px 20px",
              marginBottom: 22
            }}
          >
            <strong style={{ display: "block", marginBottom: 4, fontSize: "1.02rem" }}>
              ¿Quiénes asisten al evento? ({quantity} {quantity === 1 ? "entrada" : "entradas"})
            </strong>
            <p style={{ margin: "0 0 16px", fontSize: "0.85rem", color: "var(--text-2)" }}>
              Ingresá el nombre y DNI de cada persona que ingresará al espectáculo. Estos datos figurarán en su entrada digital.
            </p>
            <div style={{ display: "grid", gap: 14 }}>
              {asistentes.map((asistente, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: "12px 14px",
                    background: "rgba(255,255,255,0.02)",
                    border: "1px solid var(--border)",
                    borderRadius: "var(--r-sm)"
                  }}
                >
                  <div style={{ fontSize: "0.78rem", fontWeight: 700, color: "var(--primary-2)", marginBottom: 8 }}>
                    ENTRADA #{idx + 1} {idx === 0 ? "· TITULAR" : ""}
                  </div>
                  <div className="form-grid two-columns">
                    <label>
                      Nombre y apellido
                      <input
                        value={asistente.nombre_asistente}
                        onChange={(e) => handleAsistenteChange(idx, "nombre_asistente", e.target.value)}
                        placeholder="Como figura en el DNI"
                        required
                      />
                    </label>
                    <label>
                      N° de DNI / Documento
                      <input
                        value={asistente.dni_asistente}
                        onChange={(e) => handleAsistenteChange(idx, "dni_asistente", e.target.value)}
                        placeholder="Sin puntos ni espacios"
                        required
                      />
                    </label>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* 3. Medio de Pago realista */}
          <div
            style={{
              background: "var(--surface)",
              border: "1px solid var(--border)",
              borderRadius: "var(--r-md)",
              padding: "18px 20px",
              marginBottom: 22
            }}
          >
            <strong style={{ display: "block", marginBottom: 12, fontSize: "1.02rem" }}>
              Elegí cómo pagar
            </strong>

            {/* Selector de pestañas de pago */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 18 }}>
              <button
                type="button"
                onClick={() => setPaymentMode("TARJETA")}
                style={{
                  padding: "12px 14px",
                  borderRadius: "var(--r-sm)",
                  border: paymentMode === "TARJETA" ? "1.5px solid var(--primary)" : "1px solid var(--border)",
                  background: paymentMode === "TARJETA" ? "rgba(225, 29, 72, 0.1)" : "rgba(255,255,255,0.02)",
                  color: "var(--text)",
                  cursor: "pointer",
                  textAlign: "left",
                  fontWeight: 600
                }}
              >
                💳 Tarjeta de Crédito o Débito
                <span style={{ display: "block", fontSize: "0.76rem", color: "var(--text-2)", fontWeight: 400, marginTop: 2 }}>
                  Visa, Mastercard, Cabal o Amex
                </span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentMode("BILLETERA")}
                style={{
                  padding: "12px 14px",
                  borderRadius: "var(--r-sm)",
                  border: paymentMode === "BILLETERA" ? "1.5px solid var(--primary)" : "1px solid var(--border)",
                  background: paymentMode === "BILLETERA" ? "rgba(225, 29, 72, 0.1)" : "rgba(255,255,255,0.02)",
                  color: "var(--text)",
                  cursor: "pointer",
                  textAlign: "left",
                  fontWeight: 600
                }}
              >
                📱 Mercado Pago / MODO
                <span style={{ display: "block", fontSize: "0.76rem", color: "var(--text-2)", fontWeight: 400, marginTop: 2 }}>
                  Dinero en cuenta o transferencia inmediata
                </span>
              </button>
            </div>

            {paymentMode === "TARJETA" ? (
              <div>
                {/* Vista previa visual de tarjeta */}
                <div
                  style={{
                    background: "linear-gradient(135deg, #1e293b 0%, #0f172a 60%, #331832 100%)",
                    border: "1px solid rgba(255,255,255,0.12)",
                    borderRadius: "14px",
                    padding: "18px 22px",
                    marginBottom: 16,
                    color: "#f8fafc",
                    boxShadow: "0 12px 28px rgba(0,0,0,0.35)"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 18 }}>
                    <span style={{ fontSize: "0.75rem", textTransform: "uppercase", letterSpacing: "0.08em", color: "#94a3b8" }}>
                      {cardForm.tipoTarjeta === "TARJETA_DEBITO" ? "Tarjeta de Débito" : "Tarjeta de Crédito"} · Procesado por {cardForm.procesador === "STRIPE" ? "Stripe" : "Mercado Pago"}
                    </span>
                    <strong style={{ fontSize: "0.95rem", color: "#38bdf8" }}>VISA</strong>
                  </div>
                  <div style={{ fontFamily: "monospace", fontSize: "1.22rem", letterSpacing: "0.14em", marginBottom: 14 }}>
                    {maskedCardPreview}
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem", color: "#cbd5e1" }}>
                    <span>{cardForm.titular || "NOMBRE DEL TITULAR"}</span>
                    <span>VENCE {cardForm.vencimiento || "MM/AA"}</span>
                  </div>
                </div>

                <div className="form-grid two-columns">
                  <label>
                    Tipo de tarjeta
                    <select
                      value={cardForm.tipoTarjeta}
                      onChange={(e) => setCardForm((prev) => ({ ...prev, tipoTarjeta: e.target.value }))}
                    >
                      <option value="TARJETA_CREDITO">Tarjeta de Crédito</option>
                      <option value="TARJETA_DEBITO">Tarjeta de Débito</option>
                    </select>
                  </label>

                  <label>
                    Procesador de pago
                    <select
                      value={cardForm.procesador}
                      onChange={(e) => setCardForm((prev) => ({ ...prev, procesador: e.target.value }))}
                    >
                      <option value="MERCADOPAGO">Mercado Pago</option>
                      <option value="STRIPE">Stripe</option>
                      <option value="MODO">MODO</option>
                    </select>
                  </label>

                  <label className="span-2">
                    Número de la tarjeta
                    <input
                      value={cardForm.numero}
                      onChange={(e) => setCardForm((prev) => ({ ...prev, numero: formatCardNumber(e.target.value) }))}
                      placeholder="4509 1234 5678 4242"
                      maxLength={19}
                      required
                    />
                  </label>

                  <label className="span-2">
                    Nombre y apellido como figura en la tarjeta
                    <input
                      value={cardForm.titular}
                      onChange={(e) => setCardForm((prev) => ({ ...prev, titular: e.target.value.toUpperCase() }))}
                      placeholder="JUAN PEREZ"
                      required
                    />
                  </label>

                  <label>
                    Vencimiento (MM/AA)
                    <input
                      value={cardForm.vencimiento}
                      onChange={(e) => setCardForm((prev) => ({ ...prev, vencimiento: formatExpiry(e.target.value) }))}
                      placeholder="09/28"
                      maxLength={5}
                      required
                    />
                  </label>

                  <label>
                    Código de seguridad (CVV)
                    <input
                      type="password"
                      value={cardForm.cvv}
                      onChange={(e) => setCardForm((prev) => ({ ...prev, cvv: e.target.value.replace(/\D/g, "").slice(0, 4) }))}
                      placeholder="•••"
                      maxLength={4}
                      required
                    />
                  </label>
                </div>

                <p style={{ margin: "12px 0 0", fontSize: "0.78rem", color: "var(--text-3)" }}>
                  🔒 Demo de pasarela: el número completo y el CVV no se envían al backend. Solo se usan en el navegador para la simulación y se conservan los últimos cuatro dígitos.
                </p>
              </div>
            ) : (
              <div
                style={{
                  background: "rgba(255,255,255,0.02)",
                  border: "1px solid var(--border)",
                  borderRadius: "var(--r-sm)",
                  padding: "16px"
                }}
              >
                <div className="form-grid">
                  <label>
                    Seleccioná tu billetera virtual
                    <select
                      value={cardForm.billetera}
                      onChange={(e) => setCardForm((prev) => ({ ...prev, billetera: e.target.value }))}
                    >
                      <option value="MERCADOPAGO">Mercado Pago (Dinero en cuenta)</option>
                      <option value="MODO">MODO (Transferencia bancaria inmediata)</option>
                    </select>
                  </label>
                </div>
                <p style={{ margin: "10px 0 0", fontSize: "0.84rem", color: "var(--text-2)" }}>
                  Al confirmar la compra, se debitará el importe directamente desde tu cuenta vinculada de{" "}
                  <strong>{cardForm.billetera === "MODO" ? "MODO" : "Mercado Pago"}</strong> y recibirás tus entradas al instante.
                </p>
              </div>
            )}
          </div>

          {paymentStepText ? (
            <div
              style={{
                background: "rgba(78, 205, 196, 0.1)",
                border: "1px solid rgba(78, 205, 196, 0.3)",
                borderRadius: "var(--r-sm)",
                padding: "12px 16px",
                marginBottom: 16,
                color: "var(--text)",
                fontSize: "0.9rem",
                textAlign: "center"
              }}
            >
              ⏳ {paymentStepText}
            </div>
          ) : null}

          <div className="checkout-actions">
            <Button type="button" variant="ghost" onClick={handleCancel}>
              Cancelar
            </Button>

            <Button type="submit" disabled={loading || !kycData.verificado}>
              {loading ? "Procesando pago..." : "Confirmar y pagar"}
            </Button>
          </div>
        </form>

        <OrderSummary event={order.event} ticketType={order.ticketType} quantity={order.quantity} />
      </div>
    </section>
  );
}

export default CheckoutPage;

