package com.faculdade.estandetiro.model.service.ia;

import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptImagemTest {

    @Test
    void baseTrocaTipoEEstiloEMantemAsRegras() {
        String base = PromptImagem.base(TipoEstande.VILA_RURAL, EstiloVisual.PIXEL_ART);

        assertThat(base)
                .startsWith("Cenário de fundo 2D para minigame de estande de tiro")
                .contains("Ambientação vila rural europeia: celeiro rústico")
                .contains("Balcão de madeira gasta em primeiro plano")
                .contains("Área central VAZIA")
                .contains("Sem personagens, sem pessoas, sem alvos, sem armas, sem texto, sem logotipos.")
                .endsWith("Estilo pixel art, proporção 16:9.");
    }

    @Test
    void oAmbienteInteiroMudaConformeOTipo() {
        for (TipoEstande tipo : TipoEstande.values()) {
            String base = PromptImagem.base(tipo, EstiloVisual.REALISTA);
            assertThat(base).contains(tipo.getAmbiente(), tipo.getBalcao(), tipo.getPiso(), tipo.getFundo());
            assertThat(PromptImagem.garantirRegras(base)).isEqualTo(base); // nenhuma regra obrigatória faltando
        }
        assertThat(PromptImagem.base(TipoEstande.CASTELO, EstiloVisual.REALISTA))
                .contains("castelo gótico", "paredes e colunas de pedra")
                .doesNotContain("celeiro", "Galpão de madeira");
        assertThat(PromptImagem.base(TipoEstande.MINA, EstiloVisual.REALISTA))
                .contains("galeria escavada na rocha")
                .doesNotContain("celeiro", "Galpão de madeira");
    }

    @Test
    void reanexaRegrasQuandoAIaEsquece() {
        String prompt = "Salão de pedra com candelabros e um vitral ao fundo.";
        String garantido = PromptImagem.garantirRegras(prompt);

        assertThat(garantido).startsWith(prompt).endsWith(PromptImagem.REGRAS_OBRIGATORIAS);
    }
}
