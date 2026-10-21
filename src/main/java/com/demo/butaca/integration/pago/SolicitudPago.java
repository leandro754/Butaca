package com.demo.butaca.integration.pago;

import java.math.BigDecimal;

/**
 * Datos mínimos que se envían a una pasarela. Nunca incluye PAN, CVV ni
 * credenciales de la tarjeta: solo un token opaco y los últimos cuatro dígitos.
 */
public record SolicitudPago(
        Long compraId,
        Long usuarioId,
        BigDecimal monto,
        String moneda,
        String pasarela,
        String medioPago,
        String ultimosCuatroDigitos,
        String tokenPago,
        String claveIdempotencia
) {
}
