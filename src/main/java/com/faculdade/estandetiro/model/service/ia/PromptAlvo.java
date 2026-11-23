package com.faculdade.estandetiro.model.service.ia;

import com.faculdade.estandetiro.model.entity.AlvoInimigo;
import com.faculdade.estandetiro.model.entity.ClasseAlvo;

/**
 * Prompt da imagem de um alvo do minigame (inimigo ou refém). Pede um personagem original sobre fundo magenta
 * liso, que depois é removido em {@link RecorteAlvo}. Nunca leva nomes de jogos ou personagens.
 */
public final class PromptAlvo {

    /** Proporção pedida ao modelo: retrato, para caber o corpo inteiro. */
    public static final String PROPORCAO = "2:3";

    private static final String FINAL = " O personagem inteiro cabe na imagem, da cabeça aos pés, com margem em volta. "
            + "Fundo totalmente magenta puro (#FF00FF), liso e uniforme, sem chão, sem sombra, sem cenário, sem texto, "
            + "sem moldura.";

    private PromptAlvo() {
    }

    public static String montar(AlvoInimigo alvo) {
        String prompt = alvo.getClasse() == ClasseAlvo.REFEM ? refem(alvo) : inimigo(alvo);
        return FiltroTermosProibidos.limpar(prompt).texto();
    }

    private static String inimigo(AlvoInimigo alvo) {
        return "Ilustração de um ALVO para um minigame de estande de tiro: personagem fictício e original, "
                + alvo.getDescricaoVisual() + ". Corpo inteiro, em pé, de frente para o observador, com a arma "
                + "segurada abaixo da altura da cabeça e a cabeça bem visível no topo. Aparência ameaçadora de filme "
                + "de terror, olhos vermelhos brilhando, pintura digital realista com contornos nítidos." + FINAL;
    }

    /** A refém é uma vítima: nada de ar ameaçador, olhos brilhando ou armas. */
    private static String refem(AlvoInimigo alvo) {
        return "Ilustração de uma REFÉM para um minigame de estande de tiro: personagem fictícia e original, "
                + alvo.getDescricaoVisual() + ". Corpo inteiro, em pé, de frente para o observador, com expressão "
                + "de medo e olhos normais, sem nenhuma arma, roupas discretas. Pintura digital realista com contornos "
                + "nítidos, no mesmo clima sombrio de filme de terror." + FINAL;
    }
}
