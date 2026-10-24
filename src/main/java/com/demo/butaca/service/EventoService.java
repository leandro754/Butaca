package com.demo.butaca.service;

import com.demo.butaca.dto.EventoRequestDTO;
import com.demo.butaca.dto.EventoResponseDTO;
import com.demo.butaca.dto.NotificacionDTO;
import com.demo.butaca.model.Categoria;
import com.demo.butaca.model.Entrada;
import com.demo.butaca.model.EstadoEntrada;
import com.demo.butaca.model.EstadoEvento;
import com.demo.butaca.model.EstadoPago;
import com.demo.butaca.model.EstadoReembolso;
import com.demo.butaca.model.Evento;
import com.demo.butaca.model.HistorialEstadoEntrada;
import com.demo.butaca.model.Reembolso;
import com.demo.butaca.model.Ubicacion;
import com.demo.butaca.model.Usuario;
import com.demo.butaca.repository.CategoriaRepository;
import com.demo.butaca.repository.EntradaRepository;
import com.demo.butaca.repository.EventoRepository;
import com.demo.butaca.repository.HistorialEstadoEntradaRepository;
import com.demo.butaca.repository.PagoRepository;
import com.demo.butaca.repository.ReembolsoRepository;
import com.demo.butaca.repository.UbicacionRepository;
import com.demo.butaca.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EventoService {

    private static final EnumSet<EstadoEntrada> ESTADOS_QUE_OCUPAN_CUPO =
            EnumSet.of(EstadoEntrada.CONFIRMADA, EstadoEntrada.USADA);

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final UbicacionRepository ubicacionRepository;
    private final EntradaRepository entradaRepository;
    private final HistorialEstadoEntradaRepository historialRepository;
    private final PagoRepository pagoRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final NotificacionService notificacionService;

    public EventoService(EventoRepository eventoRepository, UsuarioRepository usuarioRepository,
                         CategoriaRepository categoriaRepository, UbicacionRepository ubicacionRepository,
                         EntradaRepository entradaRepository, HistorialEstadoEntradaRepository historialRepository,
                         PagoRepository pagoRepository, ReembolsoRepository reembolsoRepository,
                         NotificacionService notificacionService) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.entradaRepository = entradaRepository;
        this.historialRepository = historialRepository;
        this.pagoRepository = pagoRepository;
        this.reembolsoRepository = reembolsoRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public EventoResponseDTO crearEvento(EventoRequestDTO request) {
        Usuario organizador = buscarOrganizador(request.getOrganizadorIdEvento());
        Evento evento = new Evento();
        aplicarDatos(evento, request);
        evento.setEstadoPublicacionEvento(parseEstado(request.getEstadoPublicacionEvento(), EstadoEvento.BORRADOR));
        evento.setOrganizador(organizador);
        return toDTO(eventoRepository.save(evento));
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> listarEventosPublicados(Long categoriaId) {
        return eventoRepository.findByEstadoPublicacionEventoOrderByFechaEventoAsc(EstadoEvento.PUBLICADO).stream()
                .filter(evento -> categoriaId == null
                        || (evento.getCategoria() != null
                        && categoriaId.equals(evento.getCategoria().getIdCategoria())))
                .filter(evento -> calcularCuposDisponibles(evento) > 0)
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> listarEventosPublicados() {
        return listarEventosPublicados(null);
    }

    @Transactional(readOnly = true)
    public EventoResponseDTO obtenerEvento(Long id) {
        return toDTO(buscarEvento(id));
    }

    @Transactional
    public EventoResponseDTO editarEvento(Long id, EventoRequestDTO request) {
        Evento evento = buscarEvento(id);
        int entradasVigentes = contarEntradasVigentes(evento.getIdEvento());
        if (request.getCuposTotalesEvento() < entradasVigentes) {
            throw new IllegalStateException("Los cupos totales no pueden ser menores a las entradas ya emitidas");
        }

        aplicarDatos(evento, request);
        if (request.getEstadoPublicacionEvento() != null) {
            evento.setEstadoPublicacionEvento(
                    parseEstado(request.getEstadoPublicacionEvento(), evento.getEstadoPublicacionEvento()));
        }
        return toDTO(eventoRepository.save(evento));
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> listarEventosPorOrganizador(Long idOrganizador) {
        return eventoRepository.findByOrganizadorIdUsuario(idOrganizador).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> listarEventosPorCategoria(Long categoriaId) {
        return eventoRepository.findByCategoriaIdCategoria(categoriaId).stream()
                .filter(evento -> EstadoEvento.PUBLICADO.equals(evento.getEstadoPublicacionEvento()))
                .filter(evento -> calcularCuposDisponibles(evento) > 0)
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void publicarEvento(Long id) {
        Evento evento = buscarEvento(id);
        if (!Boolean.TRUE.equals(evento.getUbicacion().getActivo())) {
            throw new IllegalStateException("No se puede publicar en una ubicacion inactiva");
        }
        if (evento.getCuposTotalesEvento() <= 0
                || evento.getCuposTotalesEvento() > evento.getUbicacion().getCapacidadMaxima()) {
            throw new IllegalStateException("El evento no tiene un cupo valido para publicarse");
        }
        evento.setEstadoPublicacionEvento(EstadoEvento.PUBLICADO);
        eventoRepository.save(evento);
    }

    @Transactional
    public void cancelarEvento(Long id) {
        Evento evento = buscarEvento(id);
        evento.setEstadoPublicacionEvento(EstadoEvento.CANCELADO);
        eventoRepository.save(evento);

        List<Entrada> entradas = entradaRepository.findByEventoIdEventoAndEstadoEntrada(
                id, EstadoEntrada.CONFIRMADA);
        LocalDateTime ahora = LocalDateTime.now();

        for (Entrada entrada : entradas) {
            entrada.setEstadoEntrada(EstadoEntrada.ANULADA);
            historialRepository.save(HistorialEstadoEntrada.builder()
                    .entrada(entrada)
                    .estadoAnteriorEntrada(EstadoEntrada.CONFIRMADA)
                    .estadoNuevoEntrada(EstadoEntrada.ANULADA)
                    .motivoCambioEntrada("Evento cancelado")
                    .fechaCambioEntrada(ahora)
                    .build());
        }
        entradaRepository.saveAll(entradas);

        // Generar reembolsos sobre los pagos asociados a las compras afectadas (tabla reembolso de DrawDB)
        entradas.stream()
                .map(Entrada::getCompra)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(compra -> pagoRepository.findFirstByCompraIdOrderByFechaCreacionPagoDesc(compra.getId())
                        .ifPresent(pago -> {
                            pago.setEstadoPago(EstadoPago.REEMBOLSADO);
                            pagoRepository.save(pago);
                            reembolsoRepository.save(Reembolso.builder()
                                    .pago(pago)
                                    .montoReembolso(pago.getMontoPago())
                                    .monedaReembolso(pago.getMonedaPago())
                                    .motivoReembolso("Cancelación del evento " + evento.getTituloEvento())
                                    .estadoReembolso(EstadoReembolso.PROCESADO)
                                    .referenciaExternaReembolso("REF-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                                    .fechaSolicitudReembolso(ahora)
                                    .fechaProcesamientoReembolso(ahora)
                                    .build());
                        }));

        entradas.stream()
                .map(Entrada::getCliente)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(cliente -> notificacionService.crearNotificacion(NotificacionDTO.builder()
                        .usuarioId(cliente.getIdUsuario())
                        .eventoId(evento.getIdEvento())
                        .mensaje("El evento " + evento.getTituloEvento() + " fue cancelado")
                        .tipo("CANCELACION")
                        .fechaEnvio(ahora)
                        .build()));
    }

    public int calcularCuposDisponibles(Evento evento) {
        return Math.max(0, evento.getCuposTotalesEvento() - contarEntradasVigentes(evento.getIdEvento()));
    }

    private int contarEntradasVigentes(Long eventoId) {
        if (eventoId == null) {
            return 0;
        }
        return Math.toIntExact(
                entradaRepository.countByEventoIdEventoAndEstadoEntradaIn(eventoId, ESTADOS_QUE_OCUPAN_CUPO));
    }

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
    }

    private Usuario buscarOrganizador(Long id) {
        Usuario organizador = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organizador no encontrado"));
        String rol = organizador.getRol() == null ? null : organizador.getRol().getNombre();
        if (!"ORGANIZADOR".equals(rol) && !"ADMIN".equals(rol)) {
            throw new IllegalArgumentException("El usuario no tiene permisos para organizar eventos");
        }
        return organizador;
    }

    private void aplicarDatos(Evento evento, EventoRequestDTO request) {
        Ubicacion ubicacion = ubicacionRepository.findById(request.getUbicacionId())
                .orElseThrow(() -> new IllegalArgumentException("Ubicacion no encontrada"));
        if (!Boolean.TRUE.equals(ubicacion.getActivo())) {
            throw new IllegalStateException("La ubicacion seleccionada no esta activa");
        }
        if (request.getCuposTotalesEvento() > ubicacion.getCapacidadMaxima()) {
            throw new IllegalArgumentException("Los cupos totales no pueden superar la capacidad de la ubicacion");
        }

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada"));

        evento.setTituloEvento(request.getTituloEvento().trim());
        evento.setDescripcionEvento(request.getDescripcionEvento());
        evento.setFechaEvento(request.getFechaEvento());
        evento.setHoraEvento(request.getHoraEvento());
        evento.setCuposTotalesEvento(request.getCuposTotalesEvento());
        evento.setImagenUrl(request.getImagenUrl());
        evento.setCategoria(categoria);
        evento.setUbicacion(ubicacion);
        evento.setPrecioEntradaEvento(request.getPrecioEntradaEvento());
    }

    private EstadoEvento parseEstado(String raw, EstadoEvento defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return EstadoEvento.valueOf(raw.trim().toUpperCase());
    }

    public EventoResponseDTO toDTO(Evento evento) {
        Ubicacion ubicacion = evento.getUbicacion();
        int ocupadas = contarEntradasVigentes(evento.getIdEvento());
        return EventoResponseDTO.builder()
                .idEvento(evento.getIdEvento())
                .tituloEvento(evento.getTituloEvento())
                .descripcionEvento(evento.getDescripcionEvento())
                .ubicacionId(ubicacion == null ? null : ubicacion.getIdUbicacion())
                .ubicacionNombre(ubicacion == null ? null : ubicacion.getNombreLugar())
                .ubicacionDireccion(ubicacion == null ? null : ubicacion.getDireccion())
                .capacidadMaxima(ubicacion == null ? null : ubicacion.getCapacidadMaxima())
                .fechaEvento(evento.getFechaEvento())
                .horaEvento(evento.getHoraEvento())
                .precioEntradaEvento(evento.getPrecioEntradaEvento())
                .cuposTotalesEvento(evento.getCuposTotalesEvento())
                .cuposDisponibles(Math.max(0, evento.getCuposTotalesEvento() - ocupadas))
                .estadoPublicacionEvento(evento.getEstadoPublicacionEvento().name())
                .imagenUrl(evento.getImagenUrl())
                .categoriaId(evento.getCategoria() == null ? null : evento.getCategoria().getIdCategoria())
                .categoriaNombre(evento.getCategoria() == null ? null : evento.getCategoria().getNombre())
                .organizadorIdEvento(evento.getOrganizador() == null ? null : evento.getOrganizador().getIdUsuario())
                .version(ocupadas)
                .build();
    }
}
