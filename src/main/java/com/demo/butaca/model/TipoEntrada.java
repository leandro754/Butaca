package com.demo.butaca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
        name = "tipo_entrada",
        indexes = @Index(name = "idx_tipo_entrada_evento", columnList = "id_evento"),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_tipo_entrada_evento_nombre",
                columnNames = {"id_evento", "nombre_tipo_entrada"}
        )
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoEntrada extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_entrada")
    @EqualsAndHashCode.Include
    private Long idTipoEntrada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Column(name = "nombre_tipo_entrada", nullable = false, length = 100)
    private String nombreTipoEntrada;

    @Column(name = "descripcion_tipo_entrada", columnDefinition = "TEXT")
    private String descripcionTipoEntrada;

    @Column(name = "precio_tipo_entrada", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioTipoEntrada;

    @Builder.Default
    @Column(name = "moneda_tipo_entrada", nullable = false, length = 3)
    private String monedaTipoEntrada = "ARS";

    @Builder.Default
    @Column(name = "activo_tipo_entrada", nullable = false)
    private Boolean activoTipoEntrada = true;
}
