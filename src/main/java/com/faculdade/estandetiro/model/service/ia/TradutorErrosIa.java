package com.faculdade.estandetiro.model.service.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.errors.ApiException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;

/**
 * Converte qualquer erro vindo da IA (SDK do Gemini usado pelo Spring AI, ou RestClient)
 * em uma {@link IaException} com mensagem amigável em português.
 */
public final class TradutorErrosIa {

    private static final ObjectMapper JSON = new ObjectMapper();

    private TradutorErrosIa() {
    }

    /**
     * @param erro         exceção original
     * @param nomeVariavel nome da variável da chave usada (ex.: GEMINI_API_KEY_TEXT) — nunca o valor
     * @param modelo       modelo chamado
     */
    public static IaException traduzir(Throwable erro, String nomeVariavel, String modelo) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            if (t instanceof IaException ia) {
                return ia;
            }
            if (t instanceof ApiException api) {
                return deStatusHttp(api.code(), api.status(), api.message(), nomeVariavel, modelo);
            }
            if (t instanceof RestClientResponseException http) {
                JsonNode erroJson = lerErro(http.getResponseBodyAsString());
                return deStatusHttp(http.getStatusCode().value(), erroJson.path("status").asText(null),
                        erroJson.path("message").asText(http.getStatusText()), nomeVariavel, modelo);
            }
            if (t instanceof ResourceAccessException || t instanceof ConnectException
                    || t instanceof UnknownHostException || t instanceof HttpTimeoutException
                    || t instanceof InterruptedIOException) {
                return new IaException(TipoErroIa.SEM_CONEXAO,
                        "Não foi possível falar com a API do Gemini (sem internet ou tempo de resposta esgotado). "
                                + "Tente de novo em instantes.", resumir(t.getMessage()));
            }
        }
        // Último recurso: procura pistas no texto do erro
        String msg = String.valueOf(erro.getMessage());
        if (msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED")) {
            return deStatusHttp(429, "RESOURCE_EXHAUSTED", msg, nomeVariavel, modelo);
        }
        if (msg.contains("API key not valid") || msg.contains("API_KEY_INVALID")) {
            return deStatusHttp(400, "INVALID_ARGUMENT", msg, nomeVariavel, modelo);
        }
        if (msg.contains("404") || msg.contains("NOT_FOUND")) {
            return deStatusHttp(404, "NOT_FOUND", msg, nomeVariavel, modelo);
        }
        return new IaException(TipoErroIa.DESCONHECIDO, "Erro inesperado ao chamar a IA.", resumir(msg));
    }

    public static IaException deStatusHttp(int codigo, String status, String mensagemApi,
                                           String nomeVariavel, String modelo) {
        String m = mensagemApi == null ? "" : mensagemApi;
        String detalhe = "HTTP " + codigo + (status == null || status.isBlank() ? "" : " " + status)
                + (m.isBlank() ? "" : ": " + resumir(m));

        boolean chaveRecusada = m.contains("API key not valid") || m.contains("API_KEY_INVALID")
                || m.contains("API key expired") || m.contains("API keys are not supported");
        if (codigo == 401 || codigo == 403 || (codigo == 400 && chaveRecusada)) {
            return new IaException(TipoErroIa.CHAVE_INVALIDA,
                    "A chave " + nomeVariavel + " foi recusada pelo Gemini (inválida, expirada, sem permissão "
                            + "ou sem faturamento ativo). Confira o valor no arquivo .env e reinicie o sistema.", detalhe);
        }
        if (codigo == 404) {
            return new IaException(TipoErroIa.MODELO_NAO_ENCONTRADO,
                    "Modelo \"" + modelo + "\" não encontrado (erro 404). Confira o nome do modelo em "
                            + "src/main/resources/application.properties.", detalhe);
        }
        if (codigo == 429) {
            return new IaException(TipoErroIa.COTA_ESGOTADA,
                    "Cota da chave " + nomeVariavel + " estourada (erro 429: muitas requisições ou limite do plano). "
                            + "Aguarde alguns minutos e tente de novo.", detalhe);
        }
        if (codigo == 400) {
            return new IaException(TipoErroIa.REQUISICAO_INVALIDA,
                    "O Gemini recusou a requisição (erro 400). Revise o texto enviado ou a configuração do modelo.",
                    detalhe);
        }
        if (codigo >= 500) {
            return new IaException(TipoErroIa.SERVICO_INDISPONIVEL,
                    "O serviço do Gemini está instável agora (erro " + codigo + "). Tente de novo em instantes.",
                    detalhe);
        }
        return new IaException(TipoErroIa.DESCONHECIDO, "Erro inesperado da API do Gemini (" + codigo + ").", detalhe);
    }

    /** Lê o objeto "error" do JSON de erro do Gemini: {"error": {"code", "message", "status"}}. */
    private static JsonNode lerErro(String corpo) {
        try {
            return JSON.readTree(corpo == null || corpo.isBlank() ? "{}" : corpo).path("error");
        } catch (Exception e) {
            return JSON.createObjectNode();
        }
    }

    static String resumir(String texto) {
        if (texto == null) {
            return "";
        }
        String limpo = texto.replaceAll("\\s+", " ").trim();
        return limpo.length() > 300 ? limpo.substring(0, 300) + "…" : limpo;
    }
}
