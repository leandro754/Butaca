package com.demo.butaca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "pasarela_pago", nullable = false, length = 30)
    private PasarelaPago pasarelaPago = PasarelaPago.MERCADOPAGO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 30)
    private MedioPago medioPago = MedioPago.TARJETA_CREDITO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago = EstadoPago.APROBADO;

    @Column(name = "referencia_externa_pago", nullable = false, unique = true, length = 120)
    private String referenciaExternaPago;

    @Column(name = "ultimos_cuatro_digitos", length = 4)
    private String ultimosCuatroDigitos;

    @Column(name = "monto_pago", precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda_pago", length = 3)
    private String moneda;

    @Column(name = "codigo_autorizacion_pago", length = 50)
    private String codigoAutorizacion;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;
}
