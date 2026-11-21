package com.faculdade.estandetiro.model.entity;

/**
 * Tipos de estande. A "ambientacao" entra no lugar de [TIPO] na base do prompt de imagem;
 * os "detalhesVisuais" são dicas extras que a IA usa para enriquecer a descrição e o prompt.
 */
public enum TipoEstande {

    VILA_RURAL("Vila rural", "vila rural europeia",
            "painel pintado ao fundo com casas de pedra, telhados de palha, cercas de madeira e a torre de uma "
                    + "igreja distante sob um céu nublado de fim de tarde"),
    CASTELO("Castelo", "castelo",
            "painel pintado ao fundo com um castelo gótico no alto de uma colina rochosa, céu tempestuoso, "
                    + "estandartes rasgados e muralhas de pedra"),
    BASE_MILITAR("Ilha / base militar", "base militar",
            "painel pintado ao fundo com uma ilha rochosa, torres de vigia, cercas de arame farpado e holofotes "
                    + "varrendo o mar escuro"),
    MINA("Mina", "mina",
            "paredes de rocha escoradas por vigas de madeira, trilhos de vagonete e um painel pintado com túneis "
                    + "de mina iluminados por lanternas"),
    PORTO("Porto / navio", "porto com navios antigos",
            "painel pintado ao fundo com navios antigos de velas rasgadas num mar revolto à noite e um cais de madeira");

    private final String rotulo;
    private final String ambientacao;
    private final String detalhesVisuais;

    TipoEstande(String rotulo, String ambientacao, String detalhesVisuais) {
        this.rotulo = rotulo;
        this.ambientacao = ambientacao;
        this.detalhesVisuais = detalhesVisuais;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getAmbientacao() {
        return ambientacao;
    }

    public String getDetalhesVisuais() {
        return detalhesVisuais;
    }
}
