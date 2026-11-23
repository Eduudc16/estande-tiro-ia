package com.faculdade.estandetiro.model.service.ia;

import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoEstande;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Base obrigatória do prompt de imagem e as regras que nunca podem faltar nele. */
public final class PromptImagem {

    /** Exigências que todo prompt de imagem precisa ter (são reanexadas se a IA esquecer alguma). */
    public static final String REGRAS_OBRIGATORIAS = "Área central VAZIA, onde os alvos vão aparecer. "
            + "Sem personagens, sem pessoas, sem alvos, sem armas, sem texto, sem logotipos. Proporção 16:9.";

    private static final List<String> TRECHOS_EXIGIDOS = List.of(
            "vazia", "sem personagens", "sem pessoas", "sem alvos", "sem armas", "sem texto", "sem logotipos", "16:9");

    private PromptImagem() {
    }

    /**
     * Monta a base do prompt trocando [TIPO] e [ESTILO]. O ambiente ao redor (paredes, teto, piso, balcão e fundo)
     * vem do tipo do estande; antes era sempre o mesmo galpão de madeira e só o painel do fundo mudava.
     */
    public static String base(TipoEstande tipo, EstiloVisual estilo) {
        return "Cenário de fundo 2D para minigame de estande de tiro, visão em primeira pessoa olhando para o "
                + "fundo do estande. Ambientação " + tipo.getAmbientacao() + ": " + tipo.getAmbiente()
                + ", clima de terror e suspense. " + tipo.getBalcao() + " em primeiro plano na parte inferior. "
                + "Área central VAZIA, com " + tipo.getPiso() + " e alguns trilhos/suportes, onde os alvos vão "
                + "aparecer; " + tipo.getFundo() + ". Sem personagens, sem pessoas, sem alvos, sem armas, sem texto, "
                + "sem logotipos. Estilo " + estilo.getTermoPrompt() + ", proporção 16:9.";
    }

    /** Se a IA tiver omitido alguma exigência, acrescenta as regras obrigatórias no final. */
    public static String garantirRegras(String prompt) {
        String texto = prompt == null ? "" : prompt.trim();
        String normalizado = semAcentos(texto.toLowerCase(Locale.ROOT));
        boolean completo = TRECHOS_EXIGIDOS.stream().allMatch(normalizado::contains);
        if (completo) {
            return texto;
        }
        return texto.isEmpty() ? REGRAS_OBRIGATORIAS : texto + " " + REGRAS_OBRIGATORIAS;
    }

    private static String semAcentos(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
