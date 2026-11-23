package com.faculdade.estandetiro;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Imagens falsas para os testes (nenhum teste chama a IA de verdade). */
public final class ImagensDeTeste {

    private ImagensDeTeste() {
    }

    /** "Personagem" (corpo marrom + cabeça) sobre fundo magenta liso, como o pedido à IA. */
    public static byte[] figuraEmFundoMagenta(String formato) throws IOException {
        BufferedImage img = new BufferedImage(600, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 0, 255));
        g.fillRect(0, 0, 600, 900);
        g.setColor(new Color(90, 60, 40));
        g.fillRect(220, 260, 160, 520);
        g.setColor(new Color(200, 170, 140));
        g.fillOval(240, 120, 120, 140);
        g.dispose();
        return codificar(img, formato);
    }

    public static byte[] codificar(BufferedImage img, String formato) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(img, formato, saida);
        return saida.toByteArray();
    }
}
