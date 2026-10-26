package com.demo.butaca.integration.pago;

import java.math.BigDecimal;

public record ResultadoPago(
        boolean aprobado,
        String referenciaExterna,
        String codigoAutorizacion,
        BigDecimal monto,
        String moneda,
        String mensaje
) {
}
