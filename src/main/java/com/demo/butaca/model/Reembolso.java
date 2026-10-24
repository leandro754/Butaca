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
        name = "reembolso",
        indexes = {
                @Index(name = "idx_reembolso_pago", columnList = "id_pago"),
                @Index(name = "idx_reembolso_estado_fecha", columnList = "estado_reembolso, fecha_solicitud_reembolso")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reembolso_referencia_externa",
                columnNames = "referencia_externa_reembolso"
        )
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reembolso extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reembolso")
    @EqualsAndHashCode.Include
    private Long idReembolso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago", nullable = false)
    private Pago pago;

    @Column(name = "monto_reembolso", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoReembolso;

    @Builder.Default
    @Column(name = "moneda_reembolso", nullable = false, length = 3)
    private String monedaReembolso = "ARS";

    @Column(name = "motivo_reembolso", columnDefinition = "TEXT")
    private String motivoReembolso;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_reembolso", nullable = false, length = 20)
    private EstadoReembolso estadoReembolso = EstadoReembolso.PENDIENTE;

    @Column(name = "referencia_externa_reembolso", unique = true, length = 255)
    private String referenciaExternaReembolso;

    @Column(name = "fecha_solicitud_reembolso", nullable = false)
    private LocalDateTime fechaSolicitudReembolso;

    @Column(name = "fecha_procesamiento_reembolso")
    private LocalDateTime fechaProcesamientoReembolso;
}
