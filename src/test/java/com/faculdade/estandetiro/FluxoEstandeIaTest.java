package com.faculdade.estandetiro;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.repository.LogGeracaoImagemRepository;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import com.faculdade.estandetiro.model.service.ia.IaException;
import com.faculdade.estandetiro.model.service.ia.ImageService;
import com.faculdade.estandetiro.model.service.ia.ImagemGerada;
import com.faculdade.estandetiro.model.service.ia.TipoErroIa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo pelas telas, com a IA simulada: o ChatModel (texto) e o ImageService (imagem)
 * são mocks, então nenhum teste usa as chaves reais nem gasta cota.
 */
@SpringBootTest(properties = {
        "GEMINI_API_KEY_TEXT=chave-de-teste",
        "GEMINI_API_KEY_IMAGE=chave-de-teste",
        "MAX_IMAGE_GENERATIONS=50",
        "spring.datasource.url=jdbc:h2:mem:fluxo;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cenarios.pasta=target/test-cenarios/fluxo"
})
@AutoConfigureMockMvc
class FluxoEstandeIaTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    EstandeTiroService estandeService;
    @Autowired
    LogGeracaoImagemRepository logRepository;

    @MockitoBean
    ChatModel chatModel;
    @MockitoBean
    ImageService imageService;

    @BeforeEach
    void preparar() throws Exception {
        reset(chatModel, imageService);
        when(imageService.isConfigurado()).thenReturn(true);
        when(imageService.getModelo()).thenReturn("gemini-3.1-flash-lite-image");
        when(imageService.gerar(anyString())).thenReturn(new ImagemGerada(pngDeTeste(), "image/png"));
    }

    @Test
    void listaENovoEstandeComValidacao() throws Exception {
        mvc.perform(get("/estandes")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Galpão da Vila")));

        mvc.perform(post("/estandes").param("nome", " ").param("tipo", "MINA").param("estiloVisual", "CARTOON"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Informe o nome do estande.")));

        mvc.perform(post("/estandes").param("nome", "Túnel Novo").param("tipo", "MINA").param("estiloVisual", "CARTOON"))
                .andExpect(status().is3xxRedirection());
        assertThat(estandeService.listarTodos()).extracting(EstandeTiro::getNome).contains("Túnel Novo");
    }

    @Test
    void geraDescricaoEPromptPeloChatClientEFiltraNomes() throws Exception {
        EstandeTiro estande = estandeService.criar(new EstandeTiro("Salão Teste", TipoEstande.CASTELO, EstiloVisual.REALISTA));
        Long id = estande.getId();

        respostaDaIa("**Descrição:** Um salão de pedra úmida, lampiões tremendo e um castelo pintado ao fundo.");
        mvc.perform(post("/estandes/{id}/descricao", id))
                .andExpect(redirectedUrl("/estandes/" + id))
                .andExpect(flash().attributeExists("sucesso"));
        assertThat(estandeService.buscarPorId(id).getDescricaoGeradaPorIA())
                .isEqualTo("Um salão de pedra úmida, lampiões tremendo e um castelo pintado ao fundo.");

        respostaDaIa("\"Galpão de madeira no estilo Resident Evil com o Leon, castelo gótico pintado ao fundo.\"");
        mvc.perform(post("/estandes/{id}/prompt", id)).andExpect(flash().attributeExists("sucesso"));
        String prompt = estandeService.buscarPorId(id).getPromptImagem();
        assertThat(prompt).doesNotContainIgnoringCase("resident evil").doesNotContainIgnoringCase("leon")
                .contains("castelo gótico").contains("Sem personagens").endsWith("Proporção 16:9.");

        verify(chatModel, times(2)).call(any(Prompt.class));
        verify(imageService, never()).gerar(anyString()); // texto nunca passa pela chave de imagem
    }

    @Test
    void cenarioUsaCacheERegenerarExigeConfirmacao() throws Exception {
        EstandeTiro estande = comPrompt("Cache Teste");
        Long id = estande.getId();

        mvc.perform(post("/estandes/{id}/cenario", id)).andExpect(flash().attributeExists("sucesso"));
        assertThat(estandeService.buscarPorId(id).getCaminhoImagem()).isEqualTo("/assets/cenarios/" + id + ".png");
        assertThat(Files.exists(Path.of("target/test-cenarios/fluxo", id + ".png"))).isTrue();
        mvc.perform(get("/assets/cenarios/{id}.png", id)).andExpect(status().isOk());

        // já tem imagem: não chama a API de novo
        mvc.perform(post("/estandes/{id}/cenario", id))
                .andExpect(flash().attribute("erro", org.hamcrest.Matchers.containsString("cache")));
        verify(imageService, times(1)).gerar(anyString());

        // regenerar sem confirmação só leva para a tela de confirmação
        mvc.perform(post("/estandes/{id}/regenerar", id)).andExpect(redirectedUrl("/estandes/" + id + "/regenerar"));
        mvc.perform(get("/estandes/{id}/regenerar", id)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sim, regenerar")));
        verify(imageService, times(1)).gerar(anyString());

        mvc.perform(post("/estandes/{id}/regenerar", id).param("confirmacao", "sim"))
                .andExpect(flash().attribute("sucesso", org.hamcrest.Matchers.containsString("regenerado")));
        verify(imageService, times(2)).gerar(anyString());
        assertThat(logRepository.findTop5ByEstandeTiroIdOrderByDataHoraDescIdDesc(id)).hasSize(2);
    }

    @Test
    void erroDaApiDeImagemApareceNaTelaEFicaNoLog() throws Exception {
        EstandeTiro estande = comPrompt("Erro Teste");
        when(imageService.gerar(anyString())).thenThrow(new IaException(TipoErroIa.COTA_ESGOTADA,
                "Cota da chave GEMINI_API_KEY_IMAGE estourada (erro 429).", "HTTP 429 RESOURCE_EXHAUSTED"));

        mvc.perform(post("/estandes/{id}/cenario", estande.getId()))
                .andExpect(flash().attribute("erro", org.hamcrest.Matchers.containsString("429")))
                .andExpect(flash().attribute("erroDetalhe", "HTTP 429 RESOURCE_EXHAUSTED"));

        var logs = logRepository.findTop5ByEstandeTiroIdOrderByDataHoraDescIdDesc(estande.getId());
        assertThat(logs).singleElement().satisfies(l -> {
            assertThat(l.isSucesso()).isFalse();
            assertThat(l.getMensagemErro()).contains("429");
        });
        assertThat(estandeService.buscarPorId(estande.getId()).temImagem()).isFalse();

        mvc.perform(get("/logs")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Erro Teste")));
    }

    @Test
    void excluirMantemOLogDeGastos() throws Exception {
        EstandeTiro estande = comPrompt("Excluir Teste");
        mvc.perform(post("/estandes/{id}/cenario", estande.getId()));
        long logsAntes = logRepository.count();

        mvc.perform(post("/estandes/{id}/excluir", estande.getId())).andExpect(redirectedUrl("/estandes"));

        assertThat(logRepository.count()).isEqualTo(logsAntes);
        mvc.perform(get("/estandes/{id}", estande.getId())).andExpect(status().isNotFound());
        assertThat(Files.exists(Path.of("target/test-cenarios/fluxo", estande.getId() + ".png"))).isFalse();
    }

    private EstandeTiro comPrompt(String nome) {
        EstandeTiro e = estandeService.criar(new EstandeTiro(nome, TipoEstande.MINA, EstiloVisual.PIXEL_ART));
        estandeService.salvarDescricao(e.getId(), "Mina escura.");
        return estandeService.salvarPromptImagem(e.getId(), "Cenário de mina, área central vazia, 16:9.");
    }

    private void respostaDaIa(String texto) {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(texto)))));
    }

    static byte[] pngDeTeste() throws Exception {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(16, 9, BufferedImage.TYPE_INT_RGB), "png", saida);
        return saida.toByteArray();
    }
}
