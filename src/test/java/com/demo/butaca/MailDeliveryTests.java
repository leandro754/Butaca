package com.demo.butaca;

import com.demo.butaca.config.MailSettings;
import com.demo.butaca.model.*;
import com.demo.butaca.repository.*;
import com.demo.butaca.service.MailDeliveryService;
import com.demo.butaca.service.TicketImageService;
import com.demo.butaca.service.MailBrandAssets;
import com.demo.butaca.service.ReceiptPdfService;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MailDeliveryTests {
    JavaMailSender sender = mock(JavaMailSender.class);
    NotificacionRepository notifications = mock(NotificacionRepository.class);
    ComprobanteRepository receipts = mock(ComprobanteRepository.class);
    MailSettings settings = new MailSettings();
    MailBrandAssets brand = new MailBrandAssets();
    TicketImageService images = new TicketImageService(brand);
    ReceiptPdfService pdfs = new ReceiptPdfService(brand);
    MailDeliveryService delivery() { return new MailDeliveryService(sender, settings, notifications, receipts, images, brand, pdfs); }

    @Test void soloMarcaEnviadoTrasAceptarSmtpYRedirigeAlDestinatarioDePrueba() throws Exception {
        settings.setEnabled(true); settings.setFrom("remitente@gmail.com");
        Notificacion job = notification();
        when(notifications.findById(1L)).thenReturn(Optional.of(job));
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        doAnswer(invocation -> { assertEquals(EstadoEnvioNotificacion.PENDIENTE, job.getEstadoEnvio()); return null; }).when(sender).send(message);
        delivery().deliver(1L);
        assertEquals("contact@leandro754.moe", message.getAllRecipients()[0].toString());
        assertEquals("contact@leandro754.moe", message.getReplyTo()[0].toString());
        assertEquals(1, message.getAllRecipients().length);
        assertEquals(EstadoEnvioNotificacion.ENVIADA, job.getEstadoEnvio());
        assertNotNull(job.getFechaEnvio());
        assertEquals("true", message.getHeader("X-Butaca-Test", null));
    }

    @Test void errorSmtpNoDeshaceCompraYNoGuardaCredencialesEnElError() throws Exception {
        settings.setEnabled(true); settings.setFrom("remitente@gmail.com");
        Notificacion job = notification();
        Compra purchase = Compra.builder().id(3L).estadoCompra(EstadoCompra.CONFIRMADA).entradas(List.of()).build();
        job.setCompra(purchase);
        Comprobante receipt = Comprobante.builder().numeroComprobante("BUT-PRUEBA").estadoEnvioComprobante(EstadoEnvio.PENDIENTE).hashCruceSeguridad("a".repeat(64)).build();
        when(notifications.findById(1L)).thenReturn(Optional.of(job));
        when(receipts.findByPagoCompraId(3L)).thenReturn(Optional.of(receipt));
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("secret-password-value")).when(sender).send(any(MimeMessage.class));
        delivery().deliver(1L);
        assertEquals(EstadoEnvioNotificacion.FALLIDA, job.getEstadoEnvio());
        assertEquals(EstadoEnvio.ERROR_ENVIO, receipt.getEstadoEnvioComprobante());
        assertEquals(EstadoCompra.CONFIRMADA, purchase.getEstadoCompra());
        assertFalse(job.getErrorEnvio().contains("secret-password-value"));
    }

    @Test void sinConfiguracionNoInventaEnvioNiContactaSmtp() {
        Notificacion job = notification(); when(notifications.findById(1L)).thenReturn(Optional.of(job));
        delivery().deliver(1L);
        assertEquals(EstadoEnvioNotificacion.PENDIENTE, job.getEstadoEnvio()); verifyNoInteractions(sender);
    }

    @Test void adjuntoNominalContieneQrQueDecodificaElUuidCompleto() throws Exception {
        Evento event = Evento.builder().tituloEvento("Prueba de entradas").fechaEvento(LocalDate.now().plusDays(2)).horaEvento(LocalTime.of(20, 30))
                .ubicacion(Ubicacion.builder().nombreLugar("Teatro de prueba").build()).build();
        UUID code = UUID.randomUUID();
        Entrada ticket = Entrada.builder().idEntrada(1L).evento(event).codigoUnicoEntrada(code).nombreAsistente("Ana Prueba").dniAsistente("44120987").precioPagado(new BigDecimal("3000.00")).build();
        var image = ImageIO.read(new ByteArrayInputStream(images.render(ticket)));
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        Result result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels))));
        assertEquals(code.toString(), result.getText());
    }

    @Test void correoDeCompraAdjuntaEntradaNominalYReciboAntesDeMarcarEnviado() throws Exception {
        settings.setEnabled(true); settings.setFrom("remitente@gmail.com");
        Evento event = Evento.builder().tituloEvento("Función de prueba").fechaEvento(LocalDate.now().plusDays(2)).horaEvento(LocalTime.of(20, 30))
                .ubicacion(Ubicacion.builder().nombreLugar("Teatro de prueba").build()).build();
        Entrada ticket = Entrada.builder().idEntrada(8L).evento(event).codigoUnicoEntrada(UUID.randomUUID())
                .nombreAsistente("Ana Prueba").dniAsistente("44120987").precioPagado(new BigDecimal("3000.00")).build();
        Compra purchase = Compra.builder().id(3L).estadoCompra(EstadoCompra.CONFIRMADA).entradas(List.of(ticket)).build();
        Notificacion job = notification(); job.setCompra(purchase);
        Comprobante receipt = Comprobante.builder().numeroComprobante("BUT-PRUEBA").estadoEnvioComprobante(EstadoEnvio.PENDIENTE).hashCruceSeguridad("a".repeat(64)).build();
        when(notifications.findById(1L)).thenReturn(Optional.of(job));
        when(receipts.findByPagoCompraId(3L)).thenReturn(Optional.of(receipt));
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        doAnswer(invocation -> { assertEquals(EstadoEnvio.PENDIENTE, receipt.getEstadoEnvioComprobante()); return null; }).when(sender).send(message);
        delivery().deliver(1L);
        message.saveChanges();
        var multipart = (jakarta.mail.Multipart) message.getContent();
        var mixed = multipart;
        // MimeMessageHelper's mixed/related structure keeps attachments at the root.
        boolean png = false, pdf = false;
        for (int index = 0; index < mixed.getCount(); index++) {
            String filename = mixed.getBodyPart(index).getFileName();
            png |= "Butaca-entrada-8.png".equals(filename);
            pdf |= "BUT-PRUEBA.pdf".equals(filename);
        }
        assertTrue(png); assertTrue(pdf);
        assertEquals(EstadoEnvio.ENVIADO, receipt.getEstadoEnvioComprobante());
        assertEquals(EstadoEnvioNotificacion.ENVIADA, job.getEstadoEnvio());
    }

    Notificacion notification() {
        return Notificacion.builder().idNotificacion(1L).usuario(Usuario.builder().emailUsuario("cliente@ejemplo.test").build())
                .mensaje("Una novedad de prueba").tipo("INFO").estadoEnvio(EstadoEnvioNotificacion.PENDIENTE).build();
    }
}
