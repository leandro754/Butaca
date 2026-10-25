package com.demo.butaca.service;

import com.demo.butaca.model.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

final class MailContent {
    record Body(String text, String html) {}
    private MailContent() {}

    static Body compose(Notificacion notification, Comprobante receipt, String publicUrl) {
        String title = receipt == null ? subject(notification) : "Tus entradas están listas";
        StringBuilder text = new StringBuilder(title + "\n\n");
        StringBuilder body = new StringBuilder("<h1 style='font-size:28px;line-height:1.2;margin:0 0 14px;color:#17191b'>" + esc(title) + "</h1>");
        if (receipt == null) {
            text.append(notification.getMensaje());
            body.append("<p style='line-height:1.7;margin:0 0 24px'>").append(esc(notification.getMensaje())).append("</p>");
        } else {
            Compra purchase = notification.getCompra();
            BigDecimal total = purchase.getEntradas().stream().map(Entrada::getPrecioPagado).reduce(BigDecimal.ZERO, BigDecimal::add);
            String number = receipt.getNumeroComprobante();
            text.append("Comprobante: ").append(number).append("\nCompra #").append(purchase.getId()).append("\n\n");
            body.append("<p style='color:#62666a;font-size:14px;margin:0 0 26px'>Compra #").append(purchase.getId()).append(" &nbsp; / &nbsp; ").append(esc(number)).append("</p>");
            if (!purchase.getEntradas().isEmpty()) {
                Evento event = purchase.getEntradas().get(0).getEvento();
                String date = event.getFechaEvento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - " + event.getHoraEvento() + " h";
                text.append(event.getTituloEvento()).append("\n").append(date).append("\n").append(event.getUbicacion().getNombreLugar()).append("\n\n");
                body.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background:#f7ebdd;border-left:4px solid #b80b32;margin-bottom:26px'><tr><td style='padding:22px'>")
                        .append("<p style='font-size:11px;letter-spacing:2px;color:#a40b2c;margin:0 0 10px'>TU PRÓXIMA SALIDA</p>")
                        .append("<h2 style='font-size:21px;line-height:1.3;margin:0 0 14px'>").append(esc(event.getTituloEvento())).append("</h2>")
                        .append("<p style='font-size:14px;line-height:1.7;margin:0'>").append(esc(date)).append("<br>")
                        .append(esc(event.getUbicacion().getNombreLugar())).append("<br><span style='color:#62666a'>")
                        .append(esc(event.getUbicacion().getDireccion())).append("</span></p></td></tr></table>");
            }
            body.append("<h2 style='font-size:15px;margin:0 0 14px'>Resumen de la compra</h2><table width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;font-size:14px'><thead><tr><th scope='col' align='left' style='padding:12px 0;border-bottom:1px solid #ddd6cf'>Entrada</th><th scope='col' align='center' style='padding:12px 8px;border-bottom:1px solid #ddd6cf'>Cant.</th><th scope='col' align='right' style='padding:12px 0;border-bottom:1px solid #ddd6cf'>Importe</th></tr></thead><tbody>");
            record Tariff(String name, BigDecimal price) {}
            Map<Tariff, Integer> items = new LinkedHashMap<>();
            for (Entrada ticket : purchase.getEntradas()) {
                items.merge(new Tariff(ticket.getTipoEntrada() == null ? "General" : ticket.getTipoEntrada().getNombreTipoEntrada(), ticket.getPrecioPagado().stripTrailingZeros()), 1, Integer::sum);
                text.append(ticket.getNombreAsistente()).append(" - Código: ").append(ticket.getCodigoUnicoEntrada()).append("\n");
            }
            for (var item : items.entrySet()) {
                body.append("<tr><td style='padding:14px 0;border-bottom:1px solid #eee8e2'>").append(esc(item.getKey().name())).append("<br><span style='font-size:12px;color:#62666a'>Unitario ")
                        .append(esc(ReceiptPdfService.money(item.getKey().price()))).append("</span></td><td align='center' style='border-bottom:1px solid #eee8e2'>").append(item.getValue())
                        .append("</td><td align='right' style='border-bottom:1px solid #eee8e2;white-space:nowrap'>").append(esc(ReceiptPdfService.money(item.getKey().price().multiply(BigDecimal.valueOf(item.getValue()))))).append("</td></tr>");
            }
            body.append("<tr><td colspan='2' style='padding:22px 0;font-weight:bold'>Total de la compra <span style='font-size:11px;color:#62666a'>(ARS)</span></td><td align='right' style='padding:22px 0;font-size:23px;font-weight:bold;white-space:nowrap'>")
                    .append(esc(ReceiptPdfService.money(total))).append("</td></tr></tbody></table>")
                    .append("<p style='font-size:14px;line-height:1.7;margin:8px 0 24px'>Adjuntamos tus entradas nominales en PNG y el comprobante de compra en PDF. Guardá las entradas en tu teléfono y presentalas junto con el DNI de cada titular.</p>");
            text.append("\nTotal ARS: ").append(ReceiptPdfService.money(total)).append("\nEntradas nominales PNG y comprobante PDF adjuntos. Presentá tu QR y DNI en puerta.\n");
        }
        String url = publicUrl.replaceAll("/+$", "") + "/dashboard";
        body.append("<table role='presentation' cellpadding='0' cellspacing='0' style='margin:4px 0 26px'><tr><td bgcolor='#b80b32' style='border-radius:5px'><a href='").append(esc(url))
                .append("' style='display:inline-block;padding:15px 24px;color:#fff;text-decoration:none;font-size:14px;font-weight:bold'>Ver mis entradas</a></td></tr></table>")
                .append("<p style='font-size:12px;line-height:1.6;color:#62666a;margin:0'>¿Necesitás ayuda? Respondé este correo o escribí a <a href='mailto:contact@leandro754.moe' style='color:#a40b2c'>contact@leandro754.moe</a>.</p>");
        String html = "<!doctype html><html lang='es'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><meta name='color-scheme' content='light'></head>"
                + "<body style='margin:0;padding:24px 10px;background:#f3eee8;color:#17191b;font-family:Arial,Helvetica,sans-serif'><div style='display:none;max-height:0;overflow:hidden'>" + esc(title) + " - Butaca</div>"
                + "<table role='presentation' align='center' width='100%' cellpadding='0' cellspacing='0' style='max-width:640px;margin:auto;background:#fff;border:1px solid #dfd6cf;border-radius:10px;overflow:hidden'>"
                + "<tr><td bgcolor='#17191b' style='padding:24px 32px;border-bottom:4px solid #b80b32'><img src='cid:butaca-logo' alt='Butaca. Más eventos para vos' width='185' height='72' style='display:block;width:185px;height:72px;border:0'></td></tr>"
                + "<tr><td style='padding:32px'>" + body + "</td></tr><tr><td style='padding:20px 32px;background:#faf7f3;border-top:1px solid #eee8e2;font-size:11px;line-height:1.65;color:#62666a'>Butaca - Plataforma académica de entradas.<br>Operación de demostración: pago simulado. Comprobante sin validez fiscal.</td></tr></table></body></html>";
        text.append("\n").append(url).append("\n\nButaca - Operación académica, pago simulado. Comprobante sin validez fiscal.");
        return new Body(text.toString(), html);
    }

    static String subject(Notificacion notification) {
        return switch (notification.getTipo()) {
            case "COMPROBANTE" -> "Compra confirmada";
            case "CANCELACION" -> "Evento cancelado y reembolso registrado";
            case "REPROGRAMACION" -> "Nueva fecha de tu evento";
            default -> "Novedades de tus eventos";
        };
    }
    static String esc(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
