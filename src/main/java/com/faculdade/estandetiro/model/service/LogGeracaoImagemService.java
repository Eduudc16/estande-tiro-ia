package com.faculdade.estandetiro.model.service;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.LogGeracaoImagem;
import com.faculdade.estandetiro.model.repository.LogGeracaoImagemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LogGeracaoImagemService {

    /** Resumo do uso da chave faturada (o gasto é só uma estimativa: sucessos x preço por imagem). */
    public record ResumoGastos(long tentativas, long sucessos, long falhas, BigDecimal precoPorImagemUsd,
                               BigDecimal gastoEstimadoUsd) {
    }

    private final LogGeracaoImagemRepository repository;
    private final BigDecimal precoPorImagemUsd;

    public LogGeracaoImagemService(LogGeracaoImagemRepository repository,
                                   @Value("${gemini.image.preco-usd:0.0336}") BigDecimal precoPorImagemUsd) {
        this.repository = repository;
        this.precoPorImagemUsd = precoPorImagemUsd;
    }

    @Transactional
    public void registrarSucesso(EstandeTiro estande, String modelo) {
        repository.save(LogGeracaoImagem.sucesso(estande, modelo));
    }

    @Transactional
    public void registrarFalha(EstandeTiro estande, String modelo, String mensagemErro) {
        repository.save(LogGeracaoImagem.falha(estande, modelo, mensagemErro));
    }

    @Transactional(readOnly = true)
    public List<LogGeracaoImagem> listarTodos() {
        return repository.listarComEstande();
    }

    @Transactional(readOnly = true)
    public List<LogGeracaoImagem> ultimosDoEstande(Long estandeId) {
        return repository.findTop5ByEstandeTiroIdOrderByDataHoraDescIdDesc(estandeId);
    }

    @Transactional(readOnly = true)
    public ResumoGastos resumo() {
        long sucessos = repository.countBySucesso(true);
        long falhas = repository.countBySucesso(false);
        return new ResumoGastos(sucessos + falhas, sucessos, falhas, precoPorImagemUsd,
                precoPorImagemUsd.multiply(BigDecimal.valueOf(sucessos)));
    }

    public BigDecimal getPrecoPorImagemUsd() {
        return precoPorImagemUsd;
    }
}
