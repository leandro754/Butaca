package com.demo.butaca.service;

import com.demo.butaca.model.Entrada;
import com.demo.butaca.model.Evento;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TicketImageService {
    private static final Color INK = Color.decode("#17191B"), MUTED = Color.decode("#62666A"), RED = Color.decode("#B80B32");
    private final MailBrandAssets brand;
    public TicketImageService(MailBrandAssets brand) { this.brand = brand; }

    public byte[] render(Entrada ticket) throws Exception {
        Evento event = ticket.getEvento();
        BufferedImage image = new BufferedImage(1600, 940, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setColor(Color.decode("#F7EBDD")); graphics.fillRect(0, 0, 1600, 940);
            graphics.setColor(INK); graphics.fillRect(0, 0, 1600, 174);
            graphics.drawImage(brand.darkLogoImage(), 64, 21, 323, 125, null);
            graphics.setColor(Color.decode("#FFC36B")); graphics.setFont(brand.font(true, 18));
            graphics.drawString("ENTRADA NOMINAL", 1100, 66);
            graphics.setColor(Color.decode("#FFF9F0")); graphics.setFont(brand.font(false, 19));
            graphics.drawString("ACCESO INDIVIDUAL", 1100, 102);
            graphics.setColor(RED); graphics.fillRect(0, 174, 1600, 5);
            graphics.setColor(Color.WHITE); graphics.fillRect(1040, 179, 560, 660);
            graphics.setColor(Color.decode("#D5C6B8"));
            graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{6, 10}, 0));
            graphics.drawLine(1040, 196, 1040, 816);
            graphics.setStroke(new BasicStroke(1));
            label(graphics, "TU PRÓXIMA SALIDA", 64, 231);
            graphics.setFont(brand.font(true, 48)); graphics.setColor(INK);
            List<String> title = wrap(graphics, event.getTituloEvento(), 900, 3);
            int y = 292;
            for (String line : title) { graphics.drawString(line, 64, y); y += 59; }
            label(graphics, "FECHA Y HORA", 64, 452);
            graphics.setFont(brand.font(true, 29)); graphics.setColor(INK);
            String date = event.getFechaEvento().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-AR")));
            graphics.drawString(date + " - " + event.getHoraEvento().format(DateTimeFormatter.ofPattern("HH:mm")) + " h", 64, 495);
            label(graphics, "LUGAR", 64, 552);
            graphics.setFont(brand.font(true, 26)); graphics.setColor(INK);
            singleLine(graphics, event.getUbicacion().getNombreLugar(), 64, 590, 900);
            graphics.setFont(brand.font(false, 19)); graphics.setColor(MUTED);
            singleLine(graphics, event.getUbicacion().getDireccion(), 64, 623, 900);
            label(graphics, "TITULAR", 64, 686);
            graphics.setFont(brand.font(true, 29)); graphics.setColor(INK);
            singleLine(graphics, ticket.getNombreAsistente(), 64, 727, 610);
            label(graphics, "DNI", 740, 686);
            graphics.setFont(brand.font(true, 29)); graphics.setColor(INK);
            graphics.drawString(ticket.getDniAsistente(), 740, 727);
            graphics.setColor(Color.decode("#D5C6B8")); graphics.drawLine(64, 761, 974, 761);
            graphics.setColor(INK); graphics.setFont(brand.font(true, 22));
            singleLine(graphics, ticket.getTipoEntrada() == null ? "General" : ticket.getTipoEntrada().getNombreTipoEntrada(), 64, 806, 610);
            graphics.setFont(brand.font(true, 25)); graphics.drawString(ReceiptPdfService.money(ticket.getPrecioPagado()), 740, 806);
            var qr = new QRCodeWriter().encode(ticket.getCodigoUnicoEntrada().toString(), BarcodeFormat.QR_CODE, 420, 420);
            for (int row = 0; row < qr.getHeight(); row++) for (int column = 0; column < qr.getWidth(); column++) {
                image.setRGB(1110 + column, 268 + row, qr.get(column, row) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
            graphics.setColor(INK); graphics.setFont(brand.font(true, 22));
            centered(graphics, "Presentá este QR y tu DNI", 1320, 739);
            graphics.setColor(MUTED); graphics.setFont(brand.font(false, 18));
            centered(graphics, "Guardá la imagen en tu teléfono.", 1320, 772);
            centered(graphics, "No compartas tu código de acceso.", 1320, 801);
            graphics.setColor(INK); graphics.setFont(brand.font(false, 17));
            graphics.drawString("CÓDIGO ÚNICO DE ENTRADA", 64, 870);
            graphics.setFont(new Font("Monospaced", Font.PLAIN, 22));
            graphics.drawString(ticket.getCodigoUnicoEntrada().toString(), 64, 906);
            graphics.setFont(brand.font(false, 15)); graphics.setColor(MUTED);
            centered(graphics, "DEMO ACADÉMICA - PAGO SIMULADO", 1320, 896);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(image, "png", bytes); return bytes.toByteArray();
        } finally { graphics.dispose(); }
    }
    private void label(Graphics2D graphics, String value, int x, int y) {
        graphics.setColor(RED); graphics.setFont(brand.font(true, 16)); graphics.drawString(value, x, y);
    }
    private void centered(Graphics2D graphics, String value, int center, int y) {
        graphics.drawString(value, center - graphics.getFontMetrics().stringWidth(value) / 2, y);
    }
    private void singleLine(Graphics2D graphics, String text, int x, int y, int width) {
        graphics.drawString(wrap(graphics, text, width, 1).get(0), x, y);
    }
    private List<String> wrap(Graphics2D graphics, String text, int width, int maximum) {
        String remaining = text == null || text.isBlank() ? "-" : text.replaceAll("\\s+", " ").trim();
        List<String> lines = new ArrayList<>();
        while (!remaining.isEmpty() && lines.size() < maximum) {
            if (graphics.getFontMetrics().stringWidth(remaining) <= width) { lines.add(remaining); break; }
            int end = remaining.length();
            while (end > 1 && graphics.getFontMetrics().stringWidth(remaining.substring(0, end) + "…") > width) end--;
            int space = remaining.lastIndexOf(' ', end);
            if (space > end / 2) end = space;
            String line = remaining.substring(0, end).trim(); remaining = remaining.substring(end).stripLeading();
            if (lines.size() == maximum - 1) line += "…";
            lines.add(line);
        }
        return lines;
    }
}
