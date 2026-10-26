package com.demo.butaca.service;

import com.demo.butaca.dto.NotificacionDTO;
import com.demo.butaca.dto.VerificacionIdentidadDTO;
import com.demo.butaca.integration.identidad.ProveedorIdentidad;
import com.demo.butaca.integration.identidad.ResultadoVerificacionIdentidad;
import com.demo.butaca.integration.identidad.SolicitudVerificacionIdentidad;
import com.demo.butaca.model.EstadoUsuario;
import com.demo.butaca.model.EstadoVerificacion;
import com.demo.butaca.model.MetodoVerificacion;
import com.demo.butaca.model.Usuario;
import com.demo.butaca.model.Persona;
import com.demo.butaca.model.VerificacionIdentidad;
import com.demo.butaca.repository.UsuarioRepository;
import com.demo.butaca.repository.PersonaRepository;
import com.demo.butaca.repository.VerificacionIdentidadRepository;
import com.demo.butaca.util.HashUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class VerificacionIdentidadService {

    private final VerificacionIdentidadRepository verificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final NotificacionService notificacionService;
    private final AuditoriaSeguridadService auditoriaService;
    private final ProveedorIdentidad proveedorIdentidad;

    public VerificacionIdentidadService(VerificacionIdentidadRepository verificacionRepository,
                                        UsuarioRepository usuarioRepository,
                                        PersonaRepository personaRepository,
                                        NotificacionService notificacionService,
                                        AuditoriaSeguridadService auditoriaService,
                                        ProveedorIdentidad proveedorIdentidad) {
        this.verificacionRepository = verificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.proveedorIdentidad = proveedorIdentidad;
    }

    /**
     * Verifica la identidad de un usuario existente. No crea cuentas, no
     * asigna contraseñas por defecto y no persiste las imágenes: solo guarda
     * hashes SHA-256 de las evidencias para trazabilidad.
     */
    @Transactional
    public VerificacionIdentidadDTO verificarIdentidad(VerificacionIdentidadDTO request,
                                                       byte[] fotoDni,
                                                       String fotoDniContentType,
                                                       byte[] selfie,
                                                       String selfieContentType) {
        if (request == null || request.getUsuarioId() == null) {
            throw new IllegalArgumentException("El id de usuario es obligatorio para verificar identidad");
        }

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!EstadoUsuario.ACTIVO.equals(usuario.getEstadoUsuario())) {
            throw new IllegalStateException("La cuenta de usuario no está activa");
        }

        if (request.getEmailUsuario() != null && !request.getEmailUsuario().isBlank()
                && !request.getEmailUsuario().trim().equalsIgnoreCase(usuario.getEmailUsuario())) {
            auditarRechazo(usuario, "El email informado no coincide con la cuenta");
            throw new IllegalArgumentException("El email informado no coincide con la cuenta");
        }

        String dni = normalizarDni(request.getDniUsuario() != null && !request.getDniUsuario().isBlank()
                ? request.getDniUsuario()
                : usuario.getDniUsuario());

        if (dni.length() < 7 || dni.length() > 8) {
            auditarRechazo(usuario, "DNI inválido");
            throw new IllegalArgumentException("Debe proporcionar un DNI de 7 u 8 dígitos");
        }

        String dniRegistrado = normalizarDni(usuario.getDniUsuario());
        if (!dniRegistrado.isBlank() && !dniRegistrado.equals(dni)) {
            auditarRechazo(usuario, "El DNI informado no coincide con el DNI registrado en la cuenta");
            throw new IllegalArgumentException("El DNI informado no coincide con el DNI registrado en la cuenta");
        }

        Optional<VerificacionIdentidad> existente = verificacionRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());

        // Si ya está verificada con el mismo DNI, la operación es idempotente y
        // no se regenera el código KYC que forma parte del hash del comprobante.
        if (existente.isPresent()
                && EstadoVerificacion.VERIFICADA.equals(existente.get().getEstadoVerificacion())
                && dni.equals(dniRegistrado)) {
            return toDTO(existente.get());
        }

        Optional<Usuario> otroUsuario = usuarioRepository.findFirstByPersonaDniPersona(Long.valueOf(dni))
                .filter(otro -> !otro.getIdUsuario().equals(usuario.getIdUsuario()));
        if (otroUsuario.isPresent()) {
            auditarRechazo(usuario, "El DNI ya está asociado a otra cuenta");
            throw new IllegalArgumentException("El DNI ya está asociado a otra cuenta");
        }

        MetodoVerificacion metodo = parsearMetodo(request.getMetodoVerificacion());
        SolicitudVerificacionIdentidad solicitud = new SolicitudVerificacionIdentidad(
                usuario.getIdUsuario(),
                dni,
                valorODefault(request.getNombreUsuario(), usuario.getNombreUsuario()),
                valorODefault(request.getApellidoUsuario(), usuario.getApellidoUsuario()),
                usuario.getNombreUsuario(),
                usuario.getApellidoUsuario(),
                usuario.getEmailUsuario(),
                request.getNumeroTramite(),
                metodo.name(),
                fotoDni,
                fotoDniContentType,
                selfie,
                selfieContentType
        );

        ResultadoVerificacionIdentidad resultado;
        try {
            resultado = proveedorIdentidad.verificar(solicitud);
        } catch (RuntimeException ex) {
            auditarRechazo(usuario, "Error al comunicarse con el proveedor de identidad");
            throw new IllegalStateException("No se pudo completar la verificación de identidad. Intentá nuevamente.");
        }

        if (resultado == null || resultado.codigo() == null || resultado.codigo().isBlank()) {
            auditarRechazo(usuario, "El proveedor de identidad no devolvió un resultado válido");
            throw new IllegalStateException("El proveedor de identidad no devolvió un resultado válido");
        }

        LocalDateTime ahora = LocalDateTime.now().withNano(0);

        VerificacionIdentidad verificacion = existente.orElseGet(() -> VerificacionIdentidad.builder()
                .usuario(usuario)
                .fechaSolicitud(ahora)
                .build());

        verificacion.setMetodoVerificacion(metodo);
        verificacion.setEstadoVerificacion(resultado.aprobado()
                ? EstadoVerificacion.VERIFICADA
                : EstadoVerificacion.RECHAZADA);
        verificacion.setCodigoValidacionKyc(resultado.codigo());
        verificacion.setObservacionVerificacion(truncar(resultado.observacion(), 255));
        verificacion.setProveedorIdentidad(resultado.proveedor());
        verificacion.setPuntajeCoincidencia(resultado.puntaje());
        verificacion.setHashEvidenciaDocumental(HashUtils.sha256(fotoDni));
        verificacion.setHashSelfie(HashUtils.sha256(selfie));
        if (verificacion.getFechaSolicitud() == null) {
            verificacion.setFechaSolicitud(ahora);
        }
        verificacion.setFechaVerificacion(ahora);
        verificacion = verificacionRepository.save(verificacion);

        if (resultado.aprobado()) {
            if (usuario.getPersona() == null) {
                Persona persona = personaRepository.findByDniPersona(Long.parseLong(dni))
                        .orElseGet(() -> personaRepository.save(Persona.builder()
                                .dniPersona(Long.parseLong(dni))
                                .nombrePersona(usuario.getNombreUsuario())
                                .apellidoPersona(usuario.getApellidoUsuario())
                                .emailPersona(usuario.getEmailUsuario())
                                .build()));
                usuario.setPersona(persona);
            }
            usuario.setDniUsuario(dni);
            usuarioRepository.save(usuario);
        }

        notificacionService.crearNotificacion(NotificacionDTO.builder()
                .usuarioId(usuario.getIdUsuario())
                .mensaje(resultado.aprobado()
                        ? "Tu identidad (DNI " + dni + ") fue verificada con éxito. Ya estás habilitado para comprar entradas nominales."
                        : "No pudimos verificar tu identidad: " + resultado.observacion())
                .tipo("VERIFICACION")
                .fechaEnvio(ahora)
                .build());

        if (resultado.aprobado()) {
            auditoriaService.registrarEvento(
                    usuario,
                    "VERIFICACION_KYC",
                    "VERIFICACION_IDENTIDAD",
                    verificacion.getIdVerificacion(),
                    "EXITOSO",
                    "Identidad verificada mediante " + metodo.name()
                            + " por " + resultado.proveedor()
                            + " (Token KYC: " + verificacion.getCodigoValidacionKyc() + ")"
            );
        } else {
            auditoriaService.registrarEventoIndependiente(
                    usuario,
                    "VERIFICACION_KYC",
                    "VERIFICACION_IDENTIDAD",
                    verificacion.getIdVerificacion(),
                    "RECHAZADO",
                    "Verificación rechazada por " + resultado.proveedor() + ": " + resultado.observacion()
            );
        }

        return toDTO(verificacion);
    }

    @Transactional(readOnly = true)
    public VerificacionIdentidadDTO obtenerPorUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return VerificacionIdentidadDTO.builder()
                    .usuarioId(usuarioId)
                    .estadoVerificacion(EstadoVerificacion.PENDIENTE.name())
                    .observacionVerificacion("Identidad pendiente de verificación")
                    .build();
        }
        return verificacionRepository.findByUsuarioIdUsuario(usuarioId)
                .map(this::toDTO)
                .orElseGet(() -> VerificacionIdentidadDTO.builder()
                        .usuarioId(usuario.getIdUsuario())
                        .dniUsuario(usuario.getDniUsuario())
                        .estadoVerificacion(EstadoVerificacion.PENDIENTE.name())
                        .observacionVerificacion("Identidad pendiente de verificación")
                        .build());
    }

    @Transactional(readOnly = true)
    public boolean estaVerificado(Long usuarioId) {
        return verificacionRepository.findByUsuarioIdUsuario(usuarioId)
                .map(v -> EstadoVerificacion.VERIFICADA.equals(v.getEstadoVerificacion()))
                .orElse(false);
    }

    private void auditarRechazo(Usuario usuario, String motivo) {
        auditoriaService.registrarEventoIndependiente(
                usuario,
                "VERIFICACION_KYC",
                "VERIFICACION_IDENTIDAD",
                usuario.getIdUsuario(),
                "RECHAZADO",
                motivo
        );
    }

    private MetodoVerificacion parsearMetodo(String metodoStr) {
        if (metodoStr == null || metodoStr.isBlank()) {
            return MetodoVerificacion.BIOMETRIA_RENAPER;
        }
        try {
            return MetodoVerificacion.valueOf(metodoStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return MetodoVerificacion.BIOMETRIA_RENAPER;
        }
    }

    private String normalizarDni(String dni) {
        return dni == null ? "" : dni.replaceAll("\\D", "");
    }

    private String valorODefault(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim();
    }

    private String truncar(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    public VerificacionIdentidadDTO toDTO(VerificacionIdentidad entidad) {
        return VerificacionIdentidadDTO.builder()
                .idVerificacion(entidad.getIdVerificacion())
                .usuarioId(entidad.getUsuario() == null ? null : entidad.getUsuario().getIdUsuario())
                .emailUsuario(entidad.getUsuario() == null ? null : entidad.getUsuario().getEmailUsuario())
                .dniUsuario(entidad.getUsuario() == null ? null : entidad.getUsuario().getDniUsuario())
                .metodoVerificacion(entidad.getMetodoVerificacion() == null ? null : entidad.getMetodoVerificacion().name())
                .estadoVerificacion(entidad.getEstadoVerificacion() == null ? null : entidad.getEstadoVerificacion().name())
                .codigoValidacionKyc(entidad.getCodigoValidacionKyc())
                .observacionVerificacion(entidad.getObservacionVerificacion())
                .hashEvidenciaDocumental(entidad.getHashEvidenciaDocumental())
                .hashSelfie(entidad.getHashSelfie())
                .proveedorIdentidad(entidad.getProveedorIdentidad())
                .puntajeCoincidencia(entidad.getPuntajeCoincidencia())
                .fechaSolicitud(entidad.getFechaSolicitud())
                .fechaVerificacion(entidad.getFechaVerificacion())
                .build();
    }
}
