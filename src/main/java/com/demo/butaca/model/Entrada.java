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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "entrada",
        indexes = {
                @Index(name = "idx_entrada_evento_estado", columnList = "id_evento, estado_entrada"),
                @Index(name = "idx_entrada_titular_estado", columnList = "id_persona_titular, estado_entrada"),
                @Index(name = "idx_entrada_compra", columnList = "id_compra"),
                @Index(name = "idx_entrada_tipo_entrada", columnList = "id_tipo_entrada")
        }
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrada extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrada")
    @EqualsAndHashCode.Include
    private Long idEntrada;

    @Column(name = "codigo_unico_entrada", nullable = false, unique = true)
    private UUID codigoUnicoEntrada;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_entrada")
    private TipoEntrada tipoEntrada;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_persona_titular", nullable = false)
    private Persona personaTitular;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrada", nullable = false, length = 20)
    private EstadoEntrada estadoEntrada = EstadoEntrada.CONFIRMADA;

    @Column(name = "precio_pagado_entrada", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPagado;

    @Builder.Default
    @Column(name = "moneda_entrada", nullable = false, length = 3)
    private String monedaEntrada = "ARS";

    @Column(name = "fecha_emision_entrada", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_validacion_entrada")
    private LocalDateTime fechaValidacion;

    @Version
    @Builder.Default
    @Column(name = "version_entrada", nullable = false)
    private Long versionEntrada = 0L;

    @Transient
    private Usuario cliente;

    @Transient
    private String dniAsistente;

    @Transient
    private String nombreAsistente;

    public Usuario getCliente() {
        if (cliente != null) {
            return cliente;
        }
        return compra != null ? compra.getCliente() : null;
    }

    public String getDniAsistente() {
        if (personaTitular != null && personaTitular.getDniPersona() != null) {
            return String.valueOf(personaTitular.getDniPersona());
        }
        return dniAsistente != null ? dniAsistente : "SIN-DNI";
    }

    public String getNombreAsistente() {
        if (personaTitular != null && !personaTitular.getNombreCompleto().isBlank()) {
            return personaTitular.getNombreCompleto();
        }
        return nombreAsistente != null ? nombreAsistente : "Titular Nominal";
    }

    @PrePersist
    public void prePersistEntrada() {
        if (this.codigoUnicoEntrada == null) {
            this.codigoUnicoEntrada = UUID.randomUUID();
        }
        if (this.fechaEmision == null) {
            this.fechaEmision = LocalDateTime.now();
        }
        if (this.monedaEntrada == null || this.monedaEntrada.isBlank()) {
            this.monedaEntrada = "ARS";
        }
        if (this.tipoEntrada == null && this.evento != null) {
            this.tipoEntrada = this.evento.obtenerTipoEntradaPrincipal();
        }
    }
}
