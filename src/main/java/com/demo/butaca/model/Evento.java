package com.demo.butaca.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "evento",
        indexes = {
                @Index(name = "idx_evento_organizador", columnList = "id_usuario_organizador"),
                @Index(name = "idx_evento_ubicacion", columnList = "id_ubicacion"),
                @Index(name = "idx_evento_categoria", columnList = "id_categoria"),
                @Index(name = "idx_evento_estado_fecha", columnList = "estado_evento, fecha_evento")
        }
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evento extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    @EqualsAndHashCode.Include
    private Long idEvento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_organizador", nullable = false)
    private Usuario organizador;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ubicacion", nullable = false)
    private Ubicacion ubicacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(name = "titulo_evento", nullable = false, length = 200)
    private String tituloEvento;

    @Column(name = "descripcion_evento", columnDefinition = "TEXT")
    private String descripcionEvento;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDate fechaEvento;

    @Column(name = "hora_evento", nullable = false)
    private LocalTime horaEvento;

    @Column(name = "cupo_total_evento", nullable = false)
    private Integer cuposTotalesEvento;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_evento", nullable = false, length = 20)
    private EstadoEvento estadoPublicacionEvento = EstadoEvento.BORRADOR;

    @Column(name = "imagen_url_evento", columnDefinition = "TEXT")
    private String imagenUrl;

    @Transient
    private BigDecimal precioEntradaEvento;

    @Version
    @Builder.Default
    @Column(name = "version_evento", nullable = false)
    private Long version = 0L;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TipoEntrada> tiposEntrada = new ArrayList<>();

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Entrada> entradas = new ArrayList<>();

    public BigDecimal getPrecioEntradaEvento() {
        if (tiposEntrada != null && !tiposEntrada.isEmpty()) {
            return tiposEntrada.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActivoTipoEntrada()))
                    .map(TipoEntrada::getPrecioTipoEntrada)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }
        return precioEntradaEvento != null ? precioEntradaEvento : BigDecimal.ZERO;
    }

    public void setPrecioEntradaEvento(BigDecimal precio) {
        this.precioEntradaEvento = precio;
        if (precio == null) {
            return;
        }
        if (this.tiposEntrada == null) {
            this.tiposEntrada = new ArrayList<>();
        }
        if (this.tiposEntrada.isEmpty()) {
            this.tiposEntrada.add(TipoEntrada.builder()
                    .evento(this)
                    .nombreTipoEntrada("General")
                    .descripcionTipoEntrada("Entrada General")
                    .precioTipoEntrada(precio)
                    .monedaTipoEntrada("ARS")
                    .activoTipoEntrada(true)
                    .build());
        } else {
            this.tiposEntrada.get(0).setPrecioTipoEntrada(precio);
        }
    }

    public TipoEntrada obtenerTipoEntradaPrincipal() {
        if (tiposEntrada != null && !tiposEntrada.isEmpty()) {
            return tiposEntrada.stream().filter(tipo -> Boolean.TRUE.equals(tipo.getActivoTipoEntrada()))
                    .min(java.util.Comparator.comparing(TipoEntrada::getPrecioTipoEntrada)).orElse(null);
        }
        return null;
    }
}
