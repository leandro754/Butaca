package com.demo.butaca.service;

import com.demo.butaca.dto.AuditoriaSeguridadDTO;
import com.demo.butaca.model.AuditoriaSeguridad;
import com.demo.butaca.model.Usuario;
import com.demo.butaca.repository.AuditoriaSeguridadRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaSeguridadService {

    private final AuditoriaSeguridadRepository auditoriaRepository;

    public AuditoriaSeguridadService(AuditoriaSeguridadRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    /**
     * Evento exitoso: participa de la transacción de negocio. Si la operación
     * se revierte, el evento también, porque el éxito no ocurrió.
     */
    @Transactional
    public AuditoriaSeguridad registrarEvento(Usuario usuario, String accion, String entidad,
                                              Long idRegistro, String resultado, String detalle) {
        return guardar(usuario, accion, entidad, idRegistro, resultado, detalle);
    }

    /**
     * Evento de rechazo/bloqueo: se guarda en una transacción independiente
     * para que el throw de negocio no borre la evidencia del intento.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditoriaSeguridad registrarEventoIndependiente(Usuario usuario, String accion, String entidad,
                                                           Long idRegistro, String resultado, String detalle) {
        return guardar(usuario, accion, entidad, idRegistro, resultado, detalle);
    }

    private AuditoriaSeguridad guardar(Usuario usuario, String accion, String entidad,
                                       Long idRegistro, String resultado, String detalle) {
        return auditoriaRepository.save(AuditoriaSeguridad.builder()
                .usuario(usuario)
                .accionAuditoria(accion)
                .entidadAfectada(entidad)
                .idRegistroAfectado(idRegistro)
                .resultadoAuditoria(resultado == null ? "EXITOSO" : resultado)
                .ipOrigen(obtenerIpOrigen())
                .detalleTracking(detalle)
                .fechaAuditoria(LocalDateTime.now())
                .build());
    }

    private String obtenerIpOrigen() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "127.0.0.1";
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String remote = request.getRemoteAddr();
            return remote == null || remote.isBlank() ? "127.0.0.1" : remote;
        } catch (RuntimeException ex) {
            return "127.0.0.1";
        }
    }

    @Transactional(readOnly = true)
    public List<AuditoriaSeguridadDTO> listarRecientes() {
        return auditoriaRepository.findTop50ByOrderByFechaAuditoriaDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaSeguridadDTO> listarPorUsuario(Long idUsuario) {
        return auditoriaRepository.findByUsuarioIdUsuarioOrderByFechaAuditoriaDesc(idUsuario)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public AuditoriaSeguridadDTO toDTO(AuditoriaSeguridad entidad) {
        return AuditoriaSeguridadDTO.builder()
                .idAuditoria(entidad.getIdAuditoria())
                .usuarioId(entidad.getUsuario() == null ? null : entidad.getUsuario().getIdUsuario())
                .emailUsuario(entidad.getUsuario() == null ? null : entidad.getUsuario().getEmailUsuario())
                .accionAuditoria(entidad.getAccionAuditoria())
                .entidadAfectada(entidad.getEntidadAfectada())
                .idRegistroAfectado(entidad.getIdRegistroAfectado())
                .resultadoAuditoria(entidad.getResultadoAuditoria())
                .ipOrigen(entidad.getIpOrigen())
                .detalleTracking(entidad.getDetalleTracking())
                .fechaAuditoria(entidad.getFechaAuditoria())
                .build();
    }
}
