package com.demo.butaca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CancelarEventoRequest {
    @NotNull @AssertTrue
    @JsonProperty("confirmar_reembolsos")
    private Boolean confirmarReembolsos;
    @NotBlank
    @JsonProperty("titulo_confirmacion")
    private String tituloConfirmacion;
    @NotNull
    private Long version;
}
