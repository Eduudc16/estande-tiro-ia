package com.faculdade.estandetiro.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Inimigo de um tipo de estande. Enquanto não tem imagem, o jogo usa o desenho SVG de índice
 * {@code indiceDesenho} (static/js/alvos.js); depois de gerada pela IA, usa a imagem recortada.
 */
@Entity
@Table(name = "alvo_inimigo")
public class AlvoInimigo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // texto simples: novos valores no enum não quebram o banco
    @Column(name = "tipo_estande", nullable = false, length = 30)
    private TipoEstande tipoEstande;

    @Column(nullable = false, length = 60)
    private String nome;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // texto simples: novos valores no enum não quebram o banco
    @Column(nullable = false, length = 10)
    private ClasseAlvo classe;

    /** Descrição visual usada no prompt de imagem (sem nomes de jogos ou personagens). */
    @Column(name = "descricao_visual", nullable = false, length = 400)
    private String descricaoVisual;

    @Column(name = "indice_desenho", nullable = false)
    private int indiceDesenho;

    /** URL da imagem recortada, ex.: /assets/alvos/3.png */
    @Column(name = "caminho_imagem")
    private String caminhoImagem;

    @Column(name = "data_imagem")
    private LocalDateTime dataImagem;

    public AlvoInimigo() {
    }

    public AlvoInimigo(TipoEstande tipoEstande, int indiceDesenho, String nome, ClasseAlvo classe,
                       String descricaoVisual) {
        this.tipoEstande = tipoEstande;
        this.indiceDesenho = indiceDesenho;
        this.nome = nome;
        this.classe = classe;
        this.descricaoVisual = descricaoVisual;
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

    public TipoEstande getTipoEstande() {
        return tipoEstande;
    }

    public void setTipoEstande(TipoEstande tipoEstande) {
        this.tipoEstande = tipoEstande;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public ClasseAlvo getClasse() {
        return classe;
    }

    public void setClasse(ClasseAlvo classe) {
        this.classe = classe;
    }

    public String getDescricaoVisual() {
        return descricaoVisual;
    }

    public void setDescricaoVisual(String descricaoVisual) {
        this.descricaoVisual = descricaoVisual;
    }

    public int getIndiceDesenho() {
        return indiceDesenho;
    }

    public void setIndiceDesenho(int indiceDesenho) {
        this.indiceDesenho = indiceDesenho;
    }

    public String getCaminhoImagem() {
        return caminhoImagem;
    }

    public void setCaminhoImagem(String caminhoImagem) {
        this.caminhoImagem = caminhoImagem;
    }

    public LocalDateTime getDataImagem() {
        return dataImagem;
    }

    public void setDataImagem(LocalDateTime dataImagem) {
        this.dataImagem = dataImagem;
    }
}
