package com.demo.butaca.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "ubicacion",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ubicacion_nombre_direccion",
                columnNames = {"nombre_ubicacion", "direccion_ubicacion"}
        )
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ubicacion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ubicacion")
    @EqualsAndHashCode.Include
    private Long idUbicacion;

    @Column(name = "nombre_ubicacion", nullable = false, length = 150)
    private String nombreLugar;

    @Column(name = "direccion_ubicacion", nullable = false, length = 255)
    private String direccion;

    @Builder.Default
    @Column(name = "capacidad_maxima_ubicacion", nullable = false)
    private Integer capacidadMaxima = 10000;

    @Builder.Default
    @Column(name = "activa_ubicacion", nullable = false)
    private Boolean activo = true;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "ubicacion")
    private List<Evento> eventos = new ArrayList<>();

    public Integer getCapacidadMaxima() {
        return capacidadMaxima != null ? capacidadMaxima : 10000;
    }
}
