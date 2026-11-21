package com.faculdade.estandetiro.model.service.ia;

import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptImagemTest {

    @Test
    void baseTrocaTipoEEstilo() {
        String base = PromptImagem.base(TipoEstande.VILA_RURAL, EstiloVisual.PIXEL_ART);

        assertThat(base)
                .startsWith("Cenário de fundo 2D para minigame de estande de tiro")
                .contains("ambientação vila rural europeia.")
                .contains("Área central VAZIA")
                .contains("Sem personagens, sem pessoas, sem alvos, sem armas, sem texto, sem logotipos.")
                .endsWith("Estilo pixel art, proporção 16:9.");
    }

    @Test
    void basePassaNaVerificacaoDeRegras() {
        String base = PromptImagem.base(TipoEstande.CASTELO, EstiloVisual.REALISTA);
        assertThat(PromptImagem.garantirRegras(base)).isEqualTo(base);
    }

    @Test
    void reanexaRegrasQuandoAIaEsquece() {
        String prompt = "Galpão de madeira com lampiões e um castelo pintado ao fundo.";
        String garantido = PromptImagem.garantirRegras(prompt);

        assertThat(garantido).startsWith(prompt).endsWith(PromptImagem.REGRAS_OBRIGATORIAS);
    }
}
