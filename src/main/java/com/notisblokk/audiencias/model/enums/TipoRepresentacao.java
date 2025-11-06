package com.notisblokk.audiencias.model.enums;

/**
 * Enum que define o tipo de representação do advogado.
 *
 * Baseado no sistema TJSP Audiências.
 */
public enum TipoRepresentacao {

    CONSTITUIDO("Constituído"),
    DATIVO("Dativo"),
    AD_HOC("Ad Hoc"),
    DEFESA("Defesa"),
    ASSISTENCIA_ACUSACAO("Assistência de Acusação");

    private final String descricao;

    TipoRepresentacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o enum a partir da descrição.
     *
     * @param descricao A descrição do tipo de representação
     * @return O enum correspondente, ou null se não encontrado
     */
    public static TipoRepresentacao fromDescricao(String descricao) {
        for (TipoRepresentacao tipo : values()) {
            if (tipo.getDescricao().equalsIgnoreCase(descricao)) {
                return tipo;
            }
        }
        return null;
    }
}
