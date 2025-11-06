package com.notisblokk.audiencias.model.enums;

/**
 * Enum que define a competência da vara judicial.
 *
 * Baseado no sistema TJSP Audiências.
 */
public enum Competencia {

    CRIMINAL("Criminal"),
    VIOLENCIA_DOMESTICA("Violência Doméstica"),
    INFANCIA_JUVENTUDE("Infância e Juventude");

    private final String descricao;

    Competencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o enum a partir da descrição.
     *
     * @param descricao A descrição da competência
     * @return O enum correspondente, ou null se não encontrado
     */
    public static Competencia fromDescricao(String descricao) {
        for (Competencia competencia : values()) {
            if (competencia.getDescricao().equalsIgnoreCase(descricao)) {
                return competencia;
            }
        }
        return null;
    }
}
