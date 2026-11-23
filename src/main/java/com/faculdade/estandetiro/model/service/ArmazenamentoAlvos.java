package com.faculdade.estandetiro.model.service;

import com.faculdade.estandetiro.model.service.ia.ImagemGerada;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Imagens dos alvos: o recorte pronto fica em assets/alvos/&lt;id&gt;.png (servido em /assets/alvos/&lt;id&gt;.png)
 * e a imagem crua devolvida pela IA fica em data/alvos-originais (fora do Git), para refazer o recorte sem pagar
 * uma nova geração.
 */
@Component
public class ArmazenamentoAlvos {

    public static final String URL_BASE = "/assets/alvos/";

    private final Path pasta;
    private final Path pastaOriginais;

    public ArmazenamentoAlvos(@Value("${app.alvos.pasta:./assets/alvos}") String pasta,
                              @Value("${app.alvos.originais:./data/alvos-originais}") String pastaOriginais) {
        this.pasta = Path.of(pasta).toAbsolutePath().normalize();
        this.pastaOriginais = Path.of(pastaOriginais).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.pasta);
            Files.createDirectories(this.pastaOriginais);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar as pastas dos alvos", e);
        }
    }

    /** Grava o recorte (PNG) e devolve a URL pública. */
    public String salvar(Long alvoId, byte[] png) throws IOException {
        gravar(pasta.resolve(alvoId + ".png"), png);
        return URL_BASE + alvoId + ".png";
    }

    public void salvarOriginal(Long alvoId, ImagemGerada imagem) throws IOException {
        gravar(pastaOriginais.resolve(alvoId + ".img"), imagem.bytes());
    }

    public Optional<byte[]> lerOriginal(Long alvoId) {
        Path arquivo = pastaOriginais.resolve(alvoId + ".img");
        try {
            return Files.exists(arquivo) ? Optional.of(Files.readAllBytes(arquivo)) : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Path getPasta() {
        return pasta;
    }

    private static void gravar(Path destino, byte[] conteudo) throws IOException {
        Path temporario = Files.createTempFile(destino.getParent(), "alvo-", ".tmp");
        Files.write(temporario, conteudo);
        try {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
