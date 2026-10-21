package com.demo.butaca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CompraRequestDTO {
    @NotNull
    @JsonProperty("evento_id")
    private Long eventoId;

    @NotNull
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

    @Min(1)
    private Integer cantidad = 1;

    @JsonProperty("pasarela_pago")
    private String pasarelaPago;

    @JsonProperty("medio_pago")
    private String medioPago;

    @JsonProperty("ultimos_cuatro_digitos")
    private String ultimosCuatroDigitos;

    /**
     * Token opaco emitido por una pasarela real. En la demo, un token que
     * empieza con RECHAZ permite probar el camino de pago rechazado.
     */
    @JsonProperty("token_pago")
    private String tokenPago;

    @JsonProperty("tipo_comprobante")
    private String tipoComprobante;

    @JsonProperty("asistentes")
    private List<AsistenteNominalDTO> asistentes;
}

