package com.faculdade.estandetiro.model.service;

/** Erro de regra do sistema, com mensagem pronta para mostrar na tela. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
