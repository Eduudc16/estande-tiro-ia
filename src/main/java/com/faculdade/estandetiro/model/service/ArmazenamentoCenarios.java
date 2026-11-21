package com.faculdade.estandetiro.model.service;

import com.faculdade.estandetiro.model.service.ia.ImagemGerada;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Salva as imagens de fundo em assets/cenarios/&lt;id&gt;.png (servidas em /assets/cenarios/&lt;id&gt;.png). */
@Component
public class ArmazenamentoCenarios {

    public static final String URL_BASE = "/assets/cenarios/";

    private final Path pasta;

    public ArmazenamentoCenarios(@Value("${app.cenarios.pasta:./assets/cenarios}") String pasta) {
        this.pasta = Path.of(pasta).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.pasta);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de cenários " + this.pasta, e);
        }
    }

    /** Grava a imagem como PNG e devolve a URL pública (ex.: /assets/cenarios/3.png). */
    public String salvar(Long estandeId, ImagemGerada imagem) {
        try {
            byte[] png = paraPng(imagem);
            Path destino = arquivo(estandeId);
            Path temporario = Files.createTempFile(pasta, estandeId + "-", ".tmp");
            Files.write(temporario, png);
            try {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return URL_BASE + estandeId + ".png";
        } catch (IOException e) {
            throw new RegraNegocioException("A imagem foi gerada, mas não foi possível salvar o arquivo: " + e.getMessage());
        }
    }

    public void excluir(Long estandeId) {
        try {
            Files.deleteIfExists(arquivo(estandeId));
        } catch (IOException ignorado) {
            // se o arquivo estiver em uso, ele só fica órfão na pasta
        }
    }

    public Path getPasta() {
        return pasta;
    }

    private Path arquivo(Long estandeId) {
        return pasta.resolve(estandeId + ".png");
    }

    /** O Nano Banana 2 Lite devolve JPEG; como o arquivo precisa ser &lt;id&gt;.png, converte quando não vier PNG. */
    static byte[] paraPng(ImagemGerada imagem) throws IOException {
        byte[] b = imagem.bytes();
        boolean ehPng = b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        if (ehPng) {
            return b;
        }
        BufferedImage lida = ImageIO.read(new ByteArrayInputStream(b));
        if (lida == null) {
            throw new IOException("formato de imagem não suportado (" + imagem.mimeType() + ")");
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(lida, "png", saida);
        return saida.toByteArray();
    }
}
