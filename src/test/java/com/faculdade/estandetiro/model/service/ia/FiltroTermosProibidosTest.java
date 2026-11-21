package com.faculdade.estandetiro.model.service.ia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroTermosProibidosTest {

    @Test
    void removeNomesDeJogoEPersonagens() {
        var r = FiltroTermosProibidos.limpar(
                "Estande inspirado em Resident Evil 4, com o Mercador e o Leon ao fundo, madeira escura.");

        assertThat(r.alterado()).isTrue();
        assertThat(r.texto()).isEqualTo("Estande inspirado em, com o e o ao fundo, madeira escura.");
        assertThat(r.removidos()).contains("Resident Evil 4", "Mercador", "Leon");
    }

    @Test
    void naoMexeEmPalavrasComunsQueContemOsNomes() {
        String texto = "Escada de madeira, nada além de lampiões; uma caçada na mina.";
        var r = FiltroTermosProibidos.limpar(texto);

        assertThat(r.alterado()).isFalse();
        assertThat(r.texto()).isEqualTo(texto);
    }

    @Test
    void ignoraMaiusculasEMinusculas() {
        var r = FiltroTermosProibidos.limpar("cenário do RESIDENT EVIL com ashley");
        assertThat(r.texto()).isEqualTo("cenário do com");
    }
}
