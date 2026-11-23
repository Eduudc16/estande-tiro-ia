package com.faculdade.estandetiro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Arma disponível no minigame. Cada uma tem os seus números e o diferencial do seu tipo. */
@Entity
@Table(name = "arma")
public class Arma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoArma tipo;

    /** Dano por tiro (inimigo comum aguenta 1; o forte, 4). */
    @Column(nullable = false)
    private int dano;

    /** Balas no pente. */
    @Column(nullable = false)
    private int capacidade;

    /** Intervalo mínimo entre dois tiros, em milissegundos. */
    @Column(name = "cadencia_ms", nullable = false)
    private int cadenciaMs;

    /** Tempo de recarga, em milissegundos. */
    @Column(name = "recarga_ms", nullable = false)
    private int recargaMs;

    @Column(length = 300)
    private String diferencial;

    /** Posição na barra de armas (tecla 1, 2, 3...). */
    @Column(nullable = false)
    private int ordem;

    public Arma() {
    }

    public Arma(String nome, TipoArma tipo, int dano, int capacidade, int cadenciaMs, int recargaMs,
                String diferencial, int ordem) {
        this.nome = nome;
        this.tipo = tipo;
        this.dano = dano;
        this.capacidade = capacidade;
        this.cadenciaMs = cadenciaMs;
        this.recargaMs = recargaMs;
        this.diferencial = diferencial;
        this.ordem = ordem;
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

    public TipoArma getTipo() {
        return tipo;
    }

    public void setTipo(TipoArma tipo) {
        this.tipo = tipo;
    }

    public int getDano() {
        return dano;
    }

    public void setDano(int dano) {
        this.dano = dano;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(int capacidade) {
        this.capacidade = capacidade;
    }

    public int getCadenciaMs() {
        return cadenciaMs;
    }

    public void setCadenciaMs(int cadenciaMs) {
        this.cadenciaMs = cadenciaMs;
    }

    public int getRecargaMs() {
        return recargaMs;
    }

    public void setRecargaMs(int recargaMs) {
        this.recargaMs = recargaMs;
    }

    public String getDiferencial() {
        return diferencial;
    }

    public void setDiferencial(String diferencial) {
        this.diferencial = diferencial;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }
}
