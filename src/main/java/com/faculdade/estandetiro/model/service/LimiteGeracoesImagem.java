package com.faculdade.estandetiro.model.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite de imagens geradas por execução do sistema (MAX_IMAGE_GENERATIONS no .env).
 * A vaga é reservada antes de chamar a API e devolvida se nenhuma imagem vier, então
 * nunca passam do limite nem chamadas simultâneas. Reiniciar o sistema zera o contador.
 */
@Component
public class LimiteGeracoesImagem {

    static final int PADRAO = 5;
    private static final Logger log = LoggerFactory.getLogger(LimiteGeracoesImagem.class);

    private final int maximo;
    private final AtomicInteger usadas = new AtomicInteger();

    public LimiteGeracoesImagem(@Value("${app.imagem.max-geracoes:5}") String valor) {
        this.maximo = interpretar(valor);
        log.info("Limite de gerações de imagem nesta execução: {}", maximo);
    }

    static int interpretar(String valor) {
        if (valor == null || valor.isBlank()) {
            return PADRAO;
        }
        try {
            return Math.max(0, Integer.parseInt(valor.trim()));
        } catch (NumberFormatException e) {
            log.warn("MAX_IMAGE_GENERATIONS inválido ('{}'); usando {}.", valor, PADRAO);
            return PADRAO;
        }
    }

    /** @return true se conseguiu uma vaga (a chamada pode ser feita). */
    public boolean tentarReservar() {
        while (true) {
            int atual = usadas.get();
            if (atual >= maximo) {
                return false;
            }
            if (usadas.compareAndSet(atual, atual + 1)) {
                return true;
            }
        }
    }

    /** Devolve a vaga quando a API não entregou imagem (não houve geração). */
    public void liberar() {
        usadas.updateAndGet(v -> Math.max(0, v - 1));
    }

    public int getUsadas() {
        return usadas.get();
    }

    public int getMaximo() {
        return maximo;
    }

    public int getRestantes() {
        return Math.max(0, maximo - usadas.get());
    }
}
