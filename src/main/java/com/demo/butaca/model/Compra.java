package com.demo.butaca.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "compra",
        indexes = @Index(name = "idx_compra_usuario_fecha", columnList = "id_usuario_comprador, fecha_compra")
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_comprador", nullable = false)
    private Usuario cliente;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_compra", nullable = false, length = 20)
    private EstadoCompra estadoCompra = EstadoCompra.PENDIENTE;

    @Builder.Default
    @Column(name = "moneda_compra", nullable = false, length = 3)
    private String monedaCompra = "ARS";

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "compra", fetch = FetchType.EAGER)
    private List<Entrada> entradas = new ArrayList<>();

    public Long getIdCompra() {
        return id;
    }

    public void setIdCompra(Long idCompra) {
        this.id = idCompra;
    }
}
