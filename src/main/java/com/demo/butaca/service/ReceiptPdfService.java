package com.demo.butaca.service;

import com.demo.butaca.model.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ReceiptPdfService {
    private final MailBrandAssets brand;
    public ReceiptPdfService(MailBrandAssets brand) { this.brand = brand; }

    public byte[] render(Compra purchase, Comprobante receipt) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.getDocumentInformation().setTitle("Comprobante " + receipt.getNumeroComprobante());
            document.getDocumentInformation().setAuthor("Butaca");
            document.getDocumentInformation().setSubject("Recibo académico de compra de entradas");
            document.getDocumentCatalog().setLanguage("es-AR");
            PDType0Font regular = PDType0Font.load(document, new ByteArrayInputStream(brand.fontBytes(false)));
            PDType0Font bold = PDType0Font.load(document, new ByteArrayInputStream(brand.fontBytes(true)));
            try (Layout page = new Layout(document, regular, bold, receipt.getNumeroComprobante(), brand.logo(true))) {
                page.heading("RECIBO DE COMPRA", 24);
                page.line(receipt.getNumeroComprobante(), 12, true, Layout.RED);
                page.gap(14);
                String issued = receipt.getFechaEmisionComprobante() == null ? "-" : receipt.getFechaEmisionComprobante().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm"));
                page.pair("FECHA DE EMISIÓN", issued, "COMPRA", "#" + purchase.getId());
                Usuario buyer = purchase.getCliente();
                String name = buyer == null ? "Comprador" : (buyer.getNombreUsuario() + " " + buyer.getApellidoUsuario()).trim();
                page.section("Datos de la compra");
                page.line(name, 12, true, Layout.INK);
                if (buyer != null) {
                    page.line("DNI " + value(buyer.getDniUsuario()) + "  |  " + value(buyer.getEmailUsuario()), 10, false, Layout.MUTED);
                }
                page.gap(10);
                Pago payment = receipt.getPago();
                String paymentMethod = payment == null ? "Pago registrado" : payment.getPasarelaPago().name() + " - " + payment.getMedioPago().name().replace('_', ' ')
                        + (payment.getUltimosCuatroDigitos() == null ? "" : " - terminación " + payment.getUltimosCuatroDigitos());
                page.line("Medio de pago: " + paymentMethod, 10, false, Layout.INK);
                if (payment != null) page.line("Referencia: " + value(payment.getReferenciaExternaPago()), 9, false, Layout.MUTED);
                page.section("Detalle de entradas");
                page.tableHeader();
                BigDecimal total = BigDecimal.ZERO;
                record Group(Long event, String title, String tariff, BigDecimal price) {}
                java.util.Map<Group, Integer> groups = new java.util.LinkedHashMap<>();
                for (Entrada ticket : purchase.getEntradas()) {
                    Evento event = ticket.getEvento();
                    String tariff = ticket.getTipoEntrada() == null ? "General" : ticket.getTipoEntrada().getNombreTipoEntrada();
                    groups.merge(new Group(event.getIdEvento(), event.getTituloEvento(), tariff, ticket.getPrecioPagado().stripTrailingZeros()), 1, Integer::sum);
                    total = total.add(ticket.getPrecioPagado());
                }
                for (var item : groups.entrySet()) {
                    Group group = item.getKey();
                    page.item(item.getValue(), group.title(), group.tariff() + " - Unitario " + money(group.price()), group.price().multiply(BigDecimal.valueOf(item.getValue())));
                }
                page.total(purchase.getEntradas().size(), total);
                if (!purchase.getEntradas().isEmpty()) {
                    Evento event = purchase.getEntradas().get(0).getEvento();
                    page.section("Información del evento");
                    page.line(event.getTituloEvento(), 11, true, Layout.INK);
                    page.line(event.getFechaEvento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - " + event.getHoraEvento() + " h", 10, false, Layout.INK);
                    if (event.getUbicacion() != null) {
                        page.line(value(event.getUbicacion().getNombreLugar()), 10, false, Layout.INK);
                        page.line(value(event.getUbicacion().getDireccion()), 9, false, Layout.MUTED);
                    }
                }
                page.section("Constancia e integridad");
                page.line("Este documento resume los importes históricos de la compra. Las entradas nominales con QR se adjuntan por separado.", 9, false, Layout.MUTED);
                page.gap(8);
                page.line("SHA-256 de integridad del registro", 8, true, Layout.MUTED);
                page.line(value(receipt.getHashCruceSeguridad()), 8, false, Layout.MUTED);
                page.gap(12);
                page.line("RECIBO ACADÉMICO - SIN VALIDEZ FISCAL", 9, true, Layout.RED);
                page.line("Operación de demostración. El pago y los reembolsos son simulados; el envío de este documento por correo es real.", 8, false, Layout.MUTED);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    static String value(String value) { return value == null || value.isBlank() ? "-" : value; }
    static String money(BigDecimal amount) { return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(amount); }

    private static class Layout implements AutoCloseable {
        static final Color INK = Color.decode("#17191B"), MUTED = Color.decode("#62666A"), RED = Color.decode("#B80B32");
        private final PDDocument document;
        private final PDType0Font regular, bold;
        private final PDImageXObject logo;
        private final String number;
        private PDPageContentStream stream;
        private float y;
        private int pageNumber;
        private final float width = PDRectangle.A4.getWidth();
        private final float height = PDRectangle.A4.getHeight();
        private final float margin = 48;

        Layout(PDDocument document, PDType0Font regular, PDType0Font bold, String number, byte[] png) throws IOException {
            this.document = document; this.regular = regular; this.bold = bold; this.number = number;
            logo = PDImageXObject.createFromByteArray(document, png, "Butaca");
            newPage();
        }
        void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4); document.addPage(page); pageNumber++;
            stream = new PDPageContentStream(document, page);
            rect(0, 0, width, 112, INK);
            stream.drawImage(logo, margin, height - 91, 163, 63.23f);
            at("COMPROBANTE DIGITAL", width - 203, 49, 8, true, Color.decode("#FFC36B"));
            at(number, width - 203, 69, 10, true, Color.WHITE);
            rect(0, 112, width, 3, RED);
            rule(height - 46);
            at("Butaca - Recibo académico / Pago simulado", margin, height - 29, 7, false, MUTED);
            at("Página " + pageNumber, width - 90, height - 29, 7, false, MUTED);
            y = 139;
        }
        void ensure(float room) throws IOException { if (y + room > height - 68) newPage(); }
        void gap(float amount) throws IOException { ensure(amount); y += amount; }
        void heading(String text, float size) throws IOException { line(text, size, true, INK); }
        void section(String title) throws IOException {
            ensure(60); y += 10; rule(y); y += 15; line(title, 10, true, INK); y += 3;
        }
        void pair(String firstLabel, String first, String secondLabel, String second) throws IOException {
            ensure(36); at(firstLabel, margin, y, 7, true, MUTED); at(secondLabel, 358, y, 7, true, MUTED); y += 17;
            at(first, margin, y, 10, false, INK); at(second, 358, y, 10, false, INK); y += 17;
        }
        void line(String text, float size, boolean strong, Color color) throws IOException {
            for (String item : wrap(text, strong ? bold : regular, size, width - margin * 2)) {
                ensure(size * 1.4f); at(item, margin, y, size, strong, color); y += size * 1.4f;
            }
        }
        void tableHeader() throws IOException {
            ensure(40); rect(margin, y - 10, width - margin * 2, 28, Color.decode("#F4EEE7"));
            at("DETALLE", margin + 12, y + 7, 8, true, INK);
            at("CANT.", 419, y + 7, 8, true, INK); at("IMPORTE", 473, y + 7, 8, true, INK); y += 30;
        }
        void item(int quantity, String title, String holder, BigDecimal amount) throws IOException {
            List<String> titleLines = wrap(title, bold, 10, 340);
            List<String> holderLines = wrap(holder, regular, 9, 340);
            float room = titleLines.size() * 15 + holderLines.size() * 13 + 21;
            if (y + room > height - 68) { newPage(); tableHeader(); }
            float top = y;
            for (String text : titleLines) { at(text, margin + 12, y, 10, true, INK); y += 15; }
            for (String text : holderLines) { at(text, margin + 12, y, 9, false, MUTED); y += 13; }
            at(String.valueOf(quantity), 431, top, 10, false, INK);
            String value = money(amount);
            at(value, width - margin - 12 - regular.getStringWidth(value) / 1000 * 10, top, 10, false, INK);
            y += 9; rule(y); y += 13;
        }
        void total(int quantity, BigDecimal total) throws IOException {
            ensure(75); y += 8;
            at(quantity + " entrada" + (quantity == 1 ? "" : "s") + " - Moneda ARS", margin, y, 9, false, MUTED);
            y += 20; at("TOTAL DE LA COMPRA", margin, y, 11, true, INK);
            String value = money(total); at(value, width - margin - bold.getStringWidth(value) / 1000 * 21, y, 21, true, INK); y += 12;
        }
        void rect(float x, float top, float w, float h, Color color) throws IOException { stream.setNonStrokingColor(color); stream.addRect(x, height - top - h, w, h); stream.fill(); }
        void rule(float top) throws IOException { stream.setStrokingColor(Color.decode("#DDD6CF")); stream.setLineWidth(.5f); stream.moveTo(margin, height - top); stream.lineTo(width - margin, height - top); stream.stroke(); }
        void at(String text, float x, float top, float size, boolean strong, Color color) throws IOException {
            stream.beginText(); stream.setFont(strong ? bold : regular, size); stream.setNonStrokingColor(color); stream.newLineAtOffset(x, height - top); stream.showText(clean(text, strong ? bold : regular)); stream.endText();
        }
        String clean(String text, PDType0Font font) throws IOException {
            StringBuilder value = new StringBuilder();
            for (int code : text.codePoints().toArray()) {
                if (Character.isISOControl(code)) { value.append(' '); continue; }
                String glyph = new String(Character.toChars(code));
                try { font.getStringWidth(glyph); value.append(glyph); }
                catch (IllegalArgumentException exception) { value.append('?'); }
            }
            return value.toString();
        }
        List<String> wrap(String value, PDType0Font font, float size, float max) throws IOException {
            String text = clean(ReceiptPdfService.value(value), font);
            List<String> lines = new ArrayList<>(); StringBuilder line = new StringBuilder();
            for (String word : text.split("\\s+")) {
                if (line.length() > 0 && font.getStringWidth(line + " " + word) / 1000 * size > max) { lines.add(line.toString()); line.setLength(0); }
                for (int code : word.codePoints().toArray()) {
                    String addition = new String(Character.toChars(code));
                    if (line.length() > 0 && font.getStringWidth(line + addition) / 1000 * size > max) { lines.add(line.toString()); line.setLength(0); }
                    line.append(addition);
                }
                line.append(' ');
            }
            if (line.length() > 0) lines.add(line.toString().stripTrailing());
            return lines;
        }
        @Override public void close() throws IOException { stream.close(); }
    }
}
