package com.faculdade.estandetiro.model.service.ia;

import com.faculdade.estandetiro.ImagensDeTeste;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecorteAlvoTest {

    private static int alfa(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) >>> 24;
    }

    @Test
    void tiraOFundoMagentaRecortaReduzEPoeContorno() throws IOException {
        byte[] png = RecorteAlvo.processar(ImagensDeTeste.figuraEmFundoMagenta("jpg"));
        BufferedImage r = ImageIO.read(new ByteArrayInputStream(png));
        int margem = RecorteAlvo.CONTORNO_ESCURO + 2;

        assertThat(r.getHeight()).isEqualTo(RecorteAlvo.ALTURA_FINAL + 2 * margem);
        assertThat(r.getWidth()).isLessThan(r.getHeight() / 2);           // cortou o fundo dos lados
        assertThat(alfa(r, 0, 0)).isZero();
        assertThat(alfa(r, r.getWidth() - 1, r.getHeight() - 1)).isZero();

        int y = (int) (r.getHeight() * 0.6);
        int corpo = r.getRGB(r.getWidth() / 2, y);
        assertThat(corpo >>> 24).isEqualTo(255);
        assertThat(corpo & 255).as("azul do corpo marrom (sem tom magenta)").isLessThan(90);

        int contorno = r.getRGB(margem - 3, y);                          // logo fora do corpo
        assertThat(contorno >>> 24).isGreaterThan(200);
        assertThat((contorno >> 16) & 255).as("contorno claro").isGreaterThan(200);
        assertThat(alfa(r, 0, y)).isZero();
    }

    @Test
    void tiraTambemOFundoPresoDentroDoPersonagem() throws IOException {
        // personagem em forma de "anel": o buraco do meio é fundo magenta sem ligação com as bordas
        BufferedImage img = new BufferedImage(600, 900, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(255, 0, 255));
        g.fillRect(0, 0, 600, 900);
        g.setColor(new java.awt.Color(90, 60, 40));
        g.fillRect(150, 150, 300, 600);
        g.setColor(new java.awt.Color(255, 0, 255));
        g.fillRect(250, 350, 100, 200);
        g.dispose();

        BufferedImage r = ImageIO.read(new ByteArrayInputStream(
                RecorteAlvo.processar(ImagensDeTeste.codificar(img, "png"))));
        assertThat(alfa(r, r.getWidth() / 2, r.getHeight() / 2)).as("buraco do meio").isZero();
    }

    @Test
    void imagemSemPersonagemFalha() throws IOException {
        BufferedImage vazia = new BufferedImage(300, 450, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 300; i++) for (int j = 0; j < 450; j++) vazia.setRGB(i, j, 0xFF00FF);

        assertThatThrownBy(() -> RecorteAlvo.processar(ImagensDeTeste.codificar(vazia, "png")))
                .isInstanceOf(IOException.class).hasMessageContaining("não foi encontrado");
    }

    @Test
    void fundoQueNaoELisoFalha() throws IOException {
        BufferedImage ruido = new BufferedImage(300, 450, BufferedImage.TYPE_INT_RGB);
        Random r = new Random(7);
        for (int i = 0; i < 300; i++) for (int j = 0; j < 450; j++) ruido.setRGB(i, j, r.nextInt(0xFFFFFF));

        assertThatThrownBy(() -> RecorteAlvo.processar(ImagensDeTeste.codificar(ruido, "png")))
                .isInstanceOf(IOException.class).hasMessageContaining("fundo não estava liso");
    }
}
