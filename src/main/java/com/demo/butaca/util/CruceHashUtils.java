package com.demo.butaca.util;

import com.demo.butaca.model.Compra;
import com.demo.butaca.model.Entrada;
import com.demo.butaca.model.Pago;
import com.demo.butaca.model.VerificacionIdentidad;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Canonicaliza los datos que se cruzan entre compra, KYC, pago y entradas
 * nominales. El mismo método se usa al emitir y al verificar el comprobante.
 */
public final class CruceHashUtils {

    private CruceHashUtils() {
    }

    public static String calcular(Compra compra, VerificacionIdentidad verificacion,
                                  Pago pago, List<Entrada> entradas) {
        List<Entrada> ordenadas = entradas.stream()
                .sorted(Comparator.comparing(Entrada::getIdEntrada))
                .toList();

        String tickets = ordenadas.stream()
                .map(entrada -> entrada.getCodigoUnicoEntrada().toString())
                .collect(Collectors.joining(","));
        String asistentes = ordenadas.stream()
                .map(entrada -> entrada.getDniAsistente() + ":" + entrada.getNombreAsistente())
                .collect(Collectors.joining(","));

        String payload = "COMPRA=" + compra.getId()
                + "|FECHA=" + compra.getFechaCompra()
                + "|DNI_COMPRADOR=" + compra.getCliente().getDniUsuario()
                + "|KYC=" + verificacion.getCodigoValidacionKyc()
                + "|PAGO=" + pago.getReferenciaExternaPago()
                + "|MONTO=" + pago.getMonto().toPlainString()
                + "|MONEDA=" + pago.getMoneda()
                + "|TICKETS=" + tickets
                + "|ASISTENTES=" + asistentes;

        return HashUtils.sha256(payload);
    }
}
