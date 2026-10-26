package com.demo.butaca.integration.identidad;

/**
 * Contrato para un proveedor de verificación de identidad (RENAPER u otro).
 * La implementación incluida es una simulación para la demo; un adaptador real
 * debe validar la documentación y la biometría con el proveedor autorizado.
 */
public interface ProveedorIdentidad {
    ResultadoVerificacionIdentidad verificar(SolicitudVerificacionIdentidad solicitud);

    String nombre();
}
