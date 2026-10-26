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
public class PagoResponseDTO {
    @JsonProperty("id_pago")
    private Long idPago;

    @JsonProperty("compra_id")
    private Long compraId;

    @JsonProperty("pasarela_pago")
    private String pasarelaPago;

    @JsonProperty("medio_pago")
    private String medioPago;

    @JsonProperty("estado_pago")
    private String estadoPago;

    @JsonProperty("referencia_externa_pago")
    private String referenciaExternaPago;

    @JsonProperty("ultimos_cuatro_digitos")
    private String ultimosCuatroDigitos;

    @JsonProperty("monto")
    private BigDecimal monto;

    @JsonProperty("moneda")
    private String moneda;

    @JsonProperty("codigo_autorizacion")
    private String codigoAutorizacion;

    @JsonProperty("fecha_pago")
    private LocalDateTime fechaPago;
}
