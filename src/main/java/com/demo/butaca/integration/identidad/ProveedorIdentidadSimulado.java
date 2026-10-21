package com.demo.butaca.integration.identidad;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/**
 * Simulación de un proveedor tipo RENAPER para el TP.
 *
 * Reglas de la demo:
 * - DNI de 7 u 8 dígitos.
 * - Foto del DNI y selfie en formato de imagen.
 * - Nombre y apellido declarados compatibles con los de la cuenta.
 *
 * No se conecta a RENAPER, no hace prueba de vida real y no conserva las
 * imágenes. El resultado queda identificado como SIMULADO.
 */
@Component
public class ProveedorIdentidadSimulado implements ProveedorIdentidad {

    private static final BigDecimal PUNTAJE_OK = new BigDecimal("0.9700");

    @Override
    public ResultadoVerificacionIdentidad verificar(SolicitudVerificacionIdentidad solicitud) {
        String dni = soloDigitos(solicitud.dni());
        if (dni.length() < 7 || dni.length() > 8) {
            return rechazo("El DNI debe tener 7 u 8 dígitos");
        }

        if (solicitud.fotoDni() == null || solicitud.fotoDni().length == 0) {
            return rechazo("Falta la foto del frente del DNI");
        }
        if (!esImagen(solicitud.fotoDni())) {
            return rechazo("La foto del DNI no es una imagen válida");
        }

        if (solicitud.selfie() == null || solicitud.selfie().length == 0) {
            return rechazo("Falta la selfie para la prueba de vida");
        }
        if (!esImagen(solicitud.selfie())) {
            return rechazo("La selfie no es una imagen válida");
        }

        if (solicitud.numeroTramite() != null && !solicitud.numeroTramite().isBlank()) {
            String tramite = soloDigitos(solicitud.numeroTramite());
            if (tramite.length() < 8 || tramite.length() > 11) {
                return rechazo("El número de trámite del DNI no tiene un formato válido");
            }
        }

        String nombreDeclarado = normalizarTexto(solicitud.nombreDeclarado());
        String apellidoDeclarado = normalizarTexto(solicitud.apellidoDeclarado());
        String nombreRegistrado = normalizarTexto(solicitud.nombreRegistrado());
        String apellidoRegistrado = normalizarTexto(solicitud.apellidoRegistrado());

        if (nombreDeclarado.isBlank() || apellidoDeclarado.isBlank()) {
            return rechazo("El nombre y el apellido son obligatorios para validar la identidad");
        }
        if (!compatible(nombreDeclarado, nombreRegistrado) || !compatible(apellidoDeclarado, apellidoRegistrado)) {
            return rechazo("Los datos declarados no coinciden con los de la cuenta");
        }

        String codigo = "SIM-KYC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return new ResultadoVerificacionIdentidad(
                true,
                nombre(),
                codigo,
                PUNTAJE_OK,
                "Identidad validada por el simulador documental/biométrico (no es RENAPER real)"
        );
    }

    @Override
    public String nombre() {
        return "SIMULADO_RENAPER_DEMO";
    }

    private ResultadoVerificacionIdentidad rechazo(String observacion) {
        return new ResultadoVerificacionIdentidad(
                false,
                nombre(),
                "SIM-KYC-REC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT),
                BigDecimal.ZERO,
                observacion
        );
    }

    private boolean compatible(String declarado, String registrado) {
        if (registrado.isBlank()) {
            return true;
        }
        return declarado.equals(registrado)
                || declarado.contains(registrado)
                || registrado.contains(declarado);
    }

    private boolean esImagen(byte[] contenido) {
        if (contenido == null || contenido.length <= 16) {
            return false;
        }
        // Se valida la firma binaria; no se confía únicamente en el Content-Type.
        return esPng(contenido) || esJpeg(contenido) || esWebp(contenido);
    }

    private boolean esPng(byte[] bytes) {
        return bytes.length > 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
    }

    private boolean esJpeg(byte[] bytes) {
        return bytes.length > 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean esWebp(byte[] bytes) {
        return bytes.length > 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private String normalizarTexto(String value) {
        if (value == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String soloDigitos(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
