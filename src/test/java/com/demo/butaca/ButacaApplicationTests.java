package com.demo.butaca;

import com.demo.butaca.dto.AsistenteNominalDTO;
import com.demo.butaca.dto.CompraRequestDTO;
import com.demo.butaca.dto.CompraResponseDTO;
import com.demo.butaca.dto.EventoResponseDTO;
import com.demo.butaca.dto.LoginRequest;
import com.demo.butaca.dto.RegistroUsuarioRequest;
import com.demo.butaca.dto.VerificacionCruceDTO;
import com.demo.butaca.dto.VerificacionIdentidadDTO;
import com.demo.butaca.integration.pago.PagoRechazadoException;
import com.demo.butaca.model.AuditoriaSeguridad;
import com.demo.butaca.model.Usuario;
import com.demo.butaca.repository.AuditoriaSeguridadRepository;
import com.demo.butaca.repository.EntradaRepository;
import com.demo.butaca.repository.HistorialEstadoEntradaRepository;
import com.demo.butaca.repository.UsuarioRepository;
import com.demo.butaca.service.AuthService;
import com.demo.butaca.service.CompraService;
import com.demo.butaca.service.EventoService;
import com.demo.butaca.service.VerificacionIdentidadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("demo")
class ButacaApplicationTests {

    private static final byte[] FOTO_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Zl1sAAAAASUVORK5CYII="
    );

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
    private EntradaRepository entradaRepository;

    @Autowired
    private HistorialEstadoEntradaRepository historialRepository;

    @Autowired
    private AuditoriaSeguridadRepository auditoriaRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void catalogoPublicoSoloMuestraEventosPublicadosConCupo() {
        List<EventoResponseDTO> eventos = eventoService.listarEventosPublicados();

        assertFalse(eventos.isEmpty());
        assertTrue(eventos.stream().allMatch(evento -> "PUBLICADO".equals(evento.getEstadoPublicacionEvento())));
        assertTrue(eventos.stream().allMatch(evento -> evento.getCuposDisponibles() > 0));
    }

    @Test
    void comprobantesDemoTienenCruceValido() {
        CompraResponseDTO demo = compraService.listarTodas().stream()
                .filter(compra -> compra.getPago() != null
                        && "PAY-MP-DEMO0001".equals(compra.getPago().getReferenciaExternaPago()))
                .findFirst()
                .orElseThrow();

        VerificacionCruceDTO cruce = compraService.verificarCruce(demo.getId());
        assertTrue(cruce.isValida(), () -> "Checks: " + cruce.getChecks());
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
        request.setEmailUsuario(cliente.getEmailUsuario());
        request.setDniUsuario(cliente.getDniUsuario());
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
        assertEquals(compra.getTotal(), compra.getPago().getMonto());
        assertNotNull(compra.getPago().getCodigoAutorizacion());
        assertNotNull(compra.getComprobante());
        assertEquals("ENVIADO", compra.getComprobante().getEstadoEnvioComprobante());
        assertEquals(64, compra.getComprobante().getHashCruceSeguridad().length());
        assertTrue(compra.getEntradas().stream().allMatch(entrada -> entrada.getCodigoUnicoEntrada() != null));
        assertTrue(compra.getEntradas().stream()
                .allMatch(entrada -> precioAlComprar.compareTo(entrada.getPrecioPagado()) == 0));
        assertTrue(compra.getEntradas().stream().allMatch(entrada ->
                historialRepository.findByEntradaIdEntradaOrderByFechaCambioEntradaAsc(entrada.getIdEntrada()).size() == 1));
        assertNotNull(compra.getId());

        VerificacionCruceDTO cruce = compraService.verificarCruce(compra.getId());
        assertTrue(cruce.isValida());
        assertTrue(cruce.getChecks().values().stream().allMatch(Boolean::booleanValue));
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
        Usuario martina = usuarioRepository.findByEmailUsuarioIgnoreCase("martina@butaca.com").orElseThrow();

        CompraRequestDTO intentoSinKyc = new CompraRequestDTO();
        intentoSinKyc.setEventoId(publicado.getIdEvento());
        intentoSinKyc.setUsuarioId(registrado.getIdUsuario());
        intentoSinKyc.setCantidad(1);
        intentoSinKyc.setEmailUsuario(martina.getEmailUsuario());
        intentoSinKyc.setDniUsuario(martina.getDniUsuario());

        assertThrows(IllegalStateException.class, () -> compraService.comprarEntrada(intentoSinKyc));

        List<AuditoriaSeguridad> auditoriaBloqueo =
                auditoriaRepository.findByUsuarioIdUsuarioOrderByFechaAuditoriaDesc(martina.getIdUsuario());
        assertTrue(auditoriaBloqueo.stream()
                .anyMatch(evento -> "CRUCE_COMPRA_BLOQUEADO".equals(evento.getAccionAuditoria())
                        && "BLOQUEADO".equals(evento.getResultadoAuditoria())));

        VerificacionIdentidadDTO verificacion = verificarIdentidad(martina);
        assertEquals("VERIFICADA", verificacion.getEstadoVerificacion());
        assertNotNull(verificacion.getHashEvidenciaDocumental());
        assertNotNull(verificacion.getHashSelfie());
        assertEquals("SIMULADO_RENAPER_DEMO", verificacion.getProveedorIdentidad());

        intentoSinKyc.setAsistentes(List.of(new AsistenteNominalDTO("45111222", "Martina Sosa")));
        CompraResponseDTO compraExitosa = compraService.comprarEntrada(intentoSinKyc);
        assertNotNull(compraExitosa.getId());
        assertNotNull(compraExitosa.getComprobante());
        assertEquals("45111222", compraExitosa.getClienteDni());
        assertTrue(compraService.verificarCruce(compraExitosa.getId()).isValida());
        assertFalse(auditoriaRepository.findByUsuarioIdUsuarioOrderByFechaAuditoriaDesc(registrado.getIdUsuario()).isEmpty());
    }

    @Test
    void kycNoCreaUsuariosNiAceptaUsuarioInexistente() {
        long usuariosAntes = usuarioRepository.count();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                verificacionService.verificarIdentidad(VerificacionIdentidadDTO.builder()
                                .usuarioId(999999L)
                                .dniUsuario("40111222")
                                .metodoVerificacion("BIOMETRIA_RENAPER")
                                .build(),
                        FOTO_PNG, "image/png", FOTO_PNG, "image/png"));

        assertTrue(error.getMessage().contains("Usuario no encontrado"));
        assertEquals(usuariosAntes, usuarioRepository.count());
    }

    @Test
    void kycRechazaFotoInvalidaYDejaAuditoriaPersistente() {
        RegistroUsuarioRequest registro = new RegistroUsuarioRequest();
        registro.setNombreUsuario("Tomas");
        registro.setApellidoUsuario("Lopez");
        registro.setDniUsuario("46777888");
        registro.setEmailUsuario("tomas@butaca.com");
        registro.setPasswordUsuario("tomas123");
        registro.setRolUsuario("CLIENTE");
        authService.registrar(registro);
        Usuario tomas = usuarioRepository.findByEmailUsuarioIgnoreCase("tomas@butaca.com").orElseThrow();

        VerificacionIdentidadDTO resultado = verificacionService.verificarIdentidad(
                VerificacionIdentidadDTO.builder()
                        .usuarioId(tomas.getIdUsuario())
                        .dniUsuario("46777888")
                        .metodoVerificacion("BIOMETRIA_RENAPER")
                        .build(),
                new byte[]{1, 2, 3},
                "text/plain",
                new byte[]{1, 2, 3},
                "text/plain"
        );

        assertEquals("RECHAZADA", resultado.getEstadoVerificacion());
        assertTrue(resultado.getObservacionVerificacion().toLowerCase().contains("imagen"));
        assertTrue(auditoriaRepository.findByUsuarioIdUsuarioOrderByFechaAuditoriaDesc(tomas.getIdUsuario()).stream()
                .anyMatch(evento -> "VERIFICACION_KYC".equals(evento.getAccionAuditoria())
                        && "RECHAZADO".equals(evento.getResultadoAuditoria())));
    }

    @Test
    void kycRechazaDniDistintoAlRegistradoYAudita() {
        RegistroUsuarioRequest registro = new RegistroUsuarioRequest();
        registro.setNombreUsuario("Lucia");
        registro.setApellidoUsuario("Ferrer");
        registro.setDniUsuario("47000111");
        registro.setEmailUsuario("lucia.kyc@butaca.com");
        registro.setPasswordUsuario("lucia123");
        registro.setRolUsuario("CLIENTE");
        authService.registrar(registro);
        Usuario lucia = usuarioRepository.findByEmailUsuarioIgnoreCase("lucia.kyc@butaca.com").orElseThrow();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                verificacionService.verificarIdentidad(
                        VerificacionIdentidadDTO.builder()
                                .usuarioId(lucia.getIdUsuario())
                                .dniUsuario("47000222")
                                .metodoVerificacion("BIOMETRIA_RENAPER")
                                .build(),
                        FOTO_PNG, "image/png", FOTO_PNG, "image/png"));

        assertTrue(error.getMessage().contains("no coincide"));
        assertFalse(verificacionService.estaVerificado(lucia.getIdUsuario()));
        assertTrue(auditoriaRepository.findByUsuarioIdUsuarioOrderByFechaAuditoriaDesc(lucia.getIdUsuario()).stream()
                .anyMatch(evento -> "VERIFICACION_KYC".equals(evento.getAccionAuditoria())
                        && "RECHAZADO".equals(evento.getResultadoAuditoria())
                        && evento.getDetalleTracking().contains("DNI informado no coincide")));
    }

    @Test
    void loginFallidoDejaAuditoriaPersistente() {
        Usuario cliente = usuarioRepository.findByEmailUsuarioIgnoreCase("cliente@butaca.com").orElseThrow();
        LoginRequest login = new LoginRequest();
        login.setEmailUsuario(cliente.getEmailUsuario());
        login.setPasswordUsuario("clave-incorrecta");

        assertThrows(IllegalArgumentException.class, () -> authService.login(login));

        assertTrue(auditoriaRepository.findByUsuarioIdUsuarioOrderByFechaAuditoriaDesc(cliente.getIdUsuario()).stream()
                .anyMatch(evento -> "LOGIN".equals(evento.getAccionAuditoria())
                        && "RECHAZADO".equals(evento.getResultadoAuditoria())));
    }

    @Test
    void pagoRechazadoNoEmiteEntradasYDejaAuditoriaIndependiente() {
        EventoResponseDTO publicado = eventoService.listarEventosPublicados().get(0);
        Usuario cliente = usuarioRepository.findByEmailUsuarioIgnoreCase("cliente@butaca.com").orElseThrow();
        long entradasAntes = entradaRepository.count();

        CompraRequestDTO request = new CompraRequestDTO();
        request.setEventoId(publicado.getIdEvento());
        request.setUsuarioId(cliente.getIdUsuario());
        request.setCantidad(1);
        request.setPasarelaPago("MERCADOPAGO");
        request.setMedioPago("TARJETA_CREDITO");
        request.setUltimosCuatroDigitos("0004");
        request.setTokenPago("RECHAZAR-DEMO");
        request.setEmailUsuario(cliente.getEmailUsuario());
        request.setDniUsuario(cliente.getDniUsuario());
        request.setAsistentes(List.of(new AsistenteNominalDTO("42850123", "Juan Perez")));

        assertThrows(PagoRechazadoException.class, () -> compraService.comprarEntrada(request));
        assertEquals(entradasAntes, entradaRepository.count());
        assertTrue(auditoriaRepository.findByUsuarioIdUsuarioOrderByFechaAuditoriaDesc(cliente.getIdUsuario()).stream()
                .anyMatch(evento -> "CRUCE_COMPRA_BLOQUEADO".equals(evento.getAccionAuditoria())
                        && evento.getDetalleTracking().contains("Pago rechazado")));
    }

    private VerificacionIdentidadDTO verificarIdentidad(Usuario usuario) {
        return verificacionService.verificarIdentidad(
                VerificacionIdentidadDTO.builder()
                        .usuarioId(usuario.getIdUsuario())
                        .emailUsuario(usuario.getEmailUsuario())
                        .nombreUsuario(usuario.getNombreUsuario())
                        .apellidoUsuario(usuario.getApellidoUsuario())
                        .dniUsuario(usuario.getDniUsuario())
                        .numeroTramite("12345678")
                        .metodoVerificacion("BIOMETRIA_RENAPER")
                        .build(),
                FOTO_PNG,
                "image/png",
                FOTO_PNG,
                "image/png"
        );
    }
}
