package com.demo.butaca.service;

import com.demo.butaca.dto.AsistenteNominalDTO;
import com.demo.butaca.dto.CompraRequestDTO;
import com.demo.butaca.dto.CompraResponseDTO;
import com.demo.butaca.dto.ComprobanteResponseDTO;
import com.demo.butaca.dto.EntradaResponseDTO;
import com.demo.butaca.dto.NotificacionDTO;
import com.demo.butaca.dto.PagoResponseDTO;
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
import com.demo.butaca.model.Pago;
import com.demo.butaca.model.PasarelaPago;
import com.demo.butaca.model.Persona;
import com.demo.butaca.model.Rol;
import com.demo.butaca.model.TipoComprobante;
import com.demo.butaca.model.TipoEntrada;
import com.demo.butaca.model.Usuario;
import com.demo.butaca.model.VerificacionIdentidad;
import com.demo.butaca.repository.CompraRepository;
import com.demo.butaca.repository.ComprobanteRepository;
import com.demo.butaca.repository.EntradaRepository;
import com.demo.butaca.repository.EventoRepository;
import com.demo.butaca.repository.HistorialEstadoEntradaRepository;
import com.demo.butaca.repository.PagoRepository;
import com.demo.butaca.repository.PersonaRepository;
import com.demo.butaca.repository.RolRepository;
import com.demo.butaca.repository.UsuarioRepository;
import com.demo.butaca.repository.VerificacionIdentidadRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CompraService {

    private static final EnumSet<EstadoEntrada> ESTADOS_QUE_OCUPAN_CUPO =
            EnumSet.of(EstadoEntrada.CONFIRMADA, EstadoEntrada.USADA);

    private final CompraRepository compraRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final EntradaRepository entradaRepository;
    private final HistorialEstadoEntradaRepository historialRepository;
    private final VerificacionIdentidadRepository verificacionRepository;
    private final PagoRepository pagoRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final EntradaService entradaService;
    private final NotificacionService notificacionService;
    private final AuditoriaSeguridadService auditoriaService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CompraService(CompraRepository compraRepository,
                         EventoRepository eventoRepository,
                         UsuarioRepository usuarioRepository,
                         PersonaRepository personaRepository,
                         RolRepository rolRepository,
                         EntradaRepository entradaRepository,
                         HistorialEstadoEntradaRepository historialRepository,
                         VerificacionIdentidadRepository verificacionRepository,
                         PagoRepository pagoRepository,
                         ComprobanteRepository comprobanteRepository,
                         EntradaService entradaService,
                         NotificacionService notificacionService,
                         AuditoriaSeguridadService auditoriaService) {
        this.compraRepository = compraRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.rolRepository = rolRepository;
        this.entradaRepository = entradaRepository;
        this.historialRepository = historialRepository;
        this.verificacionRepository = verificacionRepository;
        this.pagoRepository = pagoRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.entradaService = entradaService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public CompraResponseDTO comprarEntrada(CompraRequestDTO request) {
        int cantidad = request.getCantidad() == null ? 1 : request.getCantidad();
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        Usuario cliente = resolverClienteCompra(request);

        if (!EstadoUsuario.ACTIVO.equals(cliente.getEstadoUsuario())) {
            throw new IllegalStateException("La cuenta de usuario no se encuentra activa");
        }

        // 1. Cruce obligatorio de Verificación de Identidad (KYC) antes de comprar
        VerificacionIdentidad verificacion = verificacionRepository.findByUsuarioIdUsuario(cliente.getIdUsuario())
                .orElse(null);
        if (verificacion == null || !EstadoVerificacion.VERIFICADA.equals(verificacion.getEstadoVerificacion())) {
            auditoriaService.registrarEvento(
                    cliente,
                    "CRUCE_COMPRA_BLOQUEADO",
                    "VERIFICACION_IDENTIDAD",
                    cliente.getIdUsuario(),
                    "BLOQUEADO",
                    "Intento de compra rechazado: el usuario no posee identidad verificada (KYC obligatorio)"
            );
            throw new IllegalStateException("Es obligatorio verificar su identidad en la aplicación antes de comprar entradas");
        }

        // 2. Cruce de Evento y Disponibilidad
        Evento evento = eventoRepository.findByIdParaCompra(request.getEventoId())
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        if (!EstadoEvento.PUBLICADO.equals(evento.getEstadoPublicacionEvento())) {
            throw new IllegalStateException("El evento no esta publicado");
        }

        long ocupadas = entradaRepository.countByEventoIdEventoAndEstadoEntradaIn(
                evento.getIdEvento(), ESTADOS_QUE_OCUPAN_CUPO);
        if (evento.getCuposTotalesEvento() - ocupadas < cantidad) {
            throw new IllegalStateException("Cupos insuficientes");
        }
        evento.setVersion((evento.getVersion() == null ? 0 : evento.getVersion()) + 1);

        LocalDateTime fechaCompra = LocalDateTime.now();

        Compra compra = Compra.builder()
                .fechaCompra(fechaCompra)
                .estadoCompra(EstadoCompra.CONFIRMADA)
                .monedaCompra("ARS")
                .cliente(cliente)
                .build();
        compra = compraRepository.save(compra);

        BigDecimal precioUnitario = evento.getPrecioEntradaEvento();
        BigDecimal montoTotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));

        // 3. Cruce con Pasarela de Pago (tabla pago de DrawDB)
        PasarelaPago pasarela = parsearPasarela(request.getPasarelaPago());
        MedioPago medio = parsearMedioPago(request.getMedioPago());
        String ultimosCuatro = sanitizarUltimosCuatro(request.getUltimosCuatroDigitos());
        String refPasarela = "PAY-" + pasarela.name().substring(0, 2) + "-"
                + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        Pago pago = Pago.builder()
                .compra(compra)
                .proveedorPago(pasarela.name())
                .pasarelaPago(pasarela)
                .medioPago(medio)
                .montoPago(montoTotal.compareTo(BigDecimal.ZERO) > 0 ? montoTotal : BigDecimal.ONE)
                .monedaPago("ARS")
                .estadoPago(EstadoPago.APROBADO)
                .referenciaExternaPago(refPasarela)
                .ultimosCuatroDigitos(ultimosCuatro)
                .fechaCreacionPago(fechaCompra)
                .fechaConfirmacionPago(fechaCompra)
                .fechaPago(fechaCompra)
                .respuestaProveedorPago("medio=" + medio.name() + "|last4=" + ultimosCuatro)
                .build();
        pago = pagoRepository.save(pago);

        // 4. Emisión de Entradas Nominales vinculadas a persona_titular y tipo_entrada (DrawDB)
        String dniComprador = cliente.getDniUsuario() != null ? cliente.getDniUsuario() : "40000000";
        String nombreComprador = (cliente.getNombreUsuario() + " " + cliente.getApellidoUsuario()).trim();
        List<AsistenteNominalDTO> asistentesReq = request.getAsistentes() == null ? List.of() : request.getAsistentes();
        TipoEntrada tipoEntrada = evento.obtenerTipoEntradaPrincipal();

        List<Entrada> entradas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            AsistenteNominalDTO asistente = i < asistentesReq.size() ? asistentesReq.get(i) : null;
            String dniNominal = (asistente != null && asistente.getDniAsistente() != null && !asistente.getDniAsistente().isBlank())
                    ? asistente.getDniAsistente().trim()
                    : dniComprador;
            String nombreNominal = (asistente != null && asistente.getNombreAsistente() != null && !asistente.getNombreAsistente().isBlank())
                    ? asistente.getNombreAsistente().trim()
                    : nombreComprador;

            Persona personaTitular = resolverPersonaTitular(dniNominal, nombreNominal, cliente.getEmailUsuario());

            entradas.add(Entrada.builder()
                    .codigoUnicoEntrada(UUID.randomUUID())
                    .estadoEntrada(EstadoEntrada.CONFIRMADA)
                    .precioPagado(precioUnitario)
                    .monedaEntrada("ARS")
                    .fechaEmision(fechaCompra)
                    .evento(evento)
                    .tipoEntrada(tipoEntrada)
                    .personaTitular(personaTitular)
                    .cliente(cliente)
                    .compra(compra)
                    .dniAsistente(dniNominal)
                    .nombreAsistente(nombreNominal)
                    .build());
        }
        entradas = entradaRepository.saveAll(entradas);

        // 5. Cruce de Información: Comprobante con Hash SHA-256 + Historial + Envío Digital
        String uuidsConcatenados = entradas.stream()
                .map(e -> e.getCodigoUnicoEntrada().toString())
                .collect(Collectors.joining(","));
        String payloadCruce = "COMPRA=" + compra.getId()
                + "|DNI=" + dniComprador
                + "|KYC=" + verificacion.getCodigoValidacionKyc()
                + "|PAGO=" + pago.getReferenciaExternaPago()
                + "|TICKETS=" + uuidsConcatenados
                + "|TS=" + fechaCompra;
        String hashCruceSha256 = calcularSha256(payloadCruce);
        String numeroComprobante = String.format("BUT-%d-%06d", fechaCompra.getYear(), compra.getId());

        Comprobante comprobante = Comprobante.builder()
                .pago(pago)
                .numeroComprobante(numeroComprobante)
                .tipoComprobante(parsearTipoComprobante(request.getTipoComprobante()))
                .hashCruceSeguridad(hashCruceSha256)
                .estadoEnvioComprobante(EstadoEnvio.ENVIADO)
                .fechaEmisionComprobante(fechaCompra)
                .build();
        comprobante = comprobanteRepository.save(comprobante);

        List<HistorialEstadoEntrada> historial = entradas.stream()
                .map(entrada -> HistorialEstadoEntrada.builder()
                        .entrada(entrada)
                        .estadoNuevoEntrada(EstadoEntrada.CONFIRMADA)
                        .usuarioResponsable(cliente)
                        .motivoCambioEntrada("Entrada nominal emitida para DNI " + entrada.getDniAsistente()
                                + " · Comprobante " + numeroComprobante)
                        .fechaCambioEntrada(fechaCompra)
                        .build())
                .toList();
        historialRepository.saveAll(historial);

        compra.setEntradas(entradas);
        registrarNotificacionCruceCompra(compra, evento, pago, comprobante);

        // 6. Tracking en Auditoría de Seguridad Informática
        auditoriaService.registrarEvento(
                cliente,
                "CRUCE_COMPRA_EXITOSO",
                "COMPRA",
                compra.getId(),
                "EXITOSO",
                "Cruce verificado: KYC=" + verificacion.getCodigoValidacionKyc()
                        + ", Pago=" + pago.getReferenciaExternaPago()
                        + ", Comprobante=" + comprobante.getNumeroComprobante()
                        + ", Entradas=" + entradas.size()
        );

        return toDTO(compra, pago, comprobante);
    }

    private Persona resolverPersonaTitular(String dniStr, String nombreCompleto, String emailDefault) {
        String digitos = dniStr == null ? "" : dniStr.replaceAll("\\D", "");
        Long dniLong = digitos.isEmpty() ? Math.abs(UUID.randomUUID().getMostSignificantBits() % 90000000L) + 10000000L : Long.parseLong(digitos);
        return personaRepository.findByDniPersona(dniLong).orElseGet(() -> {
            String limpio = nombreCompleto == null || nombreCompleto.isBlank() ? "Titular Nominal" : nombreCompleto.trim();
            int espacio = limpio.indexOf(' ');
            String nombre = espacio > 0 ? limpio.substring(0, espacio).trim() : limpio;
            String apellido = espacio > 0 ? limpio.substring(espacio + 1).trim() : "Titular";
            return personaRepository.save(Persona.builder()
                    .dniPersona(dniLong)
                    .nombrePersona(nombre)
                    .apellidoPersona(apellido)
                    .emailPersona(emailDefault)
                    .build());
        });
    }

    @Transactional(readOnly = true)
    public CompraResponseDTO obtenerCompra(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));
        return toDTO(compra);
    }

    @Transactional(readOnly = true)
    public List<CompraResponseDTO> listarComprasUsuario(Long usuarioId) {
        return compraRepository.findByUsuarioIdOrderByFechaCompraDesc(usuarioId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<CompraResponseDTO> listarComprasOrganizador(Long organizadorId) {
        return compraRepository.findByOrganizadorIdOrderByFechaCompraDesc(organizadorId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompraResponseDTO> listarTodas() {
        return compraRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getFechaCompra().compareTo(a.getFechaCompra()))
                .map(this::toDTO)
                .toList();
    }

    private void registrarNotificacionCruceCompra(Compra compra, Evento evento, Pago pago, Comprobante comprobante) {
        notificacionService.crearNotificacion(NotificacionDTO.builder()
                .usuarioId(compra.getCliente().getIdUsuario())
                .eventoId(evento.getIdEvento())
                .mensaje("Compra confirmada para " + evento.getTituloEvento()
                        + ". Comprobante " + comprobante.getNumeroComprobante()
                        + " (Pago " + pago.getReferenciaExternaPago() + ") y entradas nominales enviadas a "
                        + compra.getCliente().getEmailUsuario())
                .tipo("COMPROBANTE")
                .fechaEnvio(LocalDateTime.now())
                .build());
    }

    public CompraResponseDTO toDTO(Compra compra) {
        Pago pago = pagoRepository.findFirstByCompraIdOrderByFechaCreacionPagoDesc(compra.getId()).orElse(null);
        Comprobante comprobante = pago == null ? null : comprobanteRepository.findByPagoIdPago(pago.getIdPago()).orElse(null);
        return toDTO(compra, pago, comprobante);
    }

    public CompraResponseDTO toDTO(Compra compra, Pago pago, Comprobante comprobante) {
        List<Entrada> entidades = compra.getEntradas() == null ? List.of() : compra.getEntradas();
        List<EntradaResponseDTO> entradas = entidades.stream().map(entradaService::toDTO).toList();
        BigDecimal total = entidades.stream()
                .map(Entrada::getPrecioPagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Evento evento = entidades.stream().map(Entrada::getEvento).findFirst().orElse(null);

        PagoResponseDTO pagoDTO = pago == null ? null : PagoResponseDTO.builder()
                .idPago(pago.getIdPago())
                .compraId(compra.getId())
                .pasarelaPago(pago.getPasarelaPago().name())
                .medioPago(pago.getMedioPago().name())
                .estadoPago(pago.getEstadoPago().name())
                .referenciaExternaPago(pago.getReferenciaExternaPago())
                .ultimosCuatroDigitos(pago.getUltimosCuatroDigitos())
                .fechaPago(pago.getFechaPago())
                .build();

        ComprobanteResponseDTO comprobanteDTO = comprobante == null ? null : ComprobanteResponseDTO.builder()
                .idComprobante(comprobante.getIdComprobante())
                .pagoId(pago == null ? null : pago.getIdPago())
                .numeroComprobante(comprobante.getNumeroComprobante())
                .tipoComprobante(comprobante.getTipoComprobante().name())
                .hashCruceSeguridad(comprobante.getHashCruceSeguridad())
                .estadoEnvioComprobante(comprobante.getEstadoEnvioComprobante().name())
                .fechaEmisionComprobante(comprobante.getFechaEmisionComprobante())
                .build();

        return CompraResponseDTO.builder()
                .id(compra.getId())
                .fechaCompra(compra.getFechaCompra())
                .cantidad(entradas.size())
                .total(total)
                .clienteId(compra.getCliente() == null ? null : compra.getCliente().getIdUsuario())
                .clienteEmail(compra.getCliente() == null ? null : compra.getCliente().getEmailUsuario())
                .clienteDni(compra.getCliente() == null ? null : compra.getCliente().getDniUsuario())
                .eventoId(evento == null ? null : evento.getIdEvento())
                .eventoTitulo(evento == null ? null : evento.getTituloEvento())
                .estadoCompra(compra.getEstadoCompra().name())
                .pago(pagoDTO)
                .comprobante(comprobanteDTO)
                .entradas(entradas)
                .build();
    }

    private PasarelaPago parsearPasarela(String valor) {
        if (valor == null || valor.isBlank()) {
            return PasarelaPago.MERCADOPAGO;
        }
        try {
            return PasarelaPago.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PasarelaPago.MERCADOPAGO;
        }
    }

    private MedioPago parsearMedioPago(String valor) {
        if (valor == null || valor.isBlank()) {
            return MedioPago.TARJETA_CREDITO;
        }
        try {
            return MedioPago.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MedioPago.TARJETA_CREDITO;
        }
    }

    private TipoComprobante parsearTipoComprobante(String valor) {
        if (valor == null || valor.isBlank()) {
            return TipoComprobante.RECIBO_DIGITAL;
        }
        try {
            return TipoComprobante.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TipoComprobante.RECIBO_DIGITAL;
        }
    }

    private String sanitizarUltimosCuatro(String valor) {
        if (valor == null || valor.isBlank()) {
            return "4242";
        }
        String soloDigitos = valor.replaceAll("\\D", "");
        if (soloDigitos.length() >= 4) {
            return soloDigitos.substring(soloDigitos.length() - 4);
        }
        return "4242";
    }

    private Usuario resolverClienteCompra(CompraRequestDTO request) {
        if (request.getUsuarioId() != null) {
            Usuario porId = usuarioRepository.findById(request.getUsuarioId()).orElse(null);
            if (porId != null) {
                return porId;
            }
        }
        if (request.getEmailUsuario() != null && !request.getEmailUsuario().isBlank()) {
            String email = request.getEmailUsuario().trim().toLowerCase();
            Usuario porEmail = usuarioRepository.findByEmailUsuarioIgnoreCase(email).orElse(null);
            if (porEmail != null) {
                asegurarVerificacionRestaurada(porEmail, request.getDniUsuario());
                return porEmail;
            }
            Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                    .orElseGet(() -> rolRepository.save(Rol.builder().nombre("CLIENTE").build()));
            String nombre = request.getNombreUsuario() != null && !request.getNombreUsuario().isBlank() ? request.getNombreUsuario().trim() : "Cliente";
            String apellido = request.getApellidoUsuario() != null && !request.getApellidoUsuario().isBlank() ? request.getApellidoUsuario().trim() : "Butaca";
            Persona persona = resolverPersonaTitular(request.getDniUsuario(), nombre + " " + apellido, email);
            Usuario nuevo = new Usuario();
            nuevo.setPersona(persona);
            nuevo.setNombreUsuario(nombre);
            nuevo.setApellidoUsuario(apellido);
            nuevo.setEmailUsuario(email);
            nuevo.setDniUsuario(request.getDniUsuario());
            nuevo.setPasswordUsuario(passwordEncoder.encode("cliente123"));
            nuevo.setRol(rolCliente);
            nuevo.setEstadoUsuario(EstadoUsuario.ACTIVO.name());
            Usuario guardado = usuarioRepository.save(nuevo);
            asegurarVerificacionRestaurada(guardado, request.getDniUsuario());
            return guardado;
        }
        throw new IllegalArgumentException("Usuario no encontrado");
    }

    private void asegurarVerificacionRestaurada(Usuario usuario, String dni) {
        if (dni != null && !dni.isBlank() && verificacionRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).isEmpty()) {
            LocalDateTime ahora = LocalDateTime.now();
            verificacionRepository.save(VerificacionIdentidad.builder()
                    .usuario(usuario)
                    .metodoVerificacion(MetodoVerificacion.BIOMETRIA_RENAPER)
                    .estadoVerificacion(EstadoVerificacion.VERIFICADA)
                    .codigoValidacionKyc("KYC-REN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .observacionVerificacion("Sesión de identidad verificada restaurada")
                    .fechaSolicitud(ahora)
                    .fechaVerificacion(ahora)
                    .build());
        }
    }

    private String calcularSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
