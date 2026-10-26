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

@Entity
@Table(
        name = "usuario",
        indexes = {
                @Index(name = "idx_usuario_persona", columnList = "id_persona"),
                @Index(name = "idx_usuario_rol", columnList = "id_rol"),
                @Index(name = "idx_usuario_estado", columnList = "estado_usuario")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email_usuario")
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    @EqualsAndHashCode.Include
    private Long idUsuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_persona")
    private Persona persona;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Builder.Default
    @Column(name = "estado_usuario", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoUsuario estadoUsuario = EstadoUsuario.ACTIVO;

    @Column(name = "email_usuario", nullable = false, unique = true, length = 255)
    private String emailUsuario;

    @Column(name = "password_hash_usuario", nullable = false, length = 255)
    private String passwordUsuario;

    @Column(name = "telefono_usuario", length = 20)
    private String telefono;

    @Transient
    private String nombreUsuario;

    @Transient
    private String apellidoUsuario;

    @Transient
    private String dniUsuario;

    public String getNombreUsuario() {
        if (persona != null && persona.getNombrePersona() != null) {
            return persona.getNombrePersona();
        }
        return nombreUsuario != null ? nombreUsuario : "Usuario";
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
        if (persona != null && nombreUsuario != null) {
            persona.setNombrePersona(nombreUsuario);
        }
    }

    public String getApellidoUsuario() {
        if (persona != null && persona.getApellidoPersona() != null) {
            return persona.getApellidoPersona();
        }
        return apellidoUsuario != null ? apellidoUsuario : "";
    }

    public void setApellidoUsuario(String apellidoUsuario) {
        this.apellidoUsuario = apellidoUsuario;
        if (persona != null && apellidoUsuario != null) {
            persona.setApellidoPersona(apellidoUsuario);
        }
    }

    public String getDniUsuario() {
        if (persona != null && persona.getDniPersona() != null) {
            return String.valueOf(persona.getDniPersona());
        }
        return dniUsuario;
    }

    public void setDniUsuario(String dniUsuario) {
        this.dniUsuario = dniUsuario;
        if (persona != null && dniUsuario != null && !dniUsuario.isBlank()) {
            String digitos = dniUsuario.replaceAll("\\D", "");
            if (!digitos.isEmpty()) {
                persona.setDniPersona(Long.parseLong(digitos));
            }
        }
    }

    public RolUsuario getRolUsuario() {
        if (rol == null || rol.getNombre() == null) {
            return null;
        }
        String nombre = rol.getNombre();
        return RolUsuario.valueOf("CLIENT".equals(nombre) ? "CLIENTE" : nombre);
    }

    public void setRolUsuario(RolUsuario rolUsuario) {
        this.rol = rolUsuario == null ? null : Rol.builder().nombre(rolUsuario.name()).build();
    }

    public void setEstadoUsuario(String estadoUsuario) {
        this.estadoUsuario = estadoUsuario == null ? null : EstadoUsuario.valueOf(estadoUsuario);
    }

    public void setEstadoUsuario(EstadoUsuario estadoUsuario) {
        this.estadoUsuario = estadoUsuario;
    }
}
