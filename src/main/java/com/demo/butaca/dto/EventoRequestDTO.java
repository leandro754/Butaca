package com.demo.butaca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class EventoRequestDTO {
    private Long version;

    @Valid @Size(min = 1, max = 20)
    @JsonProperty("tipos_entrada")
    private List<TipoEntradaDTO> tiposEntrada;
    @NotBlank
    @JsonProperty("titulo_evento")
    private String tituloEvento;

    @JsonProperty("descripcion_evento")
    private String descripcionEvento;

    @NotNull
    @JsonProperty("ubicacion_id")
    private Long ubicacionId;

    @NotNull
    @FutureOrPresent
    @JsonProperty("fecha_evento")
    private LocalDate fechaEvento;

    @NotNull
    @JsonProperty("hora_evento")
    private LocalTime horaEvento;

    @NotNull
    @DecimalMin("0.0")
    @JsonProperty("precio_entrada_evento")
    private BigDecimal precioEntradaEvento;

    @NotNull
    @Positive
    @JsonProperty("cupos_totales_evento")
    private Integer cuposTotalesEvento;

    @JsonProperty("estado_publicacion_evento")
    private String estadoPublicacionEvento;

    @JsonProperty("imagen_url")
    private String imagenUrl;

    @NotNull
    @JsonProperty("categoria_id")
    private Long categoriaId;

    @NotNull
    @JsonProperty("organizador_id_evento")
    private Long organizadorIdEvento;
}
