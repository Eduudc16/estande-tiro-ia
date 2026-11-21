package com.faculdade.estandetiro.model.service.ia;

import com.google.genai.errors.ClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Testa a chamada REST de imagem sem internet (servidor simulado), incluindo os erros tratados. */
class ImageServiceTest {

    private static final String URL =
            "https://api.teste/v1beta/models/gemini-3.1-flash-lite-image:generateContent";

    private MockRestServiceServer servidor;
    private ImageService service;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.teste/v1beta")
                .defaultHeader("x-goog-api-key", "chave-de-teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
        service = new ImageService(builder.build(), "gemini-3.1-flash-lite-image", "16:9", true);
    }

    @Test
    void enviaPromptComChaveDeImagemEProporcaoELeImagemBase64() {
        String png = Base64.getEncoder().encodeToString(new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4, 5});
        servidor.expect(requestTo(URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "chave-de-teste"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("prompt do cenário"))
                .andExpect(jsonPath("$.generationConfig.responseModalities[0]").value("IMAGE"))
                .andExpect(jsonPath("$.generationConfig.imageConfig.aspectRatio").value("16:9"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[
                          {"text":"aqui está"},
                          {"inlineData":{"mimeType":"image/png","data":"%s"}}]}}]}""".formatted(png),
                        MediaType.APPLICATION_JSON));

        ImagemGerada imagem = service.gerar("prompt do cenário");

        assertThat(imagem.mimeType()).isEqualTo("image/png");
        assertThat(imagem.bytes()).hasSize(9);
        servidor.verify();
    }

    @Test
    void erro429ViraCotaEsgotada() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":429,\"message\":\"Quota exceeded\",\"status\":\"RESOURCE_EXHAUSTED\"}}"));

        assertThatThrownBy(() -> service.gerar("x"))
                .isInstanceOfSatisfying(IaException.class, e -> {
                    assertThat(e.getTipo()).isEqualTo(TipoErroIa.COTA_ESGOTADA);
                    assertThat(e.getMessage()).contains("429").contains("GEMINI_API_KEY_IMAGE");
                    assertThat(e.getDetalhe()).contains("RESOURCE_EXHAUSTED");
                });
    }

    @Test
    void erro404ViraModeloNaoEncontrado() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":404,\"message\":\"models/x is not found\",\"status\":\"NOT_FOUND\"}}"));

        assertThatThrownBy(() -> service.gerar("x"))
                .isInstanceOfSatisfying(IaException.class, e -> {
                    assertThat(e.getTipo()).isEqualTo(TipoErroIa.MODELO_NAO_ENCONTRADO);
                    assertThat(e.getMessage()).contains("gemini-3.1-flash-lite-image");
                });
    }

    @Test
    void chaveInvalidaViraMensagemDeChave() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":400,\"message\":\"API key not valid. Please pass a valid API key.\","
                        + "\"status\":\"INVALID_ARGUMENT\"}}"));

        assertThatThrownBy(() -> service.gerar("x"))
                .isInstanceOfSatisfying(IaException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(TipoErroIa.CHAVE_INVALIDA));
    }

    @Test
    void respostaSemImagemViraErroAmigavel() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"candidates\":[{\"finishReason\":\"IMAGE_SAFETY\",\"content\":{\"parts\":[]}}]}",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.gerar("x"))
                .isInstanceOfSatisfying(IaException.class, e -> {
                    assertThat(e.getTipo()).isEqualTo(TipoErroIa.SEM_IMAGEM);
                    assertThat(e.getMessage()).contains("IMAGE_SAFETY");
                });
    }

    @Test
    void semChaveNemChamaAApi() {
        var semChave = new ImageService(RestClient.create(), "modelo", "16:9", false);
        assertThatThrownBy(() -> semChave.gerar("x"))
                .isInstanceOfSatisfying(IaException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(TipoErroIa.CHAVE_NAO_CONFIGURADA));
    }

    @Test
    void errosDoSdkDoSpringAiTambemSaoTraduzidos() {
        var erro = new RuntimeException("falhou", new ClientException(429, "RESOURCE_EXHAUSTED", "Quota exceeded"));
        IaException ia = TradutorErrosIa.traduzir(erro, "GEMINI_API_KEY_TEXT", "gemini-3.5-flash-lite");

        assertThat(ia.getTipo()).isEqualTo(TipoErroIa.COTA_ESGOTADA);
        assertThat(ia.getMessage()).contains("GEMINI_API_KEY_TEXT");
    }
}
