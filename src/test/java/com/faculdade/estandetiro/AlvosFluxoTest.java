package com.faculdade.estandetiro;

import com.faculdade.estandetiro.model.entity.AlvoInimigo;
import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.repository.LogGeracaoImagemRepository;
import com.faculdade.estandetiro.model.service.AlvoInimigoService;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import com.faculdade.estandetiro.model.service.ia.IaException;
import com.faculdade.estandetiro.model.service.ia.ImageService;
import com.faculdade.estandetiro.model.service.ia.ImagemGerada;
import com.faculdade.estandetiro.model.service.ia.TipoErroIa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alvos gerados pela IA (simulada): recorte, cache, refazer recorte grátis, log e uso no jogo. */
@SpringBootTest(properties = {
        "GEMINI_API_KEY_TEXT=chave-de-teste",
        "GEMINI_API_KEY_IMAGE=chave-de-teste",
        "MAX_IMAGE_GENERATIONS=50",
        "spring.datasource.url=jdbc:h2:mem:alvos;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cenarios.pasta=target/test-cenarios/alvos",
        "app.alvos.pasta=target/test-alvos/publicos",
        "app.alvos.originais=target/test-alvos/originais"
})
@AutoConfigureMockMvc
class AlvosFluxoTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    AlvoInimigoService alvoService;
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
        when(imageService.isConfigurado()).thenReturn(true);
        when(imageService.getModelo()).thenReturn("gemini-3.1-flash-lite-image");
        when(imageService.gerar(anyString(), anyString()))
                .thenReturn(new ImagemGerada(ImagensDeTeste.figuraEmFundoMagenta("jpg"), "image/jpeg"));
    }

    @Test
    void telaMostraOsInimigosDeCadaTipo() throws Exception {
        mvc.perform(get("/alvos")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Aldeão com forcado")))
                .andExpect(content().string(containsString("Cultista com cajado")))
                .andExpect(content().string(containsString("Gerar imagem com IA")));
    }

    @Test
    void gerarAlvoRecortaSalvaEOJogoUsaAImagem() throws Exception {
        Long id = alvoService.listarPorTipo(TipoEstande.VILA_RURAL).get(0).getId();

        mvc.perform(post("/alvos/{id}/gerar", id))
                .andExpect(redirectedUrl("/alvos#tipo-vila_rural"))
                .andExpect(flash().attributeExists("sucesso"));

        AlvoInimigo depois = alvoService.buscarPorId(id);
        assertThat(depois.getCaminhoImagem()).isEqualTo("/assets/alvos/" + id + ".png");
        assertThat(Files.exists(Path.of("target/test-alvos/publicos", id + ".png"))).isTrue();
        assertThat(Files.exists(Path.of("target/test-alvos/originais", id + ".img"))).isTrue();
        mvc.perform(get("/assets/alvos/{id}.png", id)).andExpect(status().isOk());
        assertThat(logRepository.countByAlvoInimigoId(id)).isEqualTo(1);

        // o prompt pede fundo magenta, em pé (2:3), e não leva nomes de jogo nem de personagem
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(imageService).gerar(prompt.capture(), eq("2:3"));
        assertThat(prompt.getValue()).contains("magenta", "forcado").doesNotContainIgnoringCase("resident");

        // o jogo da vila passa a usar a imagem
        EstandeTiro vila = estandeService.listarTodos().stream()
                .filter(e -> e.getTipo() == TipoEstande.VILA_RURAL).findFirst().orElseThrow();
        mvc.perform(get("/estandes/{id}/jogar", vila.getId()))
                // o Thymeleaf escapa "/" como "\/" dentro do JavaScript (que lê igual a "/")
                .andExpect(content().string(containsString("\"imagem\":\"\\/assets\\/alvos\\/" + id + ".png\"")));

        // cache: não gera de novo; refazer o recorte é grátis
        mvc.perform(post("/alvos/{id}/gerar", id)).andExpect(flash().attribute("erro", containsString("cache")));
        mvc.perform(post("/alvos/{id}/recortar", id)).andExpect(flash().attributeExists("sucesso"));
        verify(imageService, times(1)).gerar(anyString(), anyString());

        // regenerar sempre passa pela tela de confirmação
        mvc.perform(post("/alvos/{id}/regenerar", id)).andExpect(redirectedUrl("/alvos/" + id + "/regenerar"));
        mvc.perform(get("/alvos/{id}/regenerar", id)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Sim, regenerar")));
    }

    @Test
    void falhaDaApiFicaNoLogEOAlvoContinuaSemImagem() throws Exception {
        AlvoInimigo foice = alvoService.listarPorTipo(TipoEstande.VILA_RURAL).get(2);
        when(imageService.gerar(anyString(), anyString())).thenThrow(new IaException(TipoErroIa.COTA_ESGOTADA,
                "Cota da chave GEMINI_API_KEY_IMAGE estourada (erro 429).", "HTTP 429 RESOURCE_EXHAUSTED"));

        mvc.perform(post("/alvos/{id}/gerar", foice.getId()))
                .andExpect(flash().attribute("erro", containsString("429")));

        assertThat(alvoService.buscarPorId(foice.getId()).temImagem()).isFalse();
        assertThat(logRepository.countByAlvoInimigoId(foice.getId())).isEqualTo(1);
        mvc.perform(get("/logs")).andExpect(content().string(containsString("Aldeã com foice")));
    }
}
