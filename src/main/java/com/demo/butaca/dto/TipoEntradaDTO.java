package com.demo.butaca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TipoEntradaDTO {
    @JsonProperty("id_tipo_entrada")
    private Long idTipoEntrada;
    @NotBlank @Size(max = 100)
    private String nombre;
    private String descripcion;
    @NotNull @DecimalMin("0.0")
    private BigDecimal precio;
    @NotNull
    private Boolean activo;
}
