package com.demo.butaca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "persona",
        indexes = @Index(name = "idx_persona_email", columnList = "email_persona")
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    @EqualsAndHashCode.Include
    private Long idPersona;

    @Column(name = "dni_persona", nullable = false, unique = true)
    private Long dniPersona;

    @Column(name = "nombre_persona", nullable = false, length = 100)
    private String nombrePersona;

    @Column(name = "apellido_persona", nullable = false, length = 100)
    private String apellidoPersona;

    @Column(name = "email_persona", length = 255)
    private String emailPersona;

    @Column(name = "telefono_persona", length = 30)
    private String telefonoPersona;

    public String getNombreCompleto() {
        String n = nombrePersona == null ? "" : nombrePersona.trim();
        String a = apellidoPersona == null ? "" : apellidoPersona.trim();
        return (n + " " + a).trim();
    }
}
