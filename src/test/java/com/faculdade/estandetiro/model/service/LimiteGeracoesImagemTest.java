package com.faculdade.estandetiro.model.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteGeracoesImagemTest {

    @Test
    void reservaAteOMaximoEDevolveVaga() {
        var limite = new LimiteGeracoesImagem("2");

        assertThat(limite.tentarReservar()).isTrue();
        assertThat(limite.tentarReservar()).isTrue();
        assertThat(limite.tentarReservar()).isFalse();
        assertThat(limite.getRestantes()).isZero();

        limite.liberar(); // a API não devolveu imagem
        assertThat(limite.getUsadas()).isEqualTo(1);
        assertThat(limite.tentarReservar()).isTrue();
    }

    @Test
    void valoresVaziosOuInvalidosUsamOPadrao() {
        assertThat(LimiteGeracoesImagem.interpretar("")).isEqualTo(LimiteGeracoesImagem.PADRAO);
        assertThat(LimiteGeracoesImagem.interpretar(null)).isEqualTo(LimiteGeracoesImagem.PADRAO);
        assertThat(LimiteGeracoesImagem.interpretar("abc")).isEqualTo(LimiteGeracoesImagem.PADRAO);
        assertThat(LimiteGeracoesImagem.interpretar(" 7 ")).isEqualTo(7);
        assertThat(LimiteGeracoesImagem.interpretar("-3")).isZero();
    }

    @Test
    void zeroBloqueiaTudo() {
        assertThat(new LimiteGeracoesImagem("0").tentarReservar()).isFalse();
    }
}
