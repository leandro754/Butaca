package com.demo.butaca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDTO {
    @JsonProperty("id_notificacion")
    private Long idNotificacion;

    @NotNull
    @JsonProperty("usuario_id")
    private Long usuarioId;

    @JsonProperty("evento_id")
    private Long eventoId;

    @JsonProperty("compra_id")
    private Long compraId;

    @JsonProperty("estado_envio")
    private String estadoEnvio;

    @NotBlank
    private String mensaje;

    private String tipo;
    private Boolean leida;

    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("fecha_envio")
    private LocalDateTime fechaEnvio;
}
