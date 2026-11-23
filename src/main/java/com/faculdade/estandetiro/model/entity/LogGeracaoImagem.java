package com.faculdade.estandetiro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de cada chamada feita a API de imagem (chave faturada), com sucesso ou erro.
 * Serve para acompanhar o gasto. Se o estande for excluido, o log continua (estandeTiro fica nulo).
 */
@Entity
@Table(name = "log_geracao_imagem")
public class LogGeracaoImagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estande_tiro_id")
    private EstandeTiro estandeTiro;

    /** Preenchido quando a imagem gerada foi de um alvo (inimigo) e não do cenário de um estande. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alvo_inimigo_id")
    private AlvoInimigo alvoInimigo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "modelo_usado", nullable = false, length = 80)
    private String modeloUsado;

    @Column(nullable = false)
    private boolean sucesso;

    @Column(name = "mensagem_erro", length = 1000)
    private String mensagemErro;

    public LogGeracaoImagem() {
    }

    public static LogGeracaoImagem sucesso(EstandeTiro estande, String modelo) {
        return novo(estande, modelo, true, null);
    }

    public static LogGeracaoImagem falha(EstandeTiro estande, String modelo, String mensagemErro) {
        String msg = mensagemErro == null ? "Erro desconhecido" : mensagemErro;
        return novo(estande, modelo, false, msg.length() > 1000 ? msg.substring(0, 1000) : msg);
    }

    public static LogGeracaoImagem sucessoAlvo(AlvoInimigo alvo, String modelo) {
        LogGeracaoImagem log = novo(null, modelo, true, null);
        log.alvoInimigo = alvo;
        return log;
    }

    public static LogGeracaoImagem falhaAlvo(AlvoInimigo alvo, String modelo, String mensagemErro) {
        LogGeracaoImagem log = falha(null, modelo, mensagemErro);
        log.alvoInimigo = alvo;
        return log;
    }

    private static LogGeracaoImagem novo(EstandeTiro estande, String modelo, boolean sucesso, String erro) {
        LogGeracaoImagem log = new LogGeracaoImagem();
        log.estandeTiro = estande;
        log.dataHora = LocalDateTime.now();
        log.modeloUsado = modelo;
        log.sucesso = sucesso;
        log.mensagemErro = erro;
        return log;
    }

    public Long getId() {
        return id;
    }

    public EstandeTiro getEstandeTiro() {
        return estandeTiro;
    }

    public void setEstandeTiro(EstandeTiro estandeTiro) {
        this.estandeTiro = estandeTiro;
    }

    public AlvoInimigo getAlvoInimigo() {
        return alvoInimigo;
    }

    public void setAlvoInimigo(AlvoInimigo alvoInimigo) {
        this.alvoInimigo = alvoInimigo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getModeloUsado() {
        return modeloUsado;
    }

    public void setModeloUsado(String modeloUsado) {
        this.modeloUsado = modeloUsado;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public void setMensagemErro(String mensagemErro) {
        this.mensagemErro = mensagemErro;
    }
}
