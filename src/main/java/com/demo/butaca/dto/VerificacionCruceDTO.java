package com.demo.butaca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionCruceDTO {
    @JsonProperty("compra_id")
    private Long compraId;

    @JsonProperty("valida")
    private boolean valida;

    @JsonProperty("hash_esperado")
    private String hashEsperado;

    @JsonProperty("hash_almacenado")
    private String hashAlmacenado;

    @JsonProperty("checks")
    private Map<String, Boolean> checks;

    @JsonProperty("observacion")
    private String observacion;
}
