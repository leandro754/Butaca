package com.demo.butaca;

import com.demo.butaca.dto.*;
import com.demo.butaca.repository.*;
import com.demo.butaca.service.*;
import com.demo.butaca.model.EstadoEnvioNotificacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "butaca.mail.enabled=false")
@ActiveProfiles("demo")
class EventConfigurationTests {
    @Autowired EventoService events;
    @Autowired CompraService purchases;
    @Autowired EntradaService tickets;
    @Autowired UsuarioRepository users;
    @Autowired CategoriaRepository categories;
    @Autowired UbicacionRepository locations;
    @Autowired ReembolsoRepository refunds;
    @Autowired NotificacionRepository notifications;

    EventoRequestDTO newEvent() {
        EventoRequestDTO request = new EventoRequestDTO();
        request.setTituloEvento("Configuración " + UUID.randomUUID());
        request.setDescripcionEvento("Prueba de configuración de evento");
        request.setFechaEvento(LocalDate.now().plusDays(20)); request.setHoraEvento(LocalTime.of(20, 0));
        request.setOrganizadorIdEvento(users.findByEmailUsuarioIgnoreCase("org@butaca.com").orElseThrow().getIdUsuario());
        request.setCategoriaId(categories.findAll().get(0).getIdCategoria());
        request.setUbicacionId(locations.findAll().get(0).getIdUbicacion());
        request.setCuposTotalesEvento(100); request.setPrecioEntradaEvento(new BigDecimal("2000.00"));
        request.setEstadoPublicacionEvento("PUBLICADO");
        request.setTiposEntrada(List.of(TipoEntradaDTO.builder().nombre("General").precio(new BigDecimal("2000.00")).activo(true).build(),
                TipoEntradaDTO.builder().nombre("VIP").precio(new BigDecimal("5000.00")).activo(true).build()));
        return request;
    }

    CompraResponseDTO buy(EventoResponseDTO event, Long type, int quantity) {
        CompraRequestDTO request = new CompraRequestDTO(); request.setEventoId(event.getIdEvento()); request.setTipoEntradaId(type);
        request.setCantidad(quantity); request.setUsuarioId(users.findByEmailUsuarioIgnoreCase("cliente@butaca.com").orElseThrow().getIdUsuario());
        return purchases.comprarEntrada(request);
    }

    @Test void tarifasSeleccionablesConservanPrecioHistoricoYNotificanReprogramacion() {
        EventoRequestDTO request = newEvent(); EventoResponseDTO created = events.crearEvento(request);
        Long vipId = created.getTiposEntrada().stream().filter(type -> type.getNombre().equals("VIP")).findFirst().orElseThrow().getIdTipoEntrada();
        CompraResponseDTO purchase = buy(created, vipId, 2);
        assertEquals(0, new BigDecimal("10000").compareTo(purchase.getTotal()));
        EventoResponseDTO current = events.obtenerEvento(created.getIdEvento());
        request.setVersion(current.getVersion()); request.setTiposEntrada(current.getTiposEntrada());
        request.getTiposEntrada().forEach(type -> type.setPrecio(new BigDecimal("7000.00")));
        request.setFechaEvento(request.getFechaEvento().plusDays(3));
        events.editarEvento(created.getIdEvento(), request);
        assertEquals(0, new BigDecimal("10000").compareTo(purchases.obtenerCompra(purchase.getId()).getTotal()));
        EventoResponseDTO edited = events.obtenerEvento(created.getIdEvento());
        assertEquals(2, edited.getEntradasVendidas());
        assertEquals(0, new BigDecimal("10000").compareTo(edited.getTotalVendido()));
        assertEquals(2, edited.getPorcentajeOcupacion());
        assertTrue(notifications.findByEventoId(created.getIdEvento()).stream().anyMatch(notification -> notification.getTipo().equals("REPROGRAMACION")
                && notification.getEstadoEnvio() == EstadoEnvioNotificacion.PENDIENTE));
        assertThrows(IllegalStateException.class, () -> events.editarEvento(created.getIdEvento(), request));
    }

    @Test void cancelarExigeAmbasConfirmacionesReembolsaTambienEntradasUsadasYNoDuplicaDevoluciones() {
        EventoResponseDTO event = events.crearEvento(newEvent());
        CompraResponseDTO purchase = buy(event, event.getTiposEntrada().get(0).getIdTipoEntrada(), 2);
        tickets.usarEntrada(purchase.getEntradas().get(0).getIdEntrada());
        CancelarEventoRequest confirmation = new CancelarEventoRequest(); confirmation.setConfirmarReembolsos(false);
        confirmation.setTituloConfirmacion(event.getTituloEvento()); confirmation.setVersion(events.obtenerEvento(event.getIdEvento()).getVersion());
        assertThrows(IllegalArgumentException.class, () -> events.cancelarEvento(event.getIdEvento(), confirmation));
        confirmation.setConfirmarReembolsos(true); confirmation.setTituloConfirmacion("otro nombre");
        assertThrows(IllegalArgumentException.class, () -> events.cancelarEvento(event.getIdEvento(), confirmation));
        confirmation.setTituloConfirmacion(event.getTituloEvento());
        events.cancelarEvento(event.getIdEvento(), confirmation);
        assertEquals(0, events.obtenerEvento(event.getIdEvento()).getEntradasVendidas());
        assertEquals(0, events.obtenerEvento(event.getIdEvento()).getTotalVendido().signum());
        CompraResponseDTO cancelled = purchases.obtenerCompra(purchase.getId());
        assertEquals("REEMBOLSADO", cancelled.getPago().getEstadoPago());
        assertTrue(cancelled.getEntradas().stream().allMatch(ticket -> ticket.getEstadoEntrada().equals("ANULADA")));
        long count = refunds.count(); events.cancelarEvento(event.getIdEvento(), confirmation); assertEquals(count, refunds.count());
    }

    @Test void rechazaTarifaAjenaYNoPermiteDesactivarTodosLosTipos() {
        EventoRequestDTO request = newEvent(); EventoResponseDTO first = events.crearEvento(request);
        EventoResponseDTO second = events.crearEvento(newEvent());
        assertThrows(IllegalArgumentException.class, () -> buy(first, second.getTiposEntrada().get(0).getIdTipoEntrada(), 1));
        request.setVersion(events.obtenerEvento(first.getIdEvento()).getVersion()); request.setTiposEntrada(first.getTiposEntrada());
        request.getTiposEntrada().forEach(type -> type.setActivo(false));
        assertThrows(IllegalArgumentException.class, () -> events.editarEvento(first.getIdEvento(), request));
    }
}
