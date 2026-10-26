package com.demo.butaca.service;

import com.demo.butaca.config.MailSettings;
import com.demo.butaca.model.*;
import com.demo.butaca.repository.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class MailDeliveryService {
    private final JavaMailSender sender;
    private final MailSettings settings;
    private final NotificacionRepository notifications;
    private final ComprobanteRepository receipts;
    private final TicketImageService ticketImages;
    private final MailBrandAssets brand;
    private final ReceiptPdfService receiptPdfs;

    public MailDeliveryService(JavaMailSender sender, MailSettings settings, NotificacionRepository notifications,
                               ComprobanteRepository receipts, TicketImageService ticketImages, MailBrandAssets brand, ReceiptPdfService receiptPdfs) {
        this.sender = sender; this.settings = settings; this.notifications = notifications;
        this.receipts = receipts; this.ticketImages = ticketImages; this.brand = brand; this.receiptPdfs = receiptPdfs;
    }

    // Separate transaction from the purchase: only committed jobs are visible to the worker.
    @Transactional
    public void deliver(Long id) {
        Notificacion notification = notifications.findById(id).orElseThrow();
        if (!settings.isEnabled() || notification.getEstadoEnvio() != EstadoEnvioNotificacion.PENDIENTE) return;
        Comprobante receipt = notification.getCompra() == null ? null
                : receipts.findByPagoCompraId(notification.getCompra().getId()).orElse(null);
        try {
            if (settings.getFrom().isBlank()) throw new IllegalStateException("Falta configurar el remitente SMTP");
            if (settings.getTestRecipient().isBlank()) throw new IllegalStateException("Falta configurar el destinatario de prueba SMTP");
            String recipient = settings.getTestRecipient();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper mail = new MimeMessageHelper(message, true, "UTF-8");
            mail.setFrom(settings.getFrom(), "Butaca");
            if (!settings.getReplyTo().isBlank()) mail.setReplyTo(settings.getReplyTo());
            mail.setTo(recipient);
            mail.setSubject("Butaca · " + (receipt == null ? MailContent.subject(notification) : "Tus entradas y comprobante " + receipt.getNumeroComprobante()));
            if (receipt != null && notification.getCompra().getEstadoCompra() != EstadoCompra.CONFIRMADA) {
                throw new IllegalStateException("La compra fue cancelada; no se enviarán entradas anuladas");
            }
            MailContent.Body content = MailContent.compose(notification, receipt, settings.getPublicUrl());
            mail.setText(content.text(), content.html());
            mail.addInline("butaca-logo", new ByteArrayResource(brand.logo(true)), "image/png");
            if (receipt != null) {
                for (Entrada ticket : notification.getCompra().getEntradas()) {
                    mail.addAttachment("Butaca-entrada-" + ticket.getIdEntrada() + ".png", new ByteArrayResource(ticketImages.render(ticket)), "image/png");
                }
                mail.addAttachment(receipt.getNumeroComprobante() + ".pdf", new ByteArrayResource(receiptPdfs.render(notification.getCompra(), receipt)), "application/pdf");
            }
            if (!settings.getTestRecipient().isBlank()) message.setHeader("X-Butaca-Test", "true");
            message.setHeader("X-Butaca-Notification", id.toString());
            sender.send(message);
            notification.setEstadoEnvio(EstadoEnvioNotificacion.ENVIADA);
            notification.setFechaEnvio(LocalDateTime.now());
            notification.setErrorEnvio(null);
            if (receipt != null) receipt.setEstadoEnvioComprobante(EstadoEnvio.ENVIADO);
        } catch (Exception exception) {
            notification.setEstadoEnvio(EstadoEnvioNotificacion.FALLIDA);
            // Keep credentials and raw SMTP responses out of the database/API.
            notification.setErrorEnvio("No se pudo enviar por SMTP (" + exception.getClass().getSimpleName() + "). Revisá configuración y reintentá.");
            if (receipt != null) receipt.setEstadoEnvioComprobante(EstadoEnvio.ERROR_ENVIO);
        }
    }

}
