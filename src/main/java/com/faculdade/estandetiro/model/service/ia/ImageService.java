package com.faculdade.estandetiro.model.service.ia;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Geração de IMAGEM com o Gemini (família Nano Banana), usando SOMENTE a chave faturada.
 * <p>
 * O Spring AI 1.1.x (linha compatível com Spring Boot 3) não tem modelo de imagem do Gemini,
 * então a chamada é REST direta na API ({@code models/{modelo}:generateContent}) com {@link RestClient}.
 * O bean é criado à mão em {@code GenAiConfig}, já com a chave GEMINI_API_KEY_IMAGE no cabeçalho.
 * Nenhuma requisição de texto passa por aqui.
 */
public class ImageService {

    public static final String VARIAVEL_CHAVE = "GEMINI_API_KEY_IMAGE";
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final RestClient restClient;
    private final String modelo;
    private final String proporcao;
    private final boolean configurado;

    public ImageService(RestClient restClient, String modelo, String proporcao, boolean configurado) {
        this.restClient = restClient;
        this.modelo = modelo;
        this.proporcao = proporcao;
        this.configurado = configurado;
    }

    /** Gera na proporção padrão (16:9, a dos cenários). */
    public ImagemGerada gerar(String prompt) {
        return gerar(prompt, proporcao);
    }

    /** Gera na proporção pedida (ex.: "2:3" para os alvos em pé). */
    public ImagemGerada gerar(String prompt, String proporcaoDaImagem) {
        if (!configurado) {
            throw IaException.chaveNaoConfigurada(VARIAVEL_CHAVE);
        }
        Map<String, Object> corpo = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseModalities", List.of("IMAGE"),
                        "imageConfig", Map.of("aspectRatio", proporcaoDaImagem)));

        JsonNode resposta;
        try {
            resposta = restClient.post()
                    .uri("/models/{modelo}:generateContent", modelo)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw TradutorErrosIa.traduzir(e, VARIAVEL_CHAVE, modelo);
        }
        ImagemGerada imagem = extrairImagem(resposta);
        log.info("Imagem gerada pelo modelo {} ({} bytes, {}). Uso: {}", modelo, imagem.bytes().length,
                imagem.mimeType(), resposta.path("usageMetadata"));
        return imagem;
    }

    /** Procura a parte com {@code inlineData} (imagem em base64) na resposta do generateContent. */
    static ImagemGerada extrairImagem(JsonNode resposta) {
        if (resposta == null) {
            throw new IaException(TipoErroIa.RESPOSTA_VAZIA, "A API de imagem respondeu vazio. Tente de novo.", null);
        }
        JsonNode candidatos = resposta.path("candidates");
        if (!candidatos.isArray() || candidatos.isEmpty()) {
            String bloqueio = resposta.path("promptFeedback").path("blockReason").asText("");
            throw new IaException(TipoErroIa.SEM_IMAGEM,
                    "A IA não devolveu nenhuma imagem" + (bloqueio.isEmpty() ? "." : " (prompt bloqueado: " + bloqueio + ").")
                            + " Ajuste o prompt e tente de novo.", bloqueio.isEmpty() ? null : "blockReason=" + bloqueio);
        }
        StringBuilder textoDaIa = new StringBuilder();
        for (JsonNode parte : candidatos.get(0).path("content").path("parts")) {
            JsonNode dados = parte.has("inlineData") ? parte.get("inlineData") : parte.path("inline_data");
            String base64 = dados.path("data").asText("");
            if (!base64.isEmpty()) {
                String mime = dados.has("mimeType") ? dados.get("mimeType").asText()
                        : dados.path("mime_type").asText("image/png");
                return new ImagemGerada(Base64.getMimeDecoder().decode(base64), mime);
            }
            if (parte.hasNonNull("text")) {
                textoDaIa.append(parte.get("text").asText()).append(' ');
            }
        }
        String motivo = candidatos.get(0).path("finishReason").asText("desconhecido");
        String detalhe = "finishReason=" + motivo
                + (textoDaIa.isEmpty() ? "" : "; resposta: " + TradutorErrosIa.resumir(textoDaIa.toString()));
        throw new IaException(TipoErroIa.SEM_IMAGEM,
                "A IA não devolveu imagem (motivo: " + motivo + "). Ajuste o prompt e tente de novo.", detalhe);
    }

    public String getModelo() {
        return modelo;
    }

    public String getProporcao() {
        return proporcao;
    }

    public boolean isConfigurado() {
        return configurado;
    }
}
