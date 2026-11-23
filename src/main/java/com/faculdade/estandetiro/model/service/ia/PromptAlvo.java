package com.faculdade.estandetiro.model.service.ia;

import com.faculdade.estandetiro.model.entity.AlvoInimigo;

/**
 * Prompt da imagem de um inimigo (alvo do minigame). Pede um personagem original sobre fundo magenta liso,
 * que depois é removido em {@link RecorteAlvo}. Nunca leva nomes de jogos ou personagens.
 */
public final class PromptAlvo {

    /** Proporção pedida ao modelo: retrato, para caber o corpo inteiro. */
    public static final String PROPORCAO = "2:3";

    private PromptAlvo() {
    }

    public static String montar(AlvoInimigo alvo) {
        String prompt = "Ilustração de um ALVO para um minigame de estande de tiro: personagem fictício e original, "
                + alvo.getDescricaoVisual() + ". Corpo inteiro, em pé, de frente para o observador, com a arma "
                + "segurada abaixo da altura da cabeça e a cabeça bem visível no topo. Aparência ameaçadora de filme "
                + "de terror, olhos vermelhos brilhando, pintura digital realista com contornos nítidos. O personagem "
                + "inteiro cabe na imagem, da cabeça aos pés, com margem em volta. Fundo totalmente magenta puro "
                + "(#FF00FF), liso e uniforme, sem chão, sem sombra, sem cenário, sem texto, sem moldura.";
        return FiltroTermosProibidos.limpar(prompt).texto();
    }
}
