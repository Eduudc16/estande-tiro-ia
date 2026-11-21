package com.faculdade.estandetiro.config;

import com.faculdade.estandetiro.model.service.ia.ImageService;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * As DUAS chaves do Gemini, cada uma no seu bean (a autoconfiguração do Spring AI só aceita uma chave,
 * por isso ela está desligada em application.properties e os beans são criados aqui à mão):
 * <ul>
 *   <li>{@link ChatClient} (Spring AI) → chave GRATUITA {@code gemini.key.text} → todas as requisições de TEXTO;</li>
 *   <li>{@link ImageService} (RestClient) → chave FATURADA {@code gemini.key.image} → SOMENTE geração de IMAGEM.</li>
 * </ul>
 * Os valores das chaves vêm do .env (nunca do código).
 */
@Configuration
public class GenAiConfig {

    /** Marcador usado só para o sistema subir sem .env; as telas avisam que a chave falta. */
    private static final String SEM_CHAVE = "chave-nao-configurada";

    // ------------------------------------------------------------------ TEXTO (chave gratuita)

    @Bean
    public ChatModel geminiTextoChatModel(@Value("${gemini.key.text}") String chaveTexto,
                                          @Value("${gemini.text.model}") String modeloTexto,
                                          @Value("${gemini.text.timeout-segundos:60}") int timeoutSegundos) {
        Client clienteTexto = Client.builder()
                .apiKey(StringUtils.hasText(chaveTexto) ? chaveTexto.trim() : SEM_CHAVE)
                .vertexAI(false) // Gemini Developer API (Google AI Studio)
                .httpOptions(HttpOptions.builder().timeout(timeoutSegundos * 1000).build())
                .build();

        return GoogleGenAiChatModel.builder()
                .genAiClient(clienteTexto)
                .defaultOptions(GoogleGenAiChatOptions.builder().model(modeloTexto).build())
                // sem retentativas automáticas: cada clique faz no máximo 1 requisição (protege a cota gratuita)
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel geminiTextoChatModel) {
        return ChatClient.builder(geminiTextoChatModel).build();
    }

    // ------------------------------------------------------------------ IMAGEM (chave faturada)

    @Bean
    public ImageService imageService(RestClient.Builder restClientBuilder,
                                     @Value("${gemini.key.image}") String chaveImagem,
                                     @Value("${gemini.image.model}") String modeloImagem,
                                     @Value("${gemini.image.base-url}") String urlBase,
                                     @Value("${gemini.image.aspect-ratio:16:9}") String proporcao,
                                     @Value("${gemini.image.timeout-segundos:120}") int timeoutSegundos) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));

        boolean configurada = StringUtils.hasText(chaveImagem);
        RestClient restClient = restClientBuilder
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .defaultHeader("x-goog-api-key", configurada ? chaveImagem.trim() : SEM_CHAVE)
                .build();
        return new ImageService(restClient, modeloImagem, proporcao, configurada);
    }
}
