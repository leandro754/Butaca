package com.demo.butaca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionIdentidadDTO {
    @JsonProperty("id_verificacion")
    private Long idVerificacion;

    @JsonProperty("usuario_id")
    private Long usuarioId;

    @JsonProperty("email_usuario")
    private String emailUsuario;

    @JsonProperty("nombre_usuario")
    private String nombreUsuario;

    @JsonProperty("apellido_usuario")
    private String apellidoUsuario;

    @JsonProperty("dni_usuario")
    private String dniUsuario;

    @JsonProperty("numero_tramite")
    private String numeroTramite;

    @JsonProperty("metodo_verificacion")
    private String metodoVerificacion;

    @JsonProperty("estado_verificacion")
    private String estadoVerificacion;

    @JsonProperty("codigo_validacion_kyc")
    private String codigoValidacionKyc;

    @JsonProperty("observacion_verificacion")
    private String observacionVerificacion;

    @JsonProperty("hash_evidencia_documental")
    private String hashEvidenciaDocumental;

    @JsonProperty("hash_selfie")
    private String hashSelfie;

    @JsonProperty("proveedor_identidad")
    private String proveedorIdentidad;

    @JsonProperty("puntaje_coincidencia")
    private BigDecimal puntajeCoincidencia;

    @JsonProperty("fecha_solicitud")
    private LocalDateTime fechaSolicitud;

    @JsonProperty("fecha_verificacion")
    private LocalDateTime fechaVerificacion;
}

