package com.faculdade.estandetiro;

import com.faculdade.estandetiro.model.entity.Arma;
import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.TipoArma;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.service.ArmaService;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import com.faculdade.estandetiro.model.service.ia.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cadastro de armas e página do minigame (o jogo em si roda no navegador, em jogo.js). */
@SpringBootTest(properties = {
        "GEMINI_API_KEY_TEXT=chave-de-teste",
        "GEMINI_API_KEY_IMAGE=chave-de-teste",
        "spring.datasource.url=jdbc:h2:mem:armas;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cenarios.pasta=target/test-cenarios/armas"
})
@AutoConfigureMockMvc
class ArmasEJogoTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ArmaService armaService;
    @Autowired
    EstandeTiroService estandeService;

    @MockitoBean
    ChatModel chatModel;
    @MockitoBean
    ImageService imageService;

    @Test
    void seisArmasDeExemploCadaUmaComSeuTipo() throws Exception {
        assertThat(armaService.listarTodas()).extracting(Arma::getTipo).containsExactly(
                TipoArma.PISTOLA, TipoArma.REVOLVER, TipoArma.ESCOPETA, TipoArma.RIFLE,
                TipoArma.SUBMETRALHADORA, TipoArma.LANCA_GRANADAS);

        mvc.perform(get("/armas")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Revólver Magnum")))
                .andExpect(content().string(containsString("Lança-granadas")));
    }

    @Test
    void cadastroDeArmaValidaOsNumeros() throws Exception {
        mvc.perform(post("/armas").param("nome", "Canhão").param("tipo", "PISTOLA").param("dano", "50")
                        .param("capacidade", "10").param("cadenciaMs", "200").param("recargaMs", "1000")
                        .param("ordem", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("O dano vai de 1 a 10.")));

        long antes = armaService.contar();
        mvc.perform(post("/armas").param("nome", "Pistola Pesada").param("tipo", "PISTOLA").param("dano", "2")
                        .param("capacidade", "8").param("cadenciaMs", "300").param("recargaMs", "1300")
                        .param("ordem", "7"))
                .andExpect(redirectedUrl("/armas"));
        assertThat(armaService.contar()).isEqualTo(antes + 1);
    }

    @Test
    void paginaDoJogoLevaAsArmasEOTemaDoEstande() throws Exception {
        EstandeTiro castelo = estandeService.listarTodos().stream()
                .filter(e -> e.getTipo() == TipoEstande.CASTELO).findFirst().orElseThrow();

        mvc.perform(get("/estandes/{id}/jogar", castelo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tema-castelo")))
                .andExpect(content().string(containsString("Cultistas encapuzados")))
                .andExpect(content().string(containsString("\"tipo\":\"ESCOPETA\"")))
                .andExpect(content().string(containsString("tipo: \"CASTELO\"")))
                .andExpect(content().string(containsString("/js/jogo-")));
    }

    @Test
    void telaDoEstandeTemBotaoDeJogarEOTema() throws Exception {
        EstandeTiro mina = estandeService.listarTodos().stream()
                .filter(e -> e.getTipo() == TipoEstande.MINA).findFirst().orElseThrow();

        mvc.perform(get("/estandes/{id}", mina.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tema-mina")))
                .andExpect(content().string(containsString("/estandes/" + mina.getId() + "/jogar")))
                .andExpect(content().string(not(containsString("data-testar-alvos"))));
    }
}
