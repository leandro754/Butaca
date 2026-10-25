package com.demo.butaca.config;

import com.demo.butaca.model.AuditoriaSeguridad;
import com.demo.butaca.model.Categoria;
import com.demo.butaca.model.Compra;
import com.demo.butaca.model.Comprobante;
import com.demo.butaca.model.Entrada;
import com.demo.butaca.model.EstadoCompra;
import com.demo.butaca.model.EstadoEntrada;
import com.demo.butaca.model.EstadoEnvio;
import com.demo.butaca.model.EstadoEvento;
import com.demo.butaca.model.EstadoPago;
import com.demo.butaca.model.EstadoUsuario;
import com.demo.butaca.model.EstadoVerificacion;
import com.demo.butaca.model.Evento;
import com.demo.butaca.model.HistorialEstadoEntrada;
import com.demo.butaca.model.MedioPago;
import com.demo.butaca.model.MetodoVerificacion;
import com.demo.butaca.model.Notificacion;
import com.demo.butaca.model.Pago;
import com.demo.butaca.model.PasarelaPago;
import com.demo.butaca.model.Persona;
import com.demo.butaca.model.Rol;
import com.demo.butaca.model.TipoComprobante;
import com.demo.butaca.model.Ubicacion;
import com.demo.butaca.model.Usuario;
import com.demo.butaca.model.VerificacionIdentidad;
import com.demo.butaca.repository.AuditoriaSeguridadRepository;
import com.demo.butaca.repository.CategoriaRepository;
import com.demo.butaca.repository.CompraRepository;
import com.demo.butaca.repository.ComprobanteRepository;
import com.demo.butaca.repository.EntradaRepository;
import com.demo.butaca.repository.EventoRepository;
import com.demo.butaca.repository.HistorialEstadoEntradaRepository;
import com.demo.butaca.repository.NotificacionRepository;
import com.demo.butaca.repository.PagoRepository;
import com.demo.butaca.repository.PersonaRepository;
import com.demo.butaca.repository.RolRepository;
import com.demo.butaca.repository.UbicacionRepository;
import com.demo.butaca.repository.UsuarioRepository;
import com.demo.butaca.repository.VerificacionIdentidadRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarioRepository,
                                   PersonaRepository personaRepository,
                                   EventoRepository eventoRepository,
                                   RolRepository rolRepository,
                                   CategoriaRepository categoriaRepository,
                                   UbicacionRepository ubicacionRepository,
                                   CompraRepository compraRepository,
                                   EntradaRepository entradaRepository,
                                   HistorialEstadoEntradaRepository historialRepository,
                                   NotificacionRepository notificacionRepository,
                                   VerificacionIdentidadRepository verificacionRepository,
                                   PagoRepository pagoRepository,
                                   ComprobanteRepository comprobanteRepository,
                                   AuditoriaSeguridadRepository auditoriaRepository,
                                   Environment environment) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            Rol adminRol = asegurarRol(rolRepository, "ADMIN");
            Rol clienteRol = asegurarRol(rolRepository, "CLIENTE");
            Rol organizadorRol = asegurarRol(rolRepository, "ORGANIZADOR");

            List<Categoria> categorias = List.of(
                    asegurarCategoria(categoriaRepository, "Conciertos", "Eventos musicales y recitales"),
                    asegurarCategoria(categoriaRepository, "Teatro", "Obras teatrales y artes escenicas"),
                    asegurarCategoria(categoriaRepository, "Deportes", "Competencias y eventos deportivos"),
                    asegurarCategoria(categoriaRepository, "Festivales", "Festivales y experiencias culturales")
            );

            List<Ubicacion> ubicaciones = List.of(
                    asegurarUbicacion(ubicacionRepository, "Teatro Vorterix", "Av. Federico Lacroze 3455, Buenos Aires", 500),
                    asegurarUbicacion(ubicacionRepository, "Parque Centenario", "Anfiteatro Eva Peron, Buenos Aires", 1000),
                    asegurarUbicacion(ubicacionRepository, "Teatro San Martin", "Av. Corrientes 1530, Buenos Aires", 350),
                    asegurarUbicacion(ubicacionRepository, "Microestadio Malvinas", "Gutemberg 350, Buenos Aires", 900),
                    asegurarUbicacion(ubicacionRepository, "Centro Cultural Recoleta", "Junin 1930, Buenos Aires", 700)
            );

            Usuario admin = asegurarUsuario(usuarioRepository, personaRepository, encoder, adminRol, "Admin", "Sistema",
                    "30111222", "admin@butaca.com", "admin123", "3810000001");
            Usuario org = asegurarUsuario(usuarioRepository, personaRepository, encoder, organizadorRol, "Productora", "Eventos S.A.",
                    "33444555", "org@butaca.com", "org123", "3810000002");
            Usuario cliente = asegurarUsuario(usuarioRepository, personaRepository, encoder, clienteRol, "Juan", "Perez",
                    "42850123", "cliente@butaca.com", "cliente123", "3810000003");
            Usuario ana = asegurarUsuario(usuarioRepository, personaRepository, encoder, clienteRol, "Ana", "Gomez",
                    "44120987", "ana@butaca.com", "ana123", "3810000004");

            asegurarVerificacionKyc(verificacionRepository, auditoriaRepository, admin, "KYC-REN-ADMIN001");
            asegurarVerificacionKyc(verificacionRepository, auditoriaRepository, org, "KYC-REN-ORG00002");
            asegurarVerificacionKyc(verificacionRepository, auditoriaRepository, cliente, "KYC-REN-CLI00003");
            asegurarVerificacionKyc(verificacionRepository, auditoriaRepository, ana, "KYC-REN-CLI00004");

            List<Evento> eventos = new ArrayList<>();
            if (eventoRepository.count() == 0) {
                eventos.add(crearEventoDemo(eventoRepository, org, categorias.get(0), ubicaciones.get(0), "Concierto de Rock Alternativo",
                        "Noche eléctrica de bandas indie y rock alternativo en vivo con puesta de luces inmersiva.",
                        LocalDate.now().plusDays(15), LocalTime.of(21, 0), new BigDecimal("15000.00"), 500,
                        EstadoEvento.PUBLICADO,
                        "https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&w=1200&q=80"));

                eventos.add(crearEventoDemo(eventoRepository, org, categorias.get(3), ubicaciones.get(1), "Festival de Jazz al Aire Libre",
                        "Ensambles de jazz contemporáneo, soul y gastronomía al atardecer en el anfiteatro.",
                        LocalDate.now().plusDays(30), LocalTime.of(16, 0), new BigDecimal("8000.00"), 1000,
                        EstadoEvento.PUBLICADO,
                        "https://images.unsplash.com/photo-1511192336575-5a79af67a629?auto=format&fit=crop&w=1200&q=80"));

                eventos.add(crearEventoDemo(eventoRepository, org, categorias.get(1), ubicaciones.get(2), "Hamlet en Sala Negra",
                        "Puesta contemporánea del clásico de Shakespeare con dirección escénica de vanguardia.",
                        LocalDate.now().plusDays(8), LocalTime.of(20, 30), new BigDecimal("12000.00"), 180,
                        EstadoEvento.PUBLICADO,
                        "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?auto=format&fit=crop&w=1200&q=80"));

                eventos.add(crearEventoDemo(eventoRepository, org, categorias.get(2), ubicaciones.get(3), "Final Metropolitana de Básquet",
                        "Definición del torneo metropolitano en vivo con pantalla 360° y acceso digital por QR.",
                        LocalDate.now().plusDays(22), LocalTime.of(19, 0), new BigDecimal("9500.00"), 900,
                        EstadoEvento.PUBLICADO,
                        "https://images.unsplash.com/photo-1504450758481-7338eba7524a?auto=format&fit=crop&w=1200&q=80"));

                eventos.add(crearEventoDemo(eventoRepository, org, categorias.get(3), ubicaciones.get(4), "Expo Indie Gaming & Synthwave",
                        "Muestra interactiva de estudios independientes, torneos retro y shows de música synthwave.",
                        LocalDate.now().plusDays(45), LocalTime.of(14, 0), new BigDecimal("6000.00"), 700,
                        EstadoEvento.PUBLICADO,
                        "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80"));

                eventos.add(crearEventoDemo(eventoRepository, org, categorias.get(1), ubicaciones.get(2), "Stand Up de Medianoche",
                        "Ciclo de comedia en vivo con cuatro comediantes invitados en formato íntimo.",
                        LocalDate.now().plusDays(12), LocalTime.of(22, 0), new BigDecimal("7000.00"), 120,
                        EstadoEvento.BORRADOR,
                        "https://images.unsplash.com/photo-1585699324551-f6c309eedeca?auto=format&fit=crop&w=1200&q=80"));

                eventos.add(crearEventoDemo(eventoRepository, org, categorias.get(0), ubicaciones.get(0), "Noche Electrónica AudioVisual",
                        "Set internacional de techno melódico y visuales láser.",
                        LocalDate.now().plusDays(18), LocalTime.of(23, 30), new BigDecimal("18000.00"), 500,
                        EstadoEvento.CANCELADO,
                        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80"));
            } else {
                eventos.addAll(eventoRepository.findAll());
            }

            if (compraRepository.count() == 0 && eventos.size() >= 4) {
                LocalDateTime ahora = LocalDateTime.now();
                crearCompraDemo(compraRepository, pagoRepository, comprobanteRepository, entradaRepository,
                        personaRepository, historialRepository, auditoriaRepository, eventos.get(0), cliente, 2,
                        ahora.minusDays(3).withHour(10).withMinute(15), "PAY-MP-DEMO0001", "BUT-2026-000001");
                crearCompraDemo(compraRepository, pagoRepository, comprobanteRepository, entradaRepository,
                        personaRepository, historialRepository, auditoriaRepository, eventos.get(1), cliente, 1,
                        ahora.minusDays(1).withHour(17).withMinute(40), "PAY-MP-DEMO0002", "BUT-2026-000002");
                crearCompraDemo(compraRepository, pagoRepository, comprobanteRepository, entradaRepository,
                        personaRepository, historialRepository, auditoriaRepository, eventos.get(2), ana, 3,
                        ahora.minusDays(2).withHour(19).withMinute(5), "PAY-ST-DEMO0003", "BUT-2026-000003");
                crearCompraDemo(compraRepository, pagoRepository, comprobanteRepository, entradaRepository,
                        personaRepository, historialRepository, auditoriaRepository, eventos.get(3), ana, 2,
                        ahora.minusHours(5), "PAY-MP-DEMO0004", "BUT-2026-000004");
            }

            if (notificacionRepository.count() == 0 && !eventos.isEmpty()) {
                LocalDateTime ahora = LocalDateTime.now();
                crearNotificacionDemo(notificacionRepository, cliente, eventos.get(0),
                        "Compra confirmada para Concierto de Rock Alternativo. Tus entradas están disponibles en tu cuenta.", "INFO", true, ahora.minusDays(3));
                crearNotificacionDemo(notificacionRepository, cliente, eventos.get(1),
                        "Tu entrada nominal (DNI 42850123) para Festival de Jazz al Aire Libre ya esta disponible.", "ENTRADA", false, ahora.minusDays(1));
                crearNotificacionDemo(notificacionRepository, ana, eventos.get(2),
                        "Recordatorio: Hamlet en Sala Negra empieza a las 20:30. Recordá presentar tu DNI físico en puerta.", "RECORDATORIO", false, ahora.minusHours(8));
                crearNotificacionDemo(notificacionRepository, admin, null,
                        "Demo inicializada con verificación KYC, pasarela PCI-DSS, comprobantes SHA-256 y tracking de seguridad.", "SISTEMA", false, ahora.minusMinutes(30));
            }
            if (environment.matchesProfiles("demo")) {
                ampliarCarteleraDemo(eventoRepository, categoriaRepository, ubicacionRepository, org);
                for (Comprobante comprobante : comprobanteRepository.findByEstadoEnvioComprobante(EstadoEnvio.PENDIENTE)) {
                    if (comprobante.getEstadoEnvioComprobante() == EstadoEnvio.PENDIENTE
                            && !notificacionRepository.existsByCompraIdAndTipo(comprobante.getPago().getCompra().getId(), "COMPROBANTE")) {
                        Compra compra = comprobante.getPago().getCompra();
                        notificacionRepository.save(Notificacion.builder().usuario(compra.getCliente()).compra(compra)
                                .mensaje("Tus entradas y comprobante " + comprobante.getNumeroComprobante() + " están disponibles.")
                                .tipo("COMPROBANTE").build());
                    }
                }
            }
        };
    }

    // Shared fixture with scripts/seed-demo-events.ps1. Existing events are never modified.
    private void ampliarCarteleraDemo(EventoRepository eventos, CategoriaRepository categorias,
                                      UbicacionRepository ubicaciones, Usuario organizador) throws java.io.IOException {
        Set<String> titulos = new HashSet<>();
        eventos.findAll().forEach(evento -> titulos.add(evento.getTituloEvento()));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("demo-events.tsv").getInputStream(), StandardCharsets.UTF_8))) {
            for (String line : reader.lines().skip(1).filter(value -> !value.isBlank()).toList()) {
                String[] field = line.split("\t", -1);
                if (!titulos.add(field[0])) continue;
                Categoria categoria = asegurarCategoria(categorias, field[1], "Eventos de " + field[1].toLowerCase());
                Ubicacion ubicacion = ubicaciones.findAll().stream()
                        .filter(value -> field[2].equals(value.getNombreLugar()))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Falta ubicación de demo: " + field[2]));
                crearEventoDemo(eventos, organizador, categoria, ubicacion, field[0], field[8],
                        LocalDate.now().plusDays(Integer.parseInt(field[3])), LocalTime.parse(field[4]),
                        new BigDecimal(field[5]), Integer.parseInt(field[6]), EstadoEvento.PUBLICADO,
                        "https://images.unsplash.com/" + field[7] + "?auto=format&fit=crop&w=1200&q=80");
            }
        }
    }

    private Rol asegurarRol(RolRepository rolRepository, String nombre) {
        return rolRepository.findByNombre(nombre).orElseGet(() -> rolRepository.save(Rol.builder().nombre(nombre).build()));
    }

    private Categoria asegurarCategoria(CategoriaRepository categoriaRepository, String nombre, String descripcion) {
        return categoriaRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> categoriaRepository.save(Categoria.builder().nombre(nombre).descripcion(descripcion).build()));
    }

    private Ubicacion asegurarUbicacion(UbicacionRepository ubicacionRepository, String nombre, String direccion, int capacidad) {
        return ubicacionRepository.findByNombreLugarIgnoreCaseAndDireccionIgnoreCase(nombre, direccion)
                .orElseGet(() -> ubicacionRepository.save(Ubicacion.builder()
                        .nombreLugar(nombre)
                        .direccion(direccion)
                        .capacidadMaxima(capacidad)
                        .activo(true)
                        .build()));
    }

    private Usuario asegurarUsuario(UsuarioRepository usuarioRepository, PersonaRepository personaRepository,
                                    BCryptPasswordEncoder encoder, Rol rol,
                                    String nombre, String apellido, String dni, String email, String password, String telefono) {
        return usuarioRepository.findByEmailUsuarioIgnoreCase(email).orElseGet(() -> {
            Long dniLong = Long.parseLong(dni);
            Persona persona = personaRepository.findByDniPersona(dniLong)
                    .orElseGet(() -> personaRepository.save(Persona.builder()
                            .dniPersona(dniLong)
                            .nombrePersona(nombre)
                            .apellidoPersona(apellido)
                            .emailPersona(email)
                            .telefonoPersona(telefono)
                            .build()));
            Usuario usuario = new Usuario();
            usuario.setPersona(persona);
            usuario.setNombreUsuario(nombre);
            usuario.setApellidoUsuario(apellido);
            usuario.setDniUsuario(dni);
            usuario.setEmailUsuario(email);
            usuario.setPasswordUsuario(encoder.encode(password));
            usuario.setRol(rol);
            usuario.setTelefono(telefono);
            usuario.setEstadoUsuario(EstadoUsuario.ACTIVO.name());
            return usuarioRepository.save(usuario);
        });
    }

    private void asegurarVerificacionKyc(VerificacionIdentidadRepository verificacionRepository,
                                         AuditoriaSeguridadRepository auditoriaRepository,
                                         Usuario usuario, String codigoKyc) {
        if (verificacionRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).isEmpty()) {
            LocalDateTime ahora = LocalDateTime.now().minusDays(5);
            VerificacionIdentidad verificacion = verificacionRepository.save(VerificacionIdentidad.builder()
                    .usuario(usuario)
                    .metodoVerificacion(MetodoVerificacion.BIOMETRIA_RENAPER)
                    .estadoVerificacion(EstadoVerificacion.VERIFICADA)
                    .codigoValidacionKyc(codigoKyc)
                    .observacionVerificacion("Identidad verificada por padrón biométrico RENAPER")
                    .fechaSolicitud(ahora)
                    .fechaVerificacion(ahora.plusMinutes(2))
                    .build());

            auditoriaRepository.save(AuditoriaSeguridad.builder()
                    .usuario(usuario)
                    .accionAuditoria("VERIFICACION_KYC")
                    .entidadAfectada("VERIFICACION_IDENTIDAD")
                    .idRegistroAfectado(verificacion.getIdVerificacion())
                    .resultadoAuditoria("EXITOSO")
                    .ipOrigen("127.0.0.1")
                    .detalleTracking("Identidad verificada para DNI " + usuario.getDniUsuario() + " (" + codigoKyc + ")")
                    .fechaAuditoria(ahora.plusMinutes(2))
                    .build());
        }
    }

    private Evento crearEventoDemo(EventoRepository eventoRepository, Usuario organizador, Categoria categoria, Ubicacion ubicacion,
                                   String titulo, String descripcion, LocalDate fecha, LocalTime hora,
                                   BigDecimal precio, int cupos, EstadoEvento estado, String imagenUrl) {
        Evento evento = new Evento();
        evento.setTituloEvento(titulo);
        evento.setDescripcionEvento(descripcion);
        evento.setFechaEvento(fecha);
        evento.setHoraEvento(hora);
        evento.setCuposTotalesEvento(cupos);
        evento.setEstadoPublicacionEvento(estado);
        evento.setImagenUrl(imagenUrl);
        evento.setOrganizador(organizador);
        evento.setCategoria(categoria);
        evento.setUbicacion(ubicacion);
        evento.setPrecioEntradaEvento(precio);
        return eventoRepository.save(evento);
    }

    private void crearCompraDemo(CompraRepository compraRepository,
                                 PagoRepository pagoRepository,
                                 ComprobanteRepository comprobanteRepository,
                                 EntradaRepository entradaRepository,
                                 PersonaRepository personaRepository,
                                 HistorialEstadoEntradaRepository historialRepository,
                                 AuditoriaSeguridadRepository auditoriaRepository,
                                 Evento evento, Usuario cliente,
                                 int cantidad, LocalDateTime fechaCompra,
                                 String refPago, String numComprobante) {
        Compra compra = Compra.builder()
                .fechaCompra(fechaCompra)
                .estadoCompra(EstadoCompra.CONFIRMADA)
                .monedaCompra("ARS")
                .cliente(cliente)
                .build();
        compra = compraRepository.save(compra);

        BigDecimal montoTotal = evento.getPrecioEntradaEvento().multiply(BigDecimal.valueOf(cantidad));

        Pago pago = pagoRepository.save(Pago.builder()
                .compra(compra)
                .proveedorPago(PasarelaPago.MERCADOPAGO.name())
                .pasarelaPago(PasarelaPago.MERCADOPAGO)
                .medioPago(MedioPago.TARJETA_CREDITO)
                .montoPago(montoTotal)
                .monedaPago("ARS")
                .estadoPago(EstadoPago.APROBADO)
                .referenciaExternaPago(refPago)
                .ultimosCuatroDigitos("4242")
                .fechaCreacionPago(fechaCompra)
                .fechaConfirmacionPago(fechaCompra)
                .fechaPago(fechaCompra)
                .respuestaProveedorPago("medio=TARJETA_CREDITO|last4=4242")
                .build());

        comprobanteRepository.save(Comprobante.builder()
                .pago(pago)
                .numeroComprobante(numComprobante)
                .tipoComprobante(TipoComprobante.RECIBO_DIGITAL)
                .hashCruceSeguridad(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""))
                .estadoEnvioComprobante(EstadoEnvio.PENDIENTE)
                .fechaEmisionComprobante(fechaCompra)
                .build());

        String nombreTitular = (cliente.getNombreUsuario() + " " + cliente.getApellidoUsuario()).trim();
        for (int i = 0; i < cantidad; i++) {
            final int indice = i;
            long dniLong = Long.parseLong(cliente.getDniUsuario()) + indice;
            String dniNominal = String.valueOf(dniLong);
            String nombreNominal = indice == 0
                    ? nombreTitular
                    : nombreTitular + " (Acompañante " + indice + ")";

            Persona personaTitular = personaRepository.findByDniPersona(dniLong)
                    .orElseGet(() -> personaRepository.save(Persona.builder()
                            .dniPersona(dniLong)
                            .nombrePersona(cliente.getNombreUsuario())
                            .apellidoPersona(indice == 0 ? cliente.getApellidoUsuario() : "Acompañante " + indice)
                            .emailPersona(cliente.getEmailUsuario())
                            .build()));

            Entrada entrada = Entrada.builder()
                    .estadoEntrada(EstadoEntrada.CONFIRMADA)
                    .precioPagado(evento.getPrecioEntradaEvento())
                    .monedaEntrada("ARS")
                    .fechaEmision(fechaCompra)
                    .evento(evento)
                    .tipoEntrada(evento.obtenerTipoEntradaPrincipal())
                    .personaTitular(personaTitular)
                    .cliente(cliente)
                    .compra(compra)
                    .dniAsistente(dniNominal)
                    .nombreAsistente(nombreNominal)
                    .build();
            entrada = entradaRepository.save(entrada);
            historialRepository.save(HistorialEstadoEntrada.builder()
                    .entrada(entrada)
                    .estadoNuevoEntrada(EstadoEntrada.CONFIRMADA)
                    .usuarioResponsable(cliente)
                    .motivoCambioEntrada("Entrada nominal emitida · Comprobante " + numComprobante)
                    .fechaCambioEntrada(fechaCompra)
                    .build());
        }

        auditoriaRepository.save(AuditoriaSeguridad.builder()
                .usuario(cliente)
                .accionAuditoria("CRUCE_COMPRA_EXITOSO")
                .entidadAfectada("COMPRA")
                .idRegistroAfectado(compra.getId())
                .resultadoAuditoria("EXITOSO")
                .ipOrigen("127.0.0.1")
                .detalleTracking("Cruce de compra #" + compra.getId() + " con pago " + refPago + " y comprobante " + numComprobante)
                .fechaAuditoria(fechaCompra)
                .build());
    }

    private void crearNotificacionDemo(NotificacionRepository notificacionRepository, Usuario usuario, Evento evento,
                                       String mensaje, String tipo, boolean leida, LocalDateTime fecha) {
        Notificacion notificacion = Notificacion.builder()
                .usuario(usuario)
                .evento(evento)
                .mensaje(mensaje)
                .tipo(tipo)
                .leida(leida)
                .fechaCreacion(fecha)
                .build();
        notificacionRepository.save(notificacion);
    }
}
