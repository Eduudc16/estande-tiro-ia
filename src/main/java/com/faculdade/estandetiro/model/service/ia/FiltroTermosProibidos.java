package com.faculdade.estandetiro.model.service.ia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Garante que nenhum nome de jogo, franquia ou personagem vá para a geração de imagem:
 * o prompt enviado leva só a descrição visual.
 */
public final class FiltroTermosProibidos {

    /** Nomes que nunca podem ir no prompt de imagem (comparação sem diferenciar maiúsculas). */
    static final List<String> TERMOS = List.of(
            "Resident Evil 4", "Resident Evil", "Biohazard", "Capcom", "RE4",
            "Leon S. Kennedy", "Leon Kennedy", "Leon", "Kennedy",
            "Ashley Graham", "Ashley", "Ada Wong", "Ada", "Luis Sera", "Luis",
            "Ramon Salazar", "Salazar", "Osmund Saddler", "Saddler", "Jack Krauser", "Krauser",
            "Bitores Mendez", "Méndez", "Mendez", "Hunnigan",
            "Mercador", "Merchant", "Ganados", "Ganado", "Los Illuminados", "Las Plagas", "Plagas");

    private static final List<Pattern> PADROES = TERMOS.stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .map(t -> Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(t) + "(?![\\p{L}\\p{N}])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
            .toList();

    private FiltroTermosProibidos() {
    }

    public record Resultado(String texto, List<String> removidos) {
        public boolean alterado() {
            return !removidos.isEmpty();
        }
    }

    public static Resultado limpar(String texto) {
        if (texto == null || texto.isBlank()) {
            return new Resultado(texto == null ? "" : texto, List.of());
        }
        List<String> removidos = new ArrayList<>();
        String resultado = texto;
        for (Pattern padrao : PADROES) {
            Matcher m = padrao.matcher(resultado);
            if (m.find()) {
                removidos.add(m.group());
                resultado = m.replaceAll("");
            }
        }
        if (removidos.isEmpty()) {
            return new Resultado(texto, List.of());
        }
        // arruma espaços e pontuação que sobram depois de tirar as palavras
        resultado = resultado
                .replaceAll("\\(\\s*\\)", "")
                .replaceAll("\"\\s*\"", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\s+([,.;:!?])", "$1")
                .replaceAll("([,;])\\s*([,.;])", "$2")
                .trim();
        return new Resultado(resultado, List.copyOf(removidos));
    }
}
