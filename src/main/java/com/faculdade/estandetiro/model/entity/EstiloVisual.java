package com.faculdade.estandetiro.model.entity;

/** Estilo visual do cenario. O "termoPrompt" entra no lugar de [ESTILO] na base do prompt de imagem. */
public enum EstiloVisual {

    REALISTA("Realista", "realista"),
    PIXEL_ART("Pixel art", "pixel art"),
    CARTOON("Cartoon", "cartoon");

    private final String rotulo;
    private final String termoPrompt;

    EstiloVisual(String rotulo, String termoPrompt) {
        this.rotulo = rotulo;
        this.termoPrompt = termoPrompt;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getTermoPrompt() {
        return termoPrompt;
    }
}
