package com.faculdade.estandetiro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Um estande de tiro (minigame). A imagem de fundo e fixa (o cenario);
 * os alvos que aparecem e somem por cima sao a parte dinamica do jogo.
 */
@Entity
@Table(name = "estande_tiro")
public class EstandeTiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEstande tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estilo_visual", nullable = false, length = 20)
    private EstiloVisual estiloVisual;

    @Column(name = "descricao_gerada_por_ia", length = 2000)
    private String descricaoGeradaPorIA;

    @Column(name = "prompt_imagem", length = 4000)
    private String promptImagem;

    /** URL da imagem salva, ex.: /assets/cenarios/3.png */
    @Column(name = "caminho_imagem")
    private String caminhoImagem;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public EstandeTiro() {
    }

    public EstandeTiro(String nome, TipoEstande tipo, EstiloVisual estiloVisual) {
        this.nome = nome;
        this.tipo = tipo;
        this.estiloVisual = estiloVisual;
    }

    @PrePersist
    void antesDeSalvar() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    public boolean temDescricao() {
        return descricaoGeradaPorIA != null && !descricaoGeradaPorIA.isBlank();
    }

    public boolean temPromptImagem() {
        return promptImagem != null && !promptImagem.isBlank();
    }

    public boolean temImagem() {
        return caminhoImagem != null && !caminhoImagem.isBlank();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoEstande getTipo() {
        return tipo;
    }

    public void setTipo(TipoEstande tipo) {
        this.tipo = tipo;
    }

    public EstiloVisual getEstiloVisual() {
        return estiloVisual;
    }

    public void setEstiloVisual(EstiloVisual estiloVisual) {
        this.estiloVisual = estiloVisual;
    }

    public String getDescricaoGeradaPorIA() {
        return descricaoGeradaPorIA;
    }

    public void setDescricaoGeradaPorIA(String descricaoGeradaPorIA) {
        this.descricaoGeradaPorIA = descricaoGeradaPorIA;
    }

    public String getPromptImagem() {
        return promptImagem;
    }

    public void setPromptImagem(String promptImagem) {
        this.promptImagem = promptImagem;
    }

    public String getCaminhoImagem() {
        return caminhoImagem;
    }

    public void setCaminhoImagem(String caminhoImagem) {
        this.caminhoImagem = caminhoImagem;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
