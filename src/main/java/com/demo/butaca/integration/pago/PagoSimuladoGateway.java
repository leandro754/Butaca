package com.demo.butaca.integration.pago;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulador de pasarela para la demo. No cobra dinero ni contacta a un
 * proveedor real. Un token que empieza con "RECHAZ" permite probar el camino
 * de pago rechazado sin introducir datos de tarjeta.
 */
@Component
public class PagoSimuladoGateway implements PagoGateway {

    @Override
    public ResultadoPago procesar(SolicitudPago solicitud) {
        if (solicitud.monto() == null || solicitud.monto().compareTo(BigDecimal.ZERO) <= 0) {
            return rechazo(solicitud, "El monto a cobrar debe ser mayor a cero");
        }

        if (solicitud.tokenPago() != null
                && solicitud.tokenPago().trim().toUpperCase().startsWith("RECHAZ")) {
            return rechazo(solicitud, "Pago rechazado por el simulador de la pasarela");
        }

        String referencia = "SIM-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String autorizacion = "AUT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new ResultadoPago(
                true,
                referencia,
                autorizacion,
                solicitud.monto(),
                solicitud.moneda(),
                "Pago aprobado por el simulador de la pasarela"
        );
    }

    private ResultadoPago rechazo(SolicitudPago solicitud, String mensaje) {
        return new ResultadoPago(
                false,
                "SIM-REC-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(),
                null,
                solicitud.monto(),
                solicitud.moneda(),
                mensaje
        );
    }
}
