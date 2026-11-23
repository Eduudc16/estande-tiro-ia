package com.faculdade.estandetiro.model.service;

import com.faculdade.estandetiro.model.entity.AlvoInimigo;
import com.faculdade.estandetiro.model.entity.ClasseAlvo;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.repository.AlvoInimigoRepository;
import com.faculdade.estandetiro.model.service.ia.IaException;
import com.faculdade.estandetiro.model.service.ia.ImageService;
import com.faculdade.estandetiro.model.service.ia.ImagemGerada;
import com.faculdade.estandetiro.model.service.ia.PromptAlvo;
import com.faculdade.estandetiro.model.service.ia.RecorteAlvo;
import com.faculdade.estandetiro.model.service.ia.TradutorErrosIa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inimigos de cada tipo de estande e a geração da imagem deles pela IA (chave faturada), com o mesmo controle de
 * custo dos cenários: cache (só refaz pelo "Regenerar"), limite por execução, uma geração por vez e log de tudo.
 */
@Service
public class AlvoInimigoService {

    private final AlvoInimigoRepository repository;
    private final ImageService imageService;
    private final LogGeracaoImagemService logService;
    private final LimiteGeracoesImagem limite;
    private final ArmazenamentoAlvos armazenamento;
    private final Set<Long> emAndamento = ConcurrentHashMap.newKeySet();

    public AlvoInimigoService(AlvoInimigoRepository repository, ImageService imageService,
                              LogGeracaoImagemService logService, LimiteGeracoesImagem limite,
                              ArmazenamentoAlvos armazenamento) {
        this.repository = repository;
        this.imageService = imageService;
        this.logService = logService;
        this.limite = limite;
        this.armazenamento = armazenamento;
    }

    @Transactional(readOnly = true)
    public List<AlvoInimigo> listarTodos() {
        return repository.findAllByOrderByTipoEstandeAscIndiceDesenhoAsc();
    }

    @Transactional(readOnly = true)
    public List<AlvoInimigo> listarPorTipo(TipoEstande tipo) {
        return repository.findByTipoEstandeOrderByIndiceDesenhoAsc(tipo);
    }

    @Transactional(readOnly = true)
    public AlvoInimigo buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alvo #" + id + " não encontrado."));
    }

    @Transactional(readOnly = true)
    public long contar() {
        return repository.count();
    }

    @Transactional(readOnly = true)
    public long contarRefens() {
        return repository.countByClasse(ClasseAlvo.REFEM);
    }

    @Transactional
    public AlvoInimigo criar(AlvoInimigo alvo) {
        return repository.save(alvo);
    }

    /**
     * Gera a imagem do inimigo com a IA e recorta o personagem do fundo.
     *
     * @param regenerar true só quando o usuário confirmou o "Regenerar"
     */
    public AlvoInimigo gerarImagem(Long id, boolean regenerar) {
        AlvoInimigo alvo = buscarPorId(id);
        if (alvo.temImagem() && !regenerar) {
            throw new RegraNegocioException("Este alvo já tem imagem (cache): nada foi gerado. "
                    + "Para criar outra, use o botão \"Regenerar\".");
        }
        if (!imageService.isConfigurado()) {
            throw IaException.chaveNaoConfigurada(ImageService.VARIAVEL_CHAVE);
        }
        String prompt = PromptAlvo.montar(alvo);
        if (!emAndamento.add(id)) {
            throw new RegraNegocioException("Já existe uma geração em andamento para este alvo. Aguarde.");
        }
        try {
            if (!limite.tentarReservar()) {
                throw new RegraNegocioException("Limite de " + limite.getMaximo() + " gerações de imagem nesta "
                        + "execução atingido. Para liberar mais, aumente MAX_IMAGE_GENERATIONS no .env e reinicie o sistema.");
            }
            String modelo = imageService.getModelo();
            ImagemGerada imagem;
            try {
                imagem = imageService.gerar(prompt, PromptAlvo.PROPORCAO);
            } catch (RuntimeException e) {
                limite.liberar(); // nenhuma imagem veio: a vaga volta
                IaException erro = TradutorErrosIa.traduzir(e, ImageService.VARIAVEL_CHAVE, modelo);
                logService.registrarFalhaAlvo(alvo, modelo, erro.getMensagemParaLog());
                throw erro;
            }
            // a imagem foi cobrada: guarda a original antes de qualquer coisa
            try {
                armazenamento.salvarOriginal(id, imagem);
            } catch (IOException e) {
                // sem a cópia original só não dá para refazer o recorte depois
            }
            try {
                AlvoInimigo salvo = aplicarRecorte(alvo, imagem.bytes());
                logService.registrarSucessoAlvo(salvo, modelo);
                return salvo;
            } catch (IOException | RuntimeException e) {
                logService.registrarFalhaAlvo(alvo, modelo, "Imagem gerada, mas o recorte falhou: " + e.getMessage());
                throw new RegraNegocioException("A imagem foi gerada, mas não foi possível recortar o personagem ("
                        + e.getMessage() + "). Use \"Refazer recorte\" ou \"Regenerar\".");
            }
        } finally {
            emAndamento.remove(id);
        }
    }

    /** Refaz o recorte a partir da imagem original já paga, sem chamar a API (grátis). */
    public AlvoInimigo refazerRecorte(Long id) {
        AlvoInimigo alvo = buscarPorId(id);
        byte[] original = armazenamento.lerOriginal(id).orElseThrow(() -> new RegraNegocioException(
                "Não há imagem original guardada para este alvo. Gere a imagem primeiro."));
        try {
            return aplicarRecorte(alvo, original);
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível recortar o personagem: " + e.getMessage());
        }
    }

    public boolean temOriginal(Long id) {
        return armazenamento.lerOriginal(id).isPresent();
    }

    private AlvoInimigo aplicarRecorte(AlvoInimigo alvo, byte[] imagem) throws IOException {
        byte[] png = RecorteAlvo.processar(imagem);
        alvo.setCaminhoImagem(armazenamento.salvar(alvo.getId(), png));
        alvo.setDataImagem(LocalDateTime.now());
        return repository.save(alvo);
    }
}
