package com.faculdade.estandetiro.model.service.ia;

/**
 * Erro ao falar com a IA. A mensagem (getMessage) e amigavel, para a tela;
 * o detalhe traz o codigo/mensagem tecnica devolvida pela API (nunca a chave).
 */
public class IaException extends RuntimeException {

    private final TipoErroIa tipo;
    private final String detalhe;

    public IaException(TipoErroIa tipo, String mensagem, String detalhe) {
        super(mensagem);
        this.tipo = tipo;
        this.detalhe = detalhe;
    }

    public static IaException chaveNaoConfigurada(String nomeVariavel) {
        return new IaException(TipoErroIa.CHAVE_NAO_CONFIGURADA,
                "A chave " + nomeVariavel + " não está configurada. Crie o arquivo .env na raiz do projeto "
                        + "(copie o .env.example), preencha a chave e reinicie o sistema.", null);
    }

    public TipoErroIa getTipo() {
        return tipo;
    }

    public String getDetalhe() {
        return detalhe;
    }

    /** Texto completo para o log de geracoes. */
    public String getMensagemParaLog() {
        return detalhe == null || detalhe.isBlank() ? getMessage() : getMessage() + " [" + detalhe + "]";
    }
}
