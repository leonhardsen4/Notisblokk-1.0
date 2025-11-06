package com.notisblokk.audiencias.model.enums;

/**
 * Enum que define os tipos de audiência judicial.
 *
 * Baseado no sistema TJSP Audiências.
 */
public enum TipoAudiencia {

    INSTRUCAO_DEBATES_JULGAMENTO("Instrução, Debates e Julgamento"),
    APRESENTACAO("Apresentação"),
    JUSTIFICACAO("Justificação"),
    SUSPENSAO_CONDICIONAL_PROCESSO("Suspensão Condicional do Processo"),
    ACORDO_NAO_PERSECUCAO_PENAL("Acordo de Não Persecução Penal"),
    JURI("Júri"),
    OUTROS("Outros");

    private final String descricao;

    TipoAudiencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o enum a partir da descrição.
     *
     * @param descricao A descrição do tipo de audiência
     * @return O enum correspondente, ou null se não encontrado
     */
    public static TipoAudiencia fromDescricao(String descricao) {
        for (TipoAudiencia tipo : values()) {
            if (tipo.getDescricao().equalsIgnoreCase(descricao)) {
                return tipo;
            }
        }
        return null;
    }
}
