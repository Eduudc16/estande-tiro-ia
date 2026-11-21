package com.faculdade.estandetiro.model.service;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.repository.EstandeTiroRepository;
import com.faculdade.estandetiro.model.repository.LogGeracaoImagemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD dos estandes de tiro. */
@Service
public class EstandeTiroService {

    private final EstandeTiroRepository repository;
    private final LogGeracaoImagemRepository logRepository;
    private final ArmazenamentoCenarios armazenamento;

    public EstandeTiroService(EstandeTiroRepository repository, LogGeracaoImagemRepository logRepository,
                              ArmazenamentoCenarios armazenamento) {
        this.repository = repository;
        this.logRepository = logRepository;
        this.armazenamento = armazenamento;
    }

    @Transactional(readOnly = true)
    public List<EstandeTiro> listarTodos() {
        return repository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public EstandeTiro buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estande de tiro #" + id + " não encontrado."));
    }

    @Transactional(readOnly = true)
    public long contar() {
        return repository.count();
    }

    @Transactional
    public EstandeTiro criar(EstandeTiro dados) {
        EstandeTiro novo = new EstandeTiro();
        copiarCampos(dados, novo);
        return repository.save(novo);
    }

    @Transactional
    public EstandeTiro atualizar(Long id, EstandeTiro dados) {
        EstandeTiro existente = buscarPorId(id);
        copiarCampos(dados, existente);
        return repository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        EstandeTiro estande = buscarPorId(id);
        logRepository.desvincularEstande(id); // o log de gastos continua existindo
        repository.delete(estande);
        armazenamento.excluir(id);
    }

    @Transactional
    public EstandeTiro salvarDescricao(Long id, String descricao) {
        EstandeTiro estande = buscarPorId(id);
        estande.setDescricaoGeradaPorIA(descricao);
        return repository.save(estande);
    }

    @Transactional
    public EstandeTiro salvarPromptImagem(Long id, String prompt) {
        EstandeTiro estande = buscarPorId(id);
        estande.setPromptImagem(prompt);
        return repository.save(estande);
    }

    @Transactional
    public EstandeTiro registrarImagem(Long id, String caminhoImagem) {
        EstandeTiro estande = buscarPorId(id);
        estande.setCaminhoImagem(caminhoImagem);
        return repository.save(estande);
    }

    /** Só os campos editáveis pelo usuário (caminho da imagem e data de criação ficam com o sistema). */
    private static void copiarCampos(EstandeTiro origem, EstandeTiro destino) {
        destino.setNome(origem.getNome() == null ? null : origem.getNome().trim());
        destino.setTipo(origem.getTipo());
        destino.setEstiloVisual(origem.getEstiloVisual());
        destino.setDescricaoGeradaPorIA(vazioParaNulo(origem.getDescricaoGeradaPorIA()));
        destino.setPromptImagem(vazioParaNulo(origem.getPromptImagem()));
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
