package com.demo.butaca.integration.identidad;

/**
 * Datos que se envían al proveedor de identidad. Las imágenes se mantienen en
 * memoria durante la verificación y no se persisten en la base.
 */
public record SolicitudVerificacionIdentidad(
        Long usuarioId,
        String dni,
        String nombreDeclarado,
        String apellidoDeclarado,
        String nombreRegistrado,
        String apellidoRegistrado,
        String email,
        String numeroTramite,
        String metodo,
        byte[] fotoDni,
        String fotoDniContentType,
        byte[] selfie,
        String selfieContentType
) {
}
