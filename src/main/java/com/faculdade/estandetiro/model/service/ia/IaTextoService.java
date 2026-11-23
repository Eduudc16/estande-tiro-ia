package com.faculdade.estandetiro.model.service.ia;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.service.RegraNegocioException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Funcionalidades de TEXTO com GenAI, sempre pelo {@link ChatClient} do Spring AI
 * (configurado em {@code GenAiConfig} com a chave gratuita GEMINI_API_KEY_TEXT).
 */
@Service
public class IaTextoService {

    public static final String VARIAVEL_CHAVE = "GEMINI_API_KEY_TEXT";

    static final String SISTEMA_DESCRICAO = """
            Você é roteirista de jogos de terror e suspense. Escreva a descrição de um ESTANDE DE TIRO de um \
            minigame, montado dentro do ambiente do tema (por exemplo: um celeiro, o salão de um castelo, um \
            bunker, a galeria de uma mina), onde alvos surgem e somem diante do jogador.
            Regras:
            - Português do Brasil, um único parágrafo de 3 a 5 frases (no máximo 90 palavras).
            - Clima de terror e suspense: luz fraca, sombras, sons distantes.
            - O AMBIENTE INTEIRO combina com o tema: paredes, teto, piso, iluminação, objetos e o fundo do estande.
            - Não cite nomes de jogos, franquias, empresas ou personagens.
            - Responda só com a descrição, sem título, sem markdown e sem aspas.""";

    static final String SISTEMA_PROMPT = """
            Você escreve prompts para um modelo de geração de imagens. Transforme a descrição de um cenário em UM \
            prompt otimizado para gerar a IMAGEM DE FUNDO de um minigame de estande de tiro.
            O prompt DEVE seguir a base abaixo e manter todas as exigências dela (visão em primeira pessoa, balcão \
            em primeiro plano, área central vazia para os alvos, nada de personagens, pessoas, alvos, armas, texto \
            ou logotipos, o estilo pedido e a proporção 16:9):

            BASE: [BASE]

            Como melhorar a base:
            - o ambiente INTEIRO precisa combinar com o tema: paredes, teto, piso, iluminação, decoração e o que \
            aparece ao fundo. Não transforme o lugar num galpão de madeira se o tema for outro;
            - acrescente detalhes visuais concretos tirados da descrição: materiais, cores, iluminação e objetos;
            - mantenha a área central livre, com trilhos paralelos e suportes vazios por onde os alvos deslizam;
            - frases descritivas e objetivas; nada de regras de jogo, pontuação ou narrativa.
            Proibido: nomes de jogos, franquias, empresas, personagens ou pessoas reais; figuras humanas ou criaturas.
            Responda SOMENTE com o prompt final, em português, em um único parágrafo, sem markdown e sem aspas.""";

    private final ChatClient chatClient;
    private final String modelo;
    private final boolean configurado;

    public IaTextoService(ChatClient chatClient,
                          @Value("${gemini.text.model}") String modelo,
                          @Value("${gemini.key.text}") String chaveTexto) {
        this.chatClient = chatClient;
        this.modelo = modelo;
        this.configurado = StringUtils.hasText(chaveTexto);
    }

    /** Funcionalidade 1: descrição curta do cenário, com clima de terror e suspense. */
    public String gerarDescricao(EstandeTiro estande) {
        String usuario = """
                Nome do estande: %s
                Tipo de ambientação: %s (%s)
                Ambiente deste tipo: %s
                Estilo visual: %s
                Escreva a descrição do cenário.""".formatted(
                estande.getNome(), estande.getTipo().getRotulo(), estande.getTipo().getAmbientacao(),
                ambienteCompleto(estande), estande.getEstiloVisual().getRotulo());
        return limitar(limpar(chamar(SISTEMA_DESCRICAO, usuario)), 2000);
    }

    /** Funcionalidade 2: transforma a descrição em prompt de imagem, seguindo a base obrigatória. */
    public String gerarPromptImagem(EstandeTiro estande) {
        if (!estande.temDescricao()) {
            throw new RegraNegocioException("Gere (ou escreva) a descrição do estande antes de gerar o prompt de imagem.");
        }
        String base = PromptImagem.base(estande.getTipo(), estande.getEstiloVisual());
        String usuario = """
                Tipo de ambientação: %s
                Ambiente deste tipo: %s
                Estilo visual: %s
                Descrição do cenário: %s""".formatted(
                estande.getTipo().getAmbientacao(), ambienteCompleto(estande),
                estande.getEstiloVisual().getTermoPrompt(), estande.getDescricaoGeradaPorIA());
        String prompt = limpar(chamar(SISTEMA_PROMPT.replace("[BASE]", base), usuario));
        prompt = FiltroTermosProibidos.limpar(prompt).texto();
        return limitar(PromptImagem.garantirRegras(prompt), 4000);
    }

    /** Paredes/teto, balcão, piso e fundo do tipo, numa linha só. */
    private static String ambienteCompleto(EstandeTiro estande) {
        var t = estande.getTipo();
        return t.getAmbiente() + "; " + t.getBalcao().toLowerCase(java.util.Locale.ROOT) + " em primeiro plano; "
                + t.getPiso() + "; "
                + t.getFundo();
    }

    private String chamar(String sistema, String usuario) {
        if (!configurado) {
            throw IaException.chaveNaoConfigurada(VARIAVEL_CHAVE);
        }
        String resposta;
        try {
            resposta = chatClient.prompt()
                    .system(sistema)
                    .user(usuario)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw TradutorErrosIa.traduzir(e, VARIAVEL_CHAVE, modelo);
        }
        if (!StringUtils.hasText(resposta)) {
            throw new IaException(TipoErroIa.RESPOSTA_VAZIA,
                    "A IA respondeu vazio (pode ter sido bloqueio de segurança). Tente de novo.", null);
        }
        return resposta;
    }

    /** Tira cercas de markdown, rótulos e aspas, e junta tudo em um parágrafo. */
    static String limpar(String texto) {
        String t = texto.trim()
                .replaceAll("(?s)^```[a-zA-Z]*\\s*", "")
                .replaceAll("(?s)\\s*```$", "")
                .replaceAll("[*#]+", "")
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("(?iu)^(prompt( final| de imagem)?|descri[cç][aã]o)\\s*:\\s*", "")
                .trim();
        while (t.length() > 1 && "\"'“”«»".indexOf(t.charAt(0)) >= 0 && "\"'“”«»".indexOf(t.charAt(t.length() - 1)) >= 0) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    private static String limitar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    public String getModelo() {
        return modelo;
    }

    public boolean isConfigurado() {
        return configurado;
    }
}
