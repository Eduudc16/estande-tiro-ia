package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.entity.Arma;
import com.faculdade.estandetiro.model.entity.TipoArma;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados do formulário de cadastro/edição de arma. */
public class ArmaForm {

    @NotBlank(message = "Informe o nome da arma.")
    @Size(max = 60, message = "O nome pode ter no máximo 60 caracteres.")
    private String nome;

    @NotNull(message = "Escolha o tipo da arma.")
    private TipoArma tipo;

    @NotNull(message = "Informe o dano.")
    @Min(value = 1, message = "O dano vai de 1 a 10.")
    @Max(value = 10, message = "O dano vai de 1 a 10.")
    private Integer dano = 1;

    @NotNull(message = "Informe o tamanho do pente.")
    @Min(value = 1, message = "O pente vai de 1 a 100 balas.")
    @Max(value = 100, message = "O pente vai de 1 a 100 balas.")
    private Integer capacidade = 10;

    @NotNull(message = "Informe a cadência.")
    @Min(value = 50, message = "A cadência vai de 50 a 3000 ms.")
    @Max(value = 3000, message = "A cadência vai de 50 a 3000 ms.")
    private Integer cadenciaMs = 250;

    @NotNull(message = "Informe o tempo de recarga.")
    @Min(value = 300, message = "A recarga vai de 300 a 6000 ms.")
    @Max(value = 6000, message = "A recarga vai de 300 a 6000 ms.")
    private Integer recargaMs = 1200;

    @Size(max = 300, message = "O diferencial pode ter no máximo 300 caracteres.")
    private String diferencial;

    @NotNull(message = "Informe a ordem.")
    @Min(value = 0, message = "A ordem vai de 0 a 99.")
    @Max(value = 99, message = "A ordem vai de 0 a 99.")
    private Integer ordem = 9;

    public static ArmaForm de(Arma arma) {
        ArmaForm f = new ArmaForm();
        f.nome = arma.getNome();
        f.tipo = arma.getTipo();
        f.dano = arma.getDano();
        f.capacidade = arma.getCapacidade();
        f.cadenciaMs = arma.getCadenciaMs();
        f.recargaMs = arma.getRecargaMs();
        f.diferencial = arma.getDiferencial();
        f.ordem = arma.getOrdem();
        return f;
    }

    public Arma paraEntidade() {
        return new Arma(nome, tipo, dano, capacidade, cadenciaMs, recargaMs, diferencial, ordem);
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

    public Integer getDano() {
        return dano;
    }

    public void setDano(Integer dano) {
        this.dano = dano;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }

    public Integer getCadenciaMs() {
        return cadenciaMs;
    }

    public void setCadenciaMs(Integer cadenciaMs) {
        this.cadenciaMs = cadenciaMs;
    }

    public Integer getRecargaMs() {
        return recargaMs;
    }

    public void setRecargaMs(Integer recargaMs) {
        this.recargaMs = recargaMs;
    }

    public String getDiferencial() {
        return diferencial;
    }

    public void setDiferencial(String diferencial) {
        this.diferencial = diferencial;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }
}
