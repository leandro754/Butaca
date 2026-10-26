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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "verificacion_identidad")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificacionIdentidad extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_verificacion")
    @EqualsAndHashCode.Include
    private Long idVerificacion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_verificacion", nullable = false, length = 30)
    private MetodoVerificacion metodoVerificacion = MetodoVerificacion.BIOMETRIA_RENAPER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion", nullable = false, length = 20)
    private EstadoVerificacion estadoVerificacion = EstadoVerificacion.PENDIENTE;

    @Column(name = "codigo_validacion_kyc", nullable = false, unique = true, length = 100)
    private String codigoValidacionKyc;

    @Column(name = "observacion_verificacion", length = 255)
    private String observacionVerificacion;

    @Column(name = "hash_evidencia_documental", length = 64)
    private String hashEvidenciaDocumental;

    @Column(name = "hash_selfie", length = 64)
    private String hashSelfie;

    @Column(name = "proveedor_identidad", length = 60)
    private String proveedorIdentidad;

    @Column(name = "puntaje_coincidencia", precision = 5, scale = 4)
    private BigDecimal puntajeCoincidencia;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;
}
