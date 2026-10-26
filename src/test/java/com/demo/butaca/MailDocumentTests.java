package com.demo.butaca;

import com.demo.butaca.model.*;
import com.demo.butaca.service.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MailDocumentTests {
    private final MailBrandAssets brand = new MailBrandAssets();
    private final ReceiptPdfService pdfs = new ReceiptPdfService(brand);

    @Test void reciboContieneMarcaTextoSeleccionableImportesHistoricosYUnaPaginaParaCompraNormal() throws Exception {
        Compra purchase = purchase(3, false);
        Comprobante receipt = receipt(purchase);
        byte[] bytes = pdfs.render(purchase, receipt);
        Path directory = Path.of("target/mail-preview"); Files.createDirectories(directory);
        Files.write(directory.resolve("comprobante-butaca.pdf"), bytes);
        Files.write(directory.resolve("entrada-butaca.png"), new TicketImageService(brand).render(purchase.getEntradas().get(0)));
        try (var document = Loader.loadPDF(bytes)) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Lucía Fernández"));
            assertTrue(text.contains("RECIBO DE COMPRA"));
            assertTrue(text.contains("SIN VALIDEZ FISCAL"));
            assertTrue(text.contains("28.500,00"));
            assertTrue(text.contains("9.500,00"));
            assertTrue(text.contains("BUT-2026-000005"));
            assertTrue(text.contains("Café y música"));
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 120), "png", directory.resolve("comprobante-preview.png").toFile());
        }
    }

    @Test void detalleLargoPaginaSinPerderUltimaTarifaNiElTotal() throws Exception {
        Compra purchase = purchase(34, true);
        byte[] bytes = pdfs.render(purchase, receipt(purchase));
        try (var document = Loader.loadPDF(bytes)) {
            assertTrue(document.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Tarifa 34"));
            assertTrue(text.contains("TOTAL DE LA COMPRA"));
            assertTrue(text.contains("34 entradas"));
            assertTrue(text.contains("SIN VALIDEZ FISCAL"));
            Path directory = Path.of("target/mail-preview");
            Files.createDirectories(directory);
            Files.write(directory.resolve("comprobante-largo.pdf"), bytes);
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                ImageIO.write(renderer.renderImageWithDPI(page, 90), "png", directory.resolve("largo-" + (page + 1) + ".png").toFile());
            }
        }
    }

    private Compra purchase(int quantity, boolean distinct) {
        Usuario buyer = new Usuario(); buyer.setNombreUsuario("Lucía"); buyer.setApellidoUsuario("Fernández");
        buyer.setDniUsuario("44120987"); buyer.setEmailUsuario("lucia@example.test");
        Evento event = Evento.builder().idEvento(4L).tituloEvento(distinct ? "Una noche de música, encuentros y nuevas historias en el teatro de la ciudad" : "Café y música: sesiones de primavera")
                .fechaEvento(LocalDate.of(2026, 11, 15)).horaEvento(LocalTime.of(20, 30))
                .ubicacion(Ubicacion.builder().nombreLugar("Centro Cultural Recoleta").direccion("Junín 1930, Buenos Aires").build()).build();
        Compra purchase = Compra.builder().id(5L).cliente(buyer).fechaCompra(LocalDateTime.of(2026, 10, 24, 22, 15)).estadoCompra(EstadoCompra.CONFIRMADA).entradas(new ArrayList<>()).build();
        for (int index = 1; index <= quantity; index++) {
            TipoEntrada type = TipoEntrada.builder().idTipoEntrada((long) (distinct ? index : 1)).nombreTipoEntrada(distinct ? "Tarifa " + index : "General").build();
            purchase.getEntradas().add(Entrada.builder().idEntrada((long) index).evento(event).compra(purchase).tipoEntrada(type)
                    .nombreAsistente("Lucía Fernández").dniAsistente("44120987").precioPagado(new BigDecimal("9500.00"))
                    .codigoUnicoEntrada(UUID.fromString("d248a2da-7620-4d69-90d4-36f575df0b68")).build());
        }
        return purchase;
    }
    private Comprobante receipt(Compra purchase) {
        Pago payment = Pago.builder().compra(purchase).estadoPago(EstadoPago.APROBADO).pasarelaPago(PasarelaPago.MERCADOPAGO)
                .medioPago(MedioPago.TARJETA_CREDITO).ultimosCuatroDigitos("4242").referenciaExternaPago("PAY-MP-DEMO0005").build();
        return Comprobante.builder().pago(payment).numeroComprobante("BUT-2026-000005").fechaEmisionComprobante(purchase.getFechaCompra()).hashCruceSeguridad("a1b2c3d4".repeat(8)).build();
    }
}
