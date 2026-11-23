package com.faculdade.estandetiro.model.entity;

import java.util.Locale;

/** Classe do inimigo no minigame (a mesma usada em static/js/jogo.js). */
public enum ClasseAlvo {

    COMUM("Comum", "Cai com 1 de dano e vale 100 pontos."),
    AGIL("Ágil", "Mais rápido; cai com 1 de dano e vale 150 pontos."),
    FORTE("Forte", "Aguenta 4 de dano e vale 300 pontos.");

    private final String rotulo;
    private final String resumo;

    ClasseAlvo(String rotulo, String resumo) {
        this.rotulo = rotulo;
        this.resumo = resumo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getResumo() {
        return resumo;
    }

    /** Nome da classe no JavaScript do jogo: comum, agil ou forte. */
    public String getChaveJogo() {
        return name().toLowerCase(Locale.ROOT);
    }
}
