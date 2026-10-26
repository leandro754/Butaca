package com.demo.butaca.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Component
public class MailBrandAssets {
    private final byte[] light;
    private final byte[] dark;
    private final byte[] regular;
    private final byte[] bold;
    private final BufferedImage darkImage;
    private final Font regularFont;
    private final Font boldFont;

    public MailBrandAssets() {
        try {
            light = read("logo-light.png"); dark = read("logo-dark.png");
            regular = read("manrope-regular.ttf"); bold = read("manrope-bold.ttf");
            darkImage = ImageIO.read(new ByteArrayInputStream(dark));
            regularFont = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(regular));
            boldFont = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(bold));
        } catch (Exception exception) { throw new IllegalStateException("No se pudieron cargar los recursos de marca de Butaca", exception); }
    }

    private byte[] read(String name) throws Exception {
        try (var input = new ClassPathResource("mail/" + name).getInputStream()) { return input.readAllBytes(); }
    }
    public byte[] logo(boolean onDark) { return onDark ? dark : light; }
    public BufferedImage darkLogoImage() { return darkImage; }
    public byte[] fontBytes(boolean strong) { return strong ? bold : regular; }
    public Font font(boolean strong, float size) { return (strong ? boldFont : regularFont).deriveFont(size); }
}
