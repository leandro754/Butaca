package com.demo.butaca.service;

import com.demo.butaca.dto.AsistenteNominalDTO;
import com.demo.butaca.dto.CompraRequestDTO;
import com.demo.butaca.dto.CompraResponseDTO;
import com.demo.butaca.dto.ComprobanteResponseDTO;
import com.demo.butaca.dto.EntradaResponseDTO;
import com.demo.butaca.dto.NotificacionDTO;
import com.demo.butaca.dto.PagoResponseDTO;
import com.demo.butaca.dto.VerificacionCruceDTO;
import com.demo.butaca.integration.pago.PagoGateway;
import com.demo.butaca.integration.pago.PagoRechazadoException;
import com.demo.butaca.integration.pago.ResultadoPago;
import com.demo.butaca.integration.pago.SolicitudPago;
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
import com.demo.butaca.model.Pago;
import com.demo.butaca.model.PasarelaPago;
import com.demo.butaca.model.TipoComprobante;
import com.demo.butaca.model.Usuario;
import com.demo.butaca.model.Persona;
import com.demo.butaca.model.TipoEntrada;
import com.demo.butaca.model.VerificacionIdentidad;
import com.demo.butaca.repository.CompraRepository;
import com.demo.butaca.repository.ComprobanteRepository;
import com.demo.butaca.repository.EntradaRepository;
import com.demo.butaca.repository.EventoRepository;
import com.demo.butaca.repository.HistorialEstadoEntradaRepository;
import com.demo.butaca.repository.PagoRepository;
import com.demo.butaca.repository.UsuarioRepository;
import com.demo.butaca.repository.PersonaRepository;
import com.demo.butaca.repository.VerificacionIdentidadRepository;
import com.demo.butaca.util.CruceHashUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class CompraService {

    private static final EnumSet<EstadoEntrada> ESTADOS_QUE_OCUPAN_CUPO =
            EnumSet.of(EstadoEntrada.CONFIRMADA, EstadoEntrada.USADA);

    private final CompraRepository compraRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final EntradaRepository entradaRepository;
    private final HistorialEstadoEntradaRepository historialRepository;
    private final VerificacionIdentidadRepository verificacionRepository;
    private final PagoRepository pagoRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final EntradaService entradaService;
    private final NotificacionService notificacionService;
    private final AuditoriaSeguridadService auditoriaService;
    private final PagoGateway pagoGateway;

    public CompraService(CompraRepository compraRepository,
                         EventoRepository eventoRepository,
                         UsuarioRepository usuarioRepository,
                         PersonaRepository personaRepository,
                         EntradaRepository entradaRepository,
                         HistorialEstadoEntradaRepository historialRepository,
                         VerificacionIdentidadRepository verificacionRepository,
                         PagoRepository pagoRepository,
                         ComprobanteRepository comprobanteRepository,
                         EntradaService entradaService,
                         NotificacionService notificacionService,
                         AuditoriaSeguridadService auditoriaService,
                         PagoGateway pagoGateway) {
        this.compraRepository = compraRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.entradaRepository = entradaRepository;
        this.historialRepository = historialRepository;
        this.verificacionRepository = verificacionRepository;
        this.pagoRepository = pagoRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.entradaService = entradaService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.pagoGateway = pagoGateway;
    }

    /**
     * Cruza identidad, evento, precio, pago y datos nominales en una sola
     * transacción. El pago se procesa mediante un adaptador y el monto que
     * devuelve la pasarela debe coincidir exactamente con el total calculado
     * en el servidor.
     */
    @Transactional
    public CompraResponseDTO comprarEntrada(CompraRequestDTO request) {
        int cantidad = request.getCantidad() == null ? 1 : request.getCantidad();
        if (request.getUsuarioId() == null) {
            throw new IllegalArgumentException("El usuario es obligatorio para comprar");
        }

        Usuario cliente = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (cantidad <= 0) {
            auditarBloqueo(cliente, "Compra rechazada: la cantidad debe ser mayor a cero");
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        if (!EstadoUsuario.ACTIVO.equals(cliente.getEstadoUsuario())) {
            auditarBloqueo(cliente, "La cuenta del comprador no está activa");
            throw new IllegalStateException("La cuenta de usuario no se encuentra activa");
        }

        validarDatosComprador(request, cliente);

        VerificacionIdentidad verificacion = verificacionRepository.findByUsuarioIdUsuario(cliente.getIdUsuario())
                .orElse(null);
        if (verificacion == null || !EstadoVerificacion.VERIFICADA.equals(verificacion.getEstadoVerificacion())) {
            auditarBloqueo(cliente, "Compra rechazada: el usuario no posee identidad verificada (KYC obligatorio)");
            throw new IllegalStateException("Es obligatorio verificar su identidad en la aplicación antes de comprar entradas");
        }

        Evento evento = eventoRepository.findByIdParaCompra(request.getEventoId())
                .orElseThrow(() -> {
                    auditarBloqueo(cliente, "Compra rechazada: evento inexistente");
                    return new IllegalArgumentException("Evento no encontrado");
                });

        if (!EstadoEvento.PUBLICADO.equals(evento.getEstadoPublicacionEvento())) {
            auditarBloqueo(cliente, "Compra rechazada: el evento no está publicado");
            throw new IllegalStateException("El evento no esta publicado");
        }
        if (evento.getPrecioEntradaEvento() == null
                || evento.getPrecioEntradaEvento().compareTo(BigDecimal.ZERO) <= 0) {
            auditarBloqueo(cliente, "Compra rechazada: el evento no tiene un precio válido");
            throw new IllegalStateException("El evento no tiene un precio válido");
        }

        long ocupadas = entradaRepository.countByEventoIdEventoAndEstadoEntradaIn(
                evento.getIdEvento(), ESTADOS_QUE_OCUPAN_CUPO);
        if (evento.getCuposTotalesEvento() - ocupadas < cantidad) {
            auditarBloqueo(cliente, "Compra rechazada: cupos insuficientes");
            throw new IllegalStateException("Cupos insuficientes");
        }

        List<AsistenteValidado> asistentes;
        try {
            asistentes = validarAsistentes(request.getAsistentes(), cantidad);
        } catch (IllegalArgumentException ex) {
            auditarBloqueo(cliente, "Compra rechazada: " + ex.getMessage());
            throw ex;
        }
        TipoEntrada tipoEntrada = request.getTipoEntradaId() == null ? evento.obtenerTipoEntradaPrincipal()
                : evento.getTiposEntrada().stream()
                .filter(tipo -> request.getTipoEntradaId().equals(tipo.getIdTipoEntrada()) && Boolean.TRUE.equals(tipo.getActivoTipoEntrada()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("El tipo de entrada no está disponible para este evento"));
        if (tipoEntrada == null) throw new IllegalStateException("El evento no tiene entradas activas");
        BigDecimal precioUnitario = tipoEntrada.getPrecioTipoEntrada();
        BigDecimal total = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        LocalDateTime fechaCompra = LocalDateTime.now().withNano(0);

        Compra compra = compraRepository.save(Compra.builder()
                .fechaCompra(fechaCompra)
                .estadoCompra(EstadoCompra.PENDIENTE)
                .monedaCompra("ARS")
                .cliente(cliente)
                .build());

        PasarelaPago pasarela = parsearPasarela(request.getPasarelaPago());
        MedioPago medio = parsearMedioPago(request.getMedioPago());
        String ultimosCuatro = sanitizarUltimosCuatro(request.getUltimosCuatroDigitos());

        SolicitudPago solicitudPago = new SolicitudPago(
                compra.getId(),
                cliente.getIdUsuario(),
                total,
                "ARS",
                pasarela.name(),
                medio.name(),
                ultimosCuatro,
                request.getTokenPago(),
                UUID.randomUUID().toString()
        );

        ResultadoPago resultadoPago;
        try {
            resultadoPago = pagoGateway.procesar(solicitudPago);
        } catch (RuntimeException ex) {
            auditarBloqueo(cliente, "Error al comunicarse con la pasarela de pago: " + ex.getMessage());
            throw new IllegalStateException("No se pudo procesar el pago. Intentá nuevamente.");
        }

        if (resultadoPago == null) {
            auditarBloqueo(cliente, "La pasarela de pago no devolvió un resultado");
            throw new IllegalStateException("La pasarela de pago no devolvió un resultado");
        }

        if (!resultadoPago.aprobado()) {
            auditarBloqueo(cliente, "Pago rechazado por la pasarela: " + resultadoPago.mensaje()
                    + " (ref. " + resultadoPago.referenciaExterna() + ")");
            throw new PagoRechazadoException(resultadoPago.mensaje());
        }

        if (resultadoPago.monto() == null || resultadoPago.monto().compareTo(total) != 0) {
            auditarBloqueo(cliente, "El monto aprobado por la pasarela no coincide con el total calculado");
            throw new IllegalStateException("El monto aprobado no coincide con el total de la compra");
        }
        if (resultadoPago.moneda() == null || !"ARS".equalsIgnoreCase(resultadoPago.moneda().trim())) {
            auditarBloqueo(cliente, "La moneda aprobada por la pasarela no coincide con ARS");
            throw new IllegalStateException("La moneda aprobada no coincide con la moneda de la compra");
        }
        if (resultadoPago.referenciaExterna() == null || resultadoPago.referenciaExterna().isBlank()) {
            auditarBloqueo(cliente, "La pasarela de pago no devolvió una referencia externa");
            throw new IllegalStateException("La pasarela de pago no devolvió una referencia externa");
        }

        Pago pago = pagoRepository.save(Pago.builder()
                .compra(compra)
                .pasarelaPago(pasarela)
                .medioPago(medio)
                .estadoPago(EstadoPago.APROBADO)
                .referenciaExternaPago(resultadoPago.referenciaExterna())
                .ultimosCuatroDigitos(ultimosCuatro)
                .montoPago(total)
                .monedaPago("ARS")
                .fechaCreacionPago(fechaCompra)
                .fechaConfirmacionPago(fechaCompra)
                .respuestaProveedorPago("medio=" + medio.name() + "|last4=" + ultimosCuatro
                        + "|auth=" + resultadoPago.codigoAutorizacion())
                .fechaPago(fechaCompra)
                .build());

        List<Entrada> entradas = asistentes.stream()
                .map(asistente -> Entrada.builder()
                        .codigoUnicoEntrada(UUID.randomUUID())
                        .estadoEntrada(EstadoEntrada.CONFIRMADA)
                        .precioPagado(precioUnitario)
                        .monedaEntrada("ARS")
                        .tipoEntrada(tipoEntrada)
                        .personaTitular(resolverPersonaTitular(asistente.dni(), asistente.nombre(), cliente.getEmailUsuario()))
                        .fechaEmision(fechaCompra)
                        .evento(evento)
                        .cliente(cliente)
                        .compra(compra)
                        .dniAsistente(asistente.dni())
                        .nombreAsistente(asistente.nombre())
                        .build())
                .toList();
        entradas = entradaRepository.saveAll(entradas);

        compra.setEntradas(entradas);
        compra.setEstadoCompra(EstadoCompra.CONFIRMADA);
        compraRepository.save(compra);

        String hashCruceSha256 = calcularHashCruce(compra, verificacion, pago, entradas);
        String numeroComprobante = String.format("BUT-%d-%06d", fechaCompra.getYear(), compra.getId());

        Comprobante comprobante = comprobanteRepository.save(Comprobante.builder()
                .pago(pago)
                .numeroComprobante(numeroComprobante)
                .tipoComprobante(parsearTipoComprobante(request.getTipoComprobante()))
                .hashCruceSeguridad(hashCruceSha256)
                .estadoEnvioComprobante(EstadoEnvio.PENDIENTE)
                .fechaEmisionComprobante(fechaCompra)
                .build());

        List<HistorialEstadoEntrada> historial = entradas.stream()
                .map(entrada -> HistorialEstadoEntrada.builder()
                        .entrada(entrada)
                        .estadoNuevoEntrada(EstadoEntrada.CONFIRMADA)
                        .usuarioResponsable(cliente)
                        .motivoCambioEntrada("Entrada nominal emitida para DNI " + entrada.getDniAsistente()
                                + " - Comprobante " + numeroComprobante)
                        .fechaCambioEntrada(fechaCompra)
                        .build())
                .toList();
        historialRepository.saveAll(historial);

        registrarNotificacionCruceCompra(compra, evento, pago, comprobante);

        auditoriaService.registrarEvento(
                cliente,
                "CRUCE_COMPRA_EXITOSO",
                "COMPRA",
                compra.getId(),
                "EXITOSO",
                "Cruce verificado: KYC=" + verificacion.getCodigoValidacionKyc()
                        + ", Pago=" + pago.getReferenciaExternaPago()
                        + ", Monto=" + total.toPlainString() + " ARS"
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

    /**
     * Recalcula el hash SHA-256 y los cruces de precio, pago y datos
     * nominales. Sirve para auditar un comprobante ya emitido.
     */
    @Transactional(readOnly = true)
    public VerificacionCruceDTO verificarCruce(Long compraId) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));
        Pago pago = pagoRepository.findFirstByCompraIdOrderByFechaCreacionPagoDesc(compraId).orElse(null);
        Comprobante comprobante = pago == null ? null : comprobanteRepository.findByPagoIdPago(pago.getIdPago()).orElse(null);
        VerificacionIdentidad verificacion = verificacionRepository
                .findByUsuarioIdUsuario(compra.getCliente().getIdUsuario()).orElse(null);
        List<Entrada> entradas = compra.getEntradas() == null ? List.of() : compra.getEntradas();

        BigDecimal totalEntradas = entradas.stream()
                .map(Entrada::getPrecioPagado)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal precioUnitario = entradas.isEmpty() ? null : entradas.get(0).getPrecioPagado();

        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("compra_confirmada", EstadoCompra.CONFIRMADA.equals(compra.getEstadoCompra()));
        checks.put("kyc_verificada", verificacion != null
                && EstadoVerificacion.VERIFICADA.equals(verificacion.getEstadoVerificacion()));
        checks.put("pago_aprobado", pago != null && EstadoPago.APROBADO.equals(pago.getEstadoPago()));
        checks.put("monto_pago_coincide", pago != null && pago.getMonto() != null
                && pago.getMonto().compareTo(totalEntradas) == 0);
        checks.put("precio_unitario_coincide", precioUnitario != null
                && precioUnitario.compareTo(BigDecimal.ZERO) > 0
                && entradas.stream().allMatch(entrada -> entrada.getPrecioPagado() != null
                && entrada.getPrecioPagado().compareTo(precioUnitario) == 0)
                && pago != null && pago.getMonto() != null
                && pago.getMonto().compareTo(precioUnitario.multiply(BigDecimal.valueOf(entradas.size()))) == 0);
        checks.put("entradas_nominales", !entradas.isEmpty() && entradas.stream().allMatch(entrada ->
                entrada.getDniAsistente() != null && !entrada.getDniAsistente().isBlank()
                        && entrada.getNombreAsistente() != null && !entrada.getNombreAsistente().isBlank()));

        String hashEsperado = null;
        if (pago != null && verificacion != null && !entradas.isEmpty()) {
            hashEsperado = calcularHashCruce(compra, verificacion, pago, entradas);
        }
        checks.put("hash_comprobante_coincide", comprobante != null && hashEsperado != null
                && hashEsperado.equals(comprobante.getHashCruceSeguridad()));

        boolean valida = checks.values().stream().allMatch(Boolean::booleanValue);
        return VerificacionCruceDTO.builder()
                .compraId(compraId)
                .valida(valida)
                .hashEsperado(hashEsperado)
                .hashAlmacenado(comprobante == null ? null : comprobante.getHashCruceSeguridad())
                .checks(checks)
                .observacion(valida
                        ? "El comprobante, el pago y las entradas nominales coinciden"
                        : "Se detectó al menos una inconsistencia en el cruce de datos")
                .build();
    }

    private void validarDatosComprador(CompraRequestDTO request, Usuario cliente) {
        if (request.getEmailUsuario() != null && !request.getEmailUsuario().isBlank()
                && !request.getEmailUsuario().trim().equalsIgnoreCase(cliente.getEmailUsuario())) {
            auditarBloqueo(cliente, "El email de la compra no coincide con el de la cuenta");
            throw new IllegalArgumentException("El email de la compra no coincide con el de la cuenta");
        }
        if (request.getDniUsuario() != null && !request.getDniUsuario().isBlank()) {
            String dniRequest = normalizarDni(request.getDniUsuario());
            String dniCuenta = normalizarDni(cliente.getDniUsuario());
            if (!dniCuenta.isBlank() && !dniRequest.equals(dniCuenta)) {
                auditarBloqueo(cliente, "El DNI de la compra no coincide con el DNI verificado");
                throw new IllegalArgumentException("El DNI de la compra no coincide con el DNI verificado");
            }
        }
    }

    private List<AsistenteValidado> validarAsistentes(List<AsistenteNominalDTO> asistentes, int cantidad) {
        if (asistentes == null || asistentes.size() != cantidad) {
            throw new IllegalArgumentException("Debe informar nombre y DNI de los " + cantidad + " asistentes");
        }

        Set<String> dnis = new HashSet<>();
        List<AsistenteValidado> validados = new ArrayList<>();
        for (AsistenteNominalDTO asistente : asistentes) {
            String dni = normalizarDni(asistente == null ? null : asistente.getDniAsistente());
            String nombre = asistente == null ? null : asistente.getNombreAsistente();

            if (dni.length() < 7 || dni.length() > 8) {
                throw new IllegalArgumentException("Cada asistente debe tener un DNI de 7 u 8 dígitos");
            }
            if (nombre == null || nombre.trim().length() < 3) {
                throw new IllegalArgumentException("Cada asistente debe tener nombre y apellido");
            }
            if (!dnis.add(dni)) {
                throw new IllegalArgumentException("No se puede repetir el DNI de un asistente en la misma compra");
            }
            validados.add(new AsistenteValidado(dni, nombre.trim().replaceAll("\\s+", " ")));
        }
        return validados;
    }

    private void registrarNotificacionCruceCompra(Compra compra, Evento evento, Pago pago, Comprobante comprobante) {
        notificacionService.crearNotificacion(NotificacionDTO.builder()
                .usuarioId(compra.getCliente().getIdUsuario())
                .compraId(compra.getId())
                .eventoId(evento.getIdEvento())
                .mensaje("Compra confirmada para " + evento.getTituloEvento()
                        + ". Comprobante " + comprobante.getNumeroComprobante()
                        + ". Tus entradas nominales ya están disponibles. El envío por correo está pendiente.")
                .tipo("COMPROBANTE")
                .fechaEnvio(LocalDateTime.now())
                .build());
    }

    private String calcularHashCruce(Compra compra, VerificacionIdentidad verificacion,
                                     Pago pago, List<Entrada> entradas) {
        return CruceHashUtils.calcular(compra, verificacion, pago, entradas);
    }

    private void auditarBloqueo(Usuario cliente, String motivo) {
        if (cliente != null) {
            auditoriaService.registrarEventoIndependiente(
                    cliente,
                    "CRUCE_COMPRA_BLOQUEADO",
                    "COMPRA",
                    null,
                    "BLOQUEADO",
                    motivo
            );
        }
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
                .filter(Objects::nonNull)
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
                .monto(pago.getMonto())
                .moneda(pago.getMoneda())
                .codigoAutorizacion(pago.getCodigoAutorizacion())
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
            return PasarelaPago.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return PasarelaPago.MERCADOPAGO;
        }
    }

    private MedioPago parsearMedioPago(String valor) {
        if (valor == null || valor.isBlank()) {
            return MedioPago.TARJETA_CREDITO;
        }
        try {
            return MedioPago.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return MedioPago.TARJETA_CREDITO;
        }
    }

    private TipoComprobante parsearTipoComprobante(String valor) {
        if (valor == null || valor.isBlank()) {
            return TipoComprobante.RECIBO_DIGITAL;
        }
        try {
            return TipoComprobante.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return TipoComprobante.RECIBO_DIGITAL;
        }
    }

    private String sanitizarUltimosCuatro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String soloDigitos = valor.replaceAll("\\D", "");
        return soloDigitos.length() >= 4 ? soloDigitos.substring(soloDigitos.length() - 4) : null;
    }

    private String normalizarDni(String dni) {
        return dni == null ? "" : dni.replaceAll("\\D", "");
    }

    private record AsistenteValidado(String dni, String nombre) {
    }
}
