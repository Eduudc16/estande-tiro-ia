package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados do formulário de cadastro/edição (só os campos que o usuário pode alterar). */
public class EstandeForm {

    @NotBlank(message = "Informe o nome do estande.")
    @Size(max = 80, message = "O nome pode ter no máximo 80 caracteres.")
    private String nome;

    @NotNull(message = "Escolha o tipo do estande.")
    private TipoEstande tipo;

    @NotNull(message = "Escolha o estilo visual.")
    private EstiloVisual estiloVisual = EstiloVisual.REALISTA;

    @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres.")
    private String descricaoGeradaPorIA;

    @Size(max = 4000, message = "O prompt pode ter no máximo 4000 caracteres.")
    private String promptImagem;

    public static EstandeForm de(EstandeTiro estande) {
        EstandeForm form = new EstandeForm();
        form.nome = estande.getNome();
        form.tipo = estande.getTipo();
        form.estiloVisual = estande.getEstiloVisual();
        form.descricaoGeradaPorIA = estande.getDescricaoGeradaPorIA();
        form.promptImagem = estande.getPromptImagem();
        return form;
    }

    public EstandeTiro paraEntidade() {
        EstandeTiro estande = new EstandeTiro(nome, tipo, estiloVisual);
        estande.setDescricaoGeradaPorIA(descricaoGeradaPorIA);
        estande.setPromptImagem(promptImagem);
        return estande;
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
}
