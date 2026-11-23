package com.faculdade.estandetiro.model.entity;

/**
 * Tipo da arma no minigame. O tipo define o COMPORTAMENTO do tiro (o diferencial), implementado em
 * static/js/jogo.js; os números (dano, pente, cadência e recarga) ficam em cada {@link Arma} cadastrada.
 */
public enum TipoArma {

    PISTOLA("Pistola", "Precisa e com recarga rápida: boa em qualquer situação."),
    REVOLVER("Revólver", "A bala atravessa escudos e todos os alvos na linha do tiro (inclusive a refém)."),
    ESCOPETA("Escopeta", "Espalha 6 chumbos numa área: derruba grupos, mas é arriscada perto da refém."),
    RIFLE("Rifle", "Zoom com o botão direito ou Shift; tiro na cabeça causa o triplo de dano."),
    SUBMETRALHADORA("Submetralhadora", "Automática: segure o clique para atirar em rajada, com pouca precisão."),
    LANCA_GRANADAS("Lança-granadas", "Explosão em área que ignora escudos e não poupa a refém.");

    private final String rotulo;
    private final String comportamento;

    TipoArma(String rotulo, String comportamento) {
        this.rotulo = rotulo;
        this.comportamento = comportamento;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getComportamento() {
        return comportamento;
    }
}
