package com.demo.butaca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pago",
        indexes = {
                @Index(name = "idx_pago_compra", columnList = "id_compra"),
                @Index(name = "idx_pago_estado_fecha", columnList = "estado_pago, fecha_creacion_pago")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pago_proveedor_referencia",
                columnNames = {"proveedor_pago", "referencia_externa_pago"}
        )
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    @EqualsAndHashCode.Include
    private Long idPago;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @Builder.Default
    @Column(name = "proveedor_pago", nullable = false, length = 50)
    private String proveedorPago = "MERCADOPAGO";

    @Column(name = "referencia_externa_pago", length = 255)
    private String referenciaExternaPago;

    @Builder.Default
    @Column(name = "monto_pago", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPago = BigDecimal.ONE;

    @Builder.Default
    @Column(name = "moneda_pago", nullable = false, length = 3)
    private String monedaPago = "ARS";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    @Column(name = "fecha_creacion_pago", nullable = false)
    private LocalDateTime fechaCreacionPago;

    @Column(name = "fecha_confirmacion_pago")
    private LocalDateTime fechaConfirmacionPago;

    @Column(name = "respuesta_proveedor_pago", columnDefinition = "TEXT")
    private String respuestaProveedorPago;

    @Transient
    private PasarelaPago pasarelaPago;

    @Transient
    private MedioPago medioPago;

    @Transient
    private String ultimosCuatroDigitos;

    @Transient
    private LocalDateTime fechaPago;

    public PasarelaPago getPasarelaPago() {
        if (pasarelaPago != null) {
            return pasarelaPago;
        }
        try {
            return PasarelaPago.valueOf(proveedorPago);
        } catch (Exception e) {
            return PasarelaPago.MERCADOPAGO;
        }
    }

    public MedioPago getMedioPago() {
        return medioPago != null ? medioPago : MedioPago.TARJETA_CREDITO;
    }

    public String getUltimosCuatroDigitos() {
        if (ultimosCuatroDigitos != null) {
            return ultimosCuatroDigitos;
        }
        if (respuestaProveedorPago != null && respuestaProveedorPago.contains("last4=")) {
            int idx = respuestaProveedorPago.indexOf("last4=") + 6;
            if (idx + 4 <= respuestaProveedorPago.length()) {
                return respuestaProveedorPago.substring(idx, idx + 4);
            }
        }
        return "4242";
    }

    public LocalDateTime getFechaPago() {
        if (fechaPago != null) {
            return fechaPago;
        }
        return fechaConfirmacionPago != null ? fechaConfirmacionPago : fechaCreacionPago;
    }
}
