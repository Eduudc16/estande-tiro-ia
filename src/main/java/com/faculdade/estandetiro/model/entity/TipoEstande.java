package com.faculdade.estandetiro.model.entity;

import java.util.Locale;

/**
 * Tipos de estande. Cada tipo define o AMBIENTE INTEIRO do cenário (paredes, teto, piso, balcão e fundo),
 * não só a imagem do fundo, e também o grupo de inimigos do minigame.
 */
public enum TipoEstande {

    VILA_RURAL("Vila rural", "vila rural europeia", "Aldeões hostis",
            "celeiro rústico de madeira escura com teto de vigas, fardos de feno, ferramentas agrícolas e réstias "
                    + "de alho penduradas, iluminado por lampiões a óleo amarelados",
            "Balcão de madeira gasta",
            "chão de terra batida coberto de palha",
            "ao fundo, as portas abertas do celeiro mostram a vila sob um céu nublado de fim de tarde, com casas "
                    + "de pedra, telhados de palha e a torre de uma igreja"),
    CASTELO("Castelo", "castelo gótico", "Cultistas encapuzados",
            "grande salão de um castelo gótico, com paredes e colunas de pedra, arcos ogivais, tapeçarias rasgadas, "
                    + "candelabros de ferro com velas acesas e armaduras decorativas nas laterais",
            "Balcão de pedra com tampo de madeira escura entalhada",
            "piso de lajotas de pedra",
            "ao fundo, um grande vitral escuro iluminado por relâmpagos e um portão de ferro"),
    BASE_MILITAR("Ilha / base militar", "base militar numa ilha", "Soldados mascarados",
            "estande de treinamento dentro de um bunker de concreto, com vigas de aço, sacos de areia, caixotes "
                    + "militares, grades metálicas, holofotes de luz fria e luzes de emergência vermelhas",
            "Balcão de chapa de aço com faixas de alerta amarelas e pretas",
            "piso de concreto manchado",
            "ao fundo, uma parede de concreto marcada de tiros e uma porta blindada entreaberta"),
    MINA("Mina", "mina abandonada", "Mineiros possuídos",
            "galeria escavada na rocha de uma mina abandonada, com paredes de pedra bruta escoradas por vigas de "
                    + "madeira, caixotes de dinamite, correntes e lanternas a óleo penduradas",
            "Balcão improvisado com tábuas sobre caixotes",
            "chão de cascalho com trilhos de vagonete",
            "ao fundo, a boca escura de um túnel com um vagonete abandonado"),
    PORTO("Porto / navio", "porto antigo à noite", "Piratas sombrios",
            "armazém velho de um cais, com paredes de madeira úmida, redes de pesca, barris, cordas, correntes e "
                    + "lanternas de navio balançando",
            "Balcão feito de tábuas de convés apoiadas em barris",
            "assoalho de madeira molhada",
            "ao fundo, um portão aberto mostra navios antigos de velas rasgadas num mar revolto sob a lua");

    private final String rotulo;
    private final String ambientacao;
    private final String inimigos;
    private final String ambiente;
    private final String balcao;
    private final String piso;
    private final String fundo;

    TipoEstande(String rotulo, String ambientacao, String inimigos, String ambiente, String balcao, String piso,
                String fundo) {
        this.rotulo = rotulo;
        this.ambientacao = ambientacao;
        this.inimigos = inimigos;
        this.ambiente = ambiente;
        this.balcao = balcao;
        this.piso = piso;
        this.fundo = fundo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Nome curto da ambientação, usado no lugar de [TIPO] na base do prompt. */
    public String getAmbientacao() {
        return ambientacao;
    }

    /** Grupo de inimigos que aparece no minigame deste tipo. */
    public String getInimigos() {
        return inimigos;
    }

    /** Paredes, teto, iluminação e decoração ao redor da área dos alvos. */
    public String getAmbiente() {
        return ambiente;
    }

    public String getBalcao() {
        return balcao;
    }

    public String getPiso() {
        return piso;
    }

    public String getFundo() {
        return fundo;
    }

    /** Classe CSS do tema (moldura e efeitos de ambiente nas telas), ex.: "vila_rural". */
    public String getCssTema() {
        return name().toLowerCase(Locale.ROOT);
    }
}
