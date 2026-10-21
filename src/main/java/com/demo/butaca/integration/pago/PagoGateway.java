package com.demo.butaca.integration.pago;

/**
 * Contrato para una pasarela de pago. La implementación por defecto es un
 * simulador de laboratorio; para producción se reemplaza por un adaptador que
 * use el SDK/API del proveedor y tokenización del lado del cliente.
 */
public interface PagoGateway {
    ResultadoPago procesar(SolicitudPago solicitud);
}
