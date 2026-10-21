package com.demo.butaca.service;

import com.demo.butaca.dto.AuthResponse;
import com.demo.butaca.dto.LoginRequest;
import com.demo.butaca.dto.RegistroUsuarioRequest;
import com.demo.butaca.model.EstadoUsuario;
import com.demo.butaca.model.EstadoVerificacion;
import com.demo.butaca.model.Rol;
import com.demo.butaca.model.Usuario;
import com.demo.butaca.model.VerificacionIdentidad;
import com.demo.butaca.repository.RolRepository;
import com.demo.butaca.repository.UsuarioRepository;
import com.demo.butaca.repository.VerificacionIdentidadRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final VerificacionIdentidadRepository verificacionRepository;
    private final AuditoriaSeguridadService auditoriaService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UsuarioRepository usuarioRepository,
                       RolRepository rolRepository,
                       VerificacionIdentidadRepository verificacionRepository,
                       AuditoriaSeguridadService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.verificacionRepository = verificacionRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public AuthResponse registrar(RegistroUsuarioRequest request) {
        String emailNormalizado = request.getEmailUsuario().trim().toLowerCase();

        if (usuarioRepository.existsByEmailUsuarioIgnoreCase(emailNormalizado)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        String rolNombre = normalizarRol(request.getRolUsuario());
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + rolNombre));

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(request.getNombreUsuario().trim());
        usuario.setApellidoUsuario(request.getApellidoUsuario().trim());
        if (request.getDniUsuario() != null && !request.getDniUsuario().isBlank()) {
            usuario.setDniUsuario(request.getDniUsuario().trim());
        }
        usuario.setEmailUsuario(emailNormalizado);
        usuario.setPasswordUsuario(passwordEncoder.encode(request.getPasswordUsuario()));
        usuario.setRol(rol);
        usuario.setTelefono(request.getTelefono());
        usuario.setEstadoUsuario((request.getEstadoUsuario() == null ? EstadoUsuario.ACTIVO : request.getEstadoUsuario()).name());

        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaService.registrarEvento(
                guardado,
                "REGISTRO",
                "USUARIO",
                guardado.getIdUsuario(),
                "EXITOSO",
                "Registro de nueva cuenta con rol " + rolNombre
        );
        return toResponse(guardado, "Usuario registrado correctamente");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String emailNormalizado = request.getEmailUsuario().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmailUsuarioIgnoreCase(emailNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales invalidas"));

        if (!passwordEncoder.matches(request.getPasswordUsuario(), usuario.getPasswordUsuario())) {
            auditoriaService.registrarEventoIndependiente(
                    usuario,
                    "LOGIN",
                    "USUARIO",
                    usuario.getIdUsuario(),
                    "RECHAZADO",
                    "Intento de inicio de sesión con contraseña incorrecta"
            );
            throw new IllegalArgumentException("Credenciales invalidas");
        }

        auditoriaService.registrarEvento(
                usuario,
                "LOGIN",
                "USUARIO",
                usuario.getIdUsuario(),
                "EXITOSO",
                "Inicio de sesión autenticado con BCrypt"
        );
        return toResponse(usuario, "Inicio de sesion correcto");
    }

    private String normalizarRol(String rolUsuario) {
        if (rolUsuario == null || rolUsuario.isBlank()) {
            return "CLIENTE";
        }
        String normalizado = rolUsuario.trim().toUpperCase();
        return "CLIENT".equals(normalizado) ? "CLIENTE" : normalizado;
    }

    private AuthResponse toResponse(Usuario usuario, String mensaje) {
        VerificacionIdentidad verificacion = verificacionRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElse(null);
        boolean verificado = verificacion != null && EstadoVerificacion.VERIFICADA.equals(verificacion.getEstadoVerificacion());
        String estadoVerif = verificacion != null ? verificacion.getEstadoVerificacion().name() : EstadoVerificacion.PENDIENTE.name();

        return AuthResponse.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombreUsuario(usuario.getNombreUsuario())
                .apellidoUsuario(usuario.getApellidoUsuario())
                .dniUsuario(usuario.getDniUsuario())
                .emailUsuario(usuario.getEmailUsuario())
                .rolUsuario(usuario.getRol() == null ? null : usuario.getRol().getNombre())
                .telefono(usuario.getTelefono())
                .estadoUsuario(usuario.getEstadoUsuario())
                .identidadVerificada(verificado)
                .estadoVerificacion(estadoVerif)
                .mensaje(mensaje)
                .build();
    }
}
