package com.notisblokk.audiencias.model.enums;

/**
 * Enum que define o formato de realização da audiência.
 *
 * Baseado no sistema TJSP Audiências.
 */
public enum FormatoAudiencia {

    VIRTUAL("Virtual"),
    PRESENCIAL("Presencial"),
    HIBRIDA("Híbrida");

    private final String descricao;

    FormatoAudiencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o enum a partir da descrição.
     *
     * @param descricao A descrição do formato
     * @return O enum correspondente, ou null se não encontrado
     */
    public static FormatoAudiencia fromDescricao(String descricao) {
        for (FormatoAudiencia formato : values()) {
            if (formato.getDescricao().equalsIgnoreCase(descricao)) {
                return formato;
            }
        }
        return null;
    }
}
