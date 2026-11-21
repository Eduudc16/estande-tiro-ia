package com.faculdade.estandetiro.model.service;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.service.ia.FiltroTermosProibidos;
import com.faculdade.estandetiro.model.service.ia.IaException;
import com.faculdade.estandetiro.model.service.ia.ImageService;
import com.faculdade.estandetiro.model.service.ia.ImagemGerada;
import com.faculdade.estandetiro.model.service.ia.TradutorErrosIa;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Funcionalidade 3: gera a imagem de fundo do estande (chave faturada) com controle de custo:
 * cache (não gera de novo se já existe imagem, só pelo "Regenerar"), limite por execução,
 * uma geração por vez para cada estande e log de toda chamada feita à API.
 */
@Service
public class CenarioService {

    public record Resultado(String caminhoImagem, List<String> termosRemovidos) {
    }

    private final EstandeTiroService estandeService;
    private final ImageService imageService;
    private final LogGeracaoImagemService logService;
    private final LimiteGeracoesImagem limite;
    private final ArmazenamentoCenarios armazenamento;
    private final Set<Long> emAndamento = ConcurrentHashMap.newKeySet();

    public CenarioService(EstandeTiroService estandeService, ImageService imageService,
                          LogGeracaoImagemService logService, LimiteGeracoesImagem limite,
                          ArmazenamentoCenarios armazenamento) {
        this.estandeService = estandeService;
        this.imageService = imageService;
        this.logService = logService;
        this.limite = limite;
        this.armazenamento = armazenamento;
    }

    /**
     * @param regenerar true só quando o usuário confirmou o botão "Regenerar"
     */
    public Resultado gerar(Long estandeId, boolean regenerar) {
        EstandeTiro estande = estandeService.buscarPorId(estandeId);
        if (estande.temImagem() && !regenerar) {
            throw new RegraNegocioException("Este estande já tem cenário (cache): nada foi gerado. "
                    + "Para criar outro, use o botão \"Regenerar\".");
        }
        if (!estande.temPromptImagem()) {
            throw new RegraNegocioException("Gere (ou escreva) o prompt de imagem antes de gerar o cenário.");
        }
        if (!imageService.isConfigurado()) {
            throw IaException.chaveNaoConfigurada(ImageService.VARIAVEL_CHAVE);
        }
        // Nunca manda nome de jogo/personagem para a geração de imagem: só a descrição visual.
        FiltroTermosProibidos.Resultado filtrado = FiltroTermosProibidos.limpar(estande.getPromptImagem());

        if (!emAndamento.add(estandeId)) {
            throw new RegraNegocioException("Já existe uma geração em andamento para este estande. Aguarde.");
        }
        try {
            if (!limite.tentarReservar()) {
                throw new RegraNegocioException("Limite de " + limite.getMaximo() + " gerações de imagem nesta "
                        + "execução atingido. Para liberar mais, aumente MAX_IMAGE_GENERATIONS no .env e reinicie o sistema.");
            }
            return chamarApi(estande, filtrado);
        } finally {
            emAndamento.remove(estandeId);
        }
    }

    private Resultado chamarApi(EstandeTiro estande, FiltroTermosProibidos.Resultado prompt) {
        String modelo = imageService.getModelo();
        ImagemGerada imagem;
        try {
            imagem = imageService.gerar(prompt.texto());
        } catch (RuntimeException e) {
            limite.liberar(); // nenhuma imagem veio: a vaga volta
            IaException erro = TradutorErrosIa.traduzir(e, ImageService.VARIAVEL_CHAVE, modelo);
            logService.registrarFalha(estande, modelo, erro.getMensagemParaLog());
            throw erro;
        }
        try {
            String caminho = armazenamento.salvar(estande.getId(), imagem);
            estandeService.registrarImagem(estande.getId(), caminho);
            logService.registrarSucesso(estande, modelo);
            return new Resultado(caminho, prompt.removidos());
        } catch (RuntimeException e) {
            // a imagem foi cobrada, então a vaga continua consumida
            logService.registrarFalha(estande, modelo, "Imagem gerada, mas não salva: " + e.getMessage());
            throw e;
        }
    }

    public boolean emAndamento(Long estandeId) {
        return emAndamento.contains(estandeId);
    }
}
