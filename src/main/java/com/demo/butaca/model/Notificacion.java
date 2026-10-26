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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notificacion",
        indexes = {
                @Index(name = "idx_notificacion_usuario_fecha", columnList = "id_usuario, created_at"),
                @Index(name = "idx_notificacion_evento", columnList = "id_evento"),
                @Index(name = "idx_notificacion_compra", columnList = "id_compra")
        }
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    @EqualsAndHashCode.Include
    private Long idNotificacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @Builder.Default
    @Column(name = "tipo_notificacion", nullable = false, length = 20)
    private String tipo = "INFO";

    @Column(name = "asunto_notificacion", length = 200)
    private String asunto;

    @Column(name = "mensaje_notificacion", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio_notificacion", nullable = false, length = 20)
    private EstadoEnvioNotificacion estadoEnvio = EstadoEnvioNotificacion.PENDIENTE;

    @Column(name = "fecha_envio_notificacion")
    private LocalDateTime fechaEnvio;

    @Column(name = "error_envio", columnDefinition = "TEXT")
    private String errorEnvio;

    @Builder.Default
    @Column(name = "leida_notificacion", nullable = false)
    private Boolean leida = false;

    @Transient
    private LocalDateTime fechaCreacion;

    public LocalDateTime getFechaCreacion() {
        if (fechaCreacion != null) {
            return fechaCreacion;
        }
        return getCreatedAt() != null ? getCreatedAt() : fechaEnvio;
    }

    public Boolean getLeida() {
        return leida != null ? leida : false;
    }
}
