package com.demo.butaca.integration.identidad;

import java.math.BigDecimal;

public record ResultadoVerificacionIdentidad(
        boolean aprobado,
        String proveedor,
        String codigo,
        BigDecimal puntaje,
        String observacion
) {
}
