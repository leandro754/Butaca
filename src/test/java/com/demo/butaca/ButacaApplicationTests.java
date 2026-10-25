package com.demo.butaca;

import com.demo.butaca.dto.AsistenteNominalDTO;
import com.demo.butaca.dto.CompraRequestDTO;
import com.demo.butaca.dto.CompraResponseDTO;
import com.demo.butaca.dto.EventoResponseDTO;
import com.demo.butaca.dto.CancelarEventoRequest;
import com.demo.butaca.dto.RegistroUsuarioRequest;
import com.demo.butaca.dto.VerificacionIdentidadDTO;
import com.demo.butaca.model.Usuario;
import com.demo.butaca.repository.AuditoriaSeguridadRepository;
import com.demo.butaca.repository.ComprobanteRepository;
import com.demo.butaca.repository.HistorialEstadoEntradaRepository;
import com.demo.butaca.repository.PersonaRepository;
import com.demo.butaca.repository.ReembolsoRepository;
import com.demo.butaca.repository.TipoEntradaRepository;
import com.demo.butaca.repository.UsuarioRepository;
import com.demo.butaca.service.AuthService;
import com.demo.butaca.service.CompraService;
import com.demo.butaca.service.EventoService;
import com.demo.butaca.service.VerificacionIdentidadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "butaca.mail.enabled=false")
@ActiveProfiles("demo")
class ButacaApplicationTests {

    @Autowired
    @Qualifier("initDatabase")
    private CommandLineRunner demoLoader;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private CompraService compraService;

    @Autowired
    private AuthService authService;

    @Autowired
    private VerificacionIdentidadService verificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HistorialEstadoEntradaRepository historialRepository;

    @Autowired
    private AuditoriaSeguridadRepository auditoriaRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private TipoEntradaRepository tipoEntradaRepository;

    @Autowired
    private ReembolsoRepository reembolsoRepository;

    @Autowired
    private ComprobanteRepository comprobanteRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void carteleraDemoVariadaNoDuplicaEventosAlRepetirSemilla() throws Exception {
        List<EventoResponseDTO> antes = eventoService.listarEventosPublicados();
        assertTrue(antes.size() >= 18);
        assertTrue(antes.stream().map(EventoResponseDTO::getCategoriaNombre).distinct().count() >= 6);
        demoLoader.run();
        List<EventoResponseDTO> despues = eventoService.listarEventosPublicados();
        assertEquals(antes.size(), despues.size());
        assertEquals(despues.size(), despues.stream().map(EventoResponseDTO::getTituloEvento).distinct().count());
    }

    @Test
    void catalogoPublicoSoloMuestraEventosPublicadosConCupo() {
        List<EventoResponseDTO> eventos = eventoService.listarEventosPublicados();

        assertFalse(eventos.isEmpty());
        assertTrue(eventos.stream().allMatch(evento -> "PUBLICADO".equals(evento.getEstadoPublicacionEvento())));
        assertTrue(eventos.stream().allMatch(evento -> evento.getCuposDisponibles() > 0));
    }

    @Test
    void comprarCalculaStockYEmiteTicketsNominalesConPagoYComprobanteCruzado() {
        EventoResponseDTO publicado = eventoService.listarEventosPublicados().get(0);
        Usuario cliente = usuarioRepository.findByEmailUsuarioIgnoreCase("cliente@butaca.com").orElseThrow();
        int stockAntes = publicado.getCuposDisponibles();
        BigDecimal precioAlComprar = publicado.getPrecioEntradaEvento();

        CompraRequestDTO request = new CompraRequestDTO();
        request.setEventoId(publicado.getIdEvento());
        request.setUsuarioId(cliente.getIdUsuario());
        request.setCantidad(2);
        request.setPasarelaPago("MERCADOPAGO");
        request.setMedioPago("TARJETA_CREDITO");
        request.setUltimosCuatroDigitos("5432");
        request.setTipoComprobante("RECIBO_DIGITAL");
        request.setAsistentes(List.of(
                new AsistenteNominalDTO("42850123", "Juan Perez"),
                new AsistenteNominalDTO("43999888", "Lucia Fernandez")
        ));

        CompraResponseDTO compra = compraService.comprarEntrada(request);
        EventoResponseDTO eventoDespues = eventoService.obtenerEvento(publicado.getIdEvento());

        assertEquals(stockAntes - 2, eventoDespues.getCuposDisponibles());
        assertTrue(eventoDespues.getVersion() > publicado.getVersion());
        assertEquals(2, compra.getCantidad());
        assertEquals(precioAlComprar.multiply(BigDecimal.valueOf(2)), compra.getTotal());
        assertEquals(2, compra.getEntradas().size());
        assertEquals("42850123", compra.getEntradas().get(0).getDniAsistente());
        assertEquals("43999888", compra.getEntradas().get(1).getDniAsistente());
        assertNotNull(compra.getPago());
        assertEquals("APROBADO", compra.getPago().getEstadoPago());
        assertEquals("5432", compra.getPago().getUltimosCuatroDigitos());
        assertNotNull(compra.getComprobante());
        assertEquals("PENDIENTE", compra.getComprobante().getEstadoEnvioComprobante());
        assertEquals(64, compra.getComprobante().getHashCruceSeguridad().length());
        assertTrue(compra.getEntradas().stream().allMatch(entrada -> entrada.getCodigoUnicoEntrada() != null));
        assertTrue(compra.getEntradas().stream()
                .allMatch(entrada -> precioAlComprar.compareTo(entrada.getPrecioPagado()) == 0));
        assertTrue(compra.getEntradas().stream().allMatch(entrada ->
                historialRepository.findByEntradaIdEntradaOrderByFechaCambioEntradaAsc(entrada.getIdEntrada()).size() == 1));
        assertNotNull(compra.getId());
    }

    @Test
    void bloqueaCompraSiUsuarioNoTieneIdentidadVerificadaYLuegoHabilitaTrasKyc() {
        EventoResponseDTO publicado = eventoService.listarEventosPublicados().get(0);

        RegistroUsuarioRequest registro = new RegistroUsuarioRequest();
        registro.setNombreUsuario("Martina");
        registro.setApellidoUsuario("Sosa");
        registro.setDniUsuario("45111222");
        registro.setEmailUsuario("martina@butaca.com");
        registro.setPasswordUsuario("martina123");
        registro.setRolUsuario("CLIENTE");
        var registrado = authService.registrar(registro);

        CompraRequestDTO intentoSinKyc = new CompraRequestDTO();
        intentoSinKyc.setEventoId(publicado.getIdEvento());
        intentoSinKyc.setUsuarioId(registrado.getIdUsuario());
        intentoSinKyc.setCantidad(1);

        assertThrows(IllegalStateException.class, () -> compraService.comprarEntrada(intentoSinKyc));

        verificacionService.verificarIdentidad(VerificacionIdentidadDTO.builder()
                .usuarioId(registrado.getIdUsuario())
                .dniUsuario("45111222")
                .metodoVerificacion("BIOMETRIA_RENAPER")
                .build());

        CompraResponseDTO compraExitosa = compraService.comprarEntrada(intentoSinKyc);
        assertNotNull(compraExitosa.getId());
        assertNotNull(compraExitosa.getComprobante());
        assertFalse(auditoriaRepository.findByUsuarioIdUsuarioOrderByFechaAuditoriaDesc(registrado.getIdUsuario()).isEmpty());
    }

    @Test
    void modeloCompleto16TablasSincronizadoConDrawDbIncluyendoPersonaTipoEntradaYReembolso() {
        assertFalse(personaRepository.findAll().isEmpty());
        assertFalse(tipoEntradaRepository.findAll().isEmpty());
        assertFalse(comprobanteRepository.findAll().isEmpty());

        List<EventoResponseDTO> publicados = eventoService.listarEventosPublicados();
        EventoResponseDTO aCancelar = publicados.get(publicados.size() - 1);
        Usuario cliente = usuarioRepository.findByEmailUsuarioIgnoreCase("cliente@butaca.com").orElseThrow();

        CompraRequestDTO compraReq = new CompraRequestDTO();
        compraReq.setEventoId(aCancelar.getIdEvento());
        compraReq.setUsuarioId(cliente.getIdUsuario());
        compraReq.setCantidad(1);
        compraService.comprarEntrada(compraReq);

        EventoResponseDTO actual = eventoService.obtenerEvento(aCancelar.getIdEvento());
        CancelarEventoRequest cancelacion = new CancelarEventoRequest();
        cancelacion.setConfirmarReembolsos(true);
        cancelacion.setTituloConfirmacion(actual.getTituloEvento());
        cancelacion.setVersion(actual.getVersion());
        eventoService.cancelarEvento(aCancelar.getIdEvento(), cancelacion);
        assertFalse(reembolsoRepository.findAll().isEmpty());
    }
}
