package com.faculdade.estandetiro.model.service;

import com.faculdade.estandetiro.model.entity.Arma;
import com.faculdade.estandetiro.model.repository.ArmaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD das armas do minigame. */
@Service
public class ArmaService {

    private final ArmaRepository repository;

    public ArmaService(ArmaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Arma> listarTodas() {
        return repository.findAllByOrderByOrdemAscIdAsc();
    }

    @Transactional(readOnly = true)
    public Arma buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arma #" + id + " não encontrada."));
    }

    @Transactional(readOnly = true)
    public long contar() {
        return repository.count();
    }

    @Transactional
    public Arma criar(Arma dados) {
        Arma nova = new Arma();
        copiarCampos(dados, nova);
        return repository.save(nova);
    }

    @Transactional
    public Arma atualizar(Long id, Arma dados) {
        Arma existente = buscarPorId(id);
        copiarCampos(dados, existente);
        return repository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscarPorId(id));
    }

    private static void copiarCampos(Arma origem, Arma destino) {
        destino.setNome(origem.getNome() == null ? null : origem.getNome().trim());
        destino.setTipo(origem.getTipo());
        destino.setDano(origem.getDano());
        destino.setCapacidade(origem.getCapacidade());
        destino.setCadenciaMs(origem.getCadenciaMs());
        destino.setRecargaMs(origem.getRecargaMs());
        String dif = origem.getDiferencial();
        destino.setDiferencial(dif == null || dif.isBlank() ? null : dif.trim());
        destino.setOrdem(origem.getOrdem());
    }
}
