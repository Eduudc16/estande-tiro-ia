package com.faculdade.estandetiro;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import com.faculdade.estandetiro.model.service.ia.ImageService;
import com.faculdade.estandetiro.model.service.ia.ImagemGerada;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

/** Com MAX_IMAGE_GENERATIONS=1, a segunda geração é barrada antes de chamar a API. */
@SpringBootTest(properties = {
        "GEMINI_API_KEY_TEXT=chave-de-teste",
        "GEMINI_API_KEY_IMAGE=chave-de-teste",
        "MAX_IMAGE_GENERATIONS=1",
        "spring.datasource.url=jdbc:h2:mem:limite;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cenarios.pasta=target/test-cenarios/limite"
})
@AutoConfigureMockMvc
class LimiteGeracoesFluxoTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    EstandeTiroService estandeService;

    @MockitoBean
    ChatModel chatModel;
    @MockitoBean
    ImageService imageService;

    @Test
    void segundaGeracaoEBarradaPeloLimite() throws Exception {
        when(imageService.isConfigurado()).thenReturn(true);
        when(imageService.getModelo()).thenReturn("gemini-3.1-flash-lite-image");
        when(imageService.gerar(anyString())).thenReturn(new ImagemGerada(FluxoEstandeIaTest.pngDeTeste(), "image/png"));

        Long primeiro = comPrompt("Primeiro").getId();
        Long segundo = comPrompt("Segundo").getId();

        mvc.perform(post("/estandes/{id}/cenario", primeiro)).andExpect(flash().attributeExists("sucesso"));
        mvc.perform(post("/estandes/{id}/cenario", segundo))
                .andExpect(flash().attribute("erro", containsString("Limite de 1 gerações")));

        verify(imageService, times(1)).gerar(anyString());
    }

    private EstandeTiro comPrompt(String nome) {
        EstandeTiro e = estandeService.criar(new EstandeTiro(nome, TipoEstande.CASTELO, EstiloVisual.REALISTA));
        estandeService.salvarDescricao(e.getId(), "Castelo.");
        return estandeService.salvarPromptImagem(e.getId(), "Castelo ao fundo, área central vazia, 16:9.");
    }
}
