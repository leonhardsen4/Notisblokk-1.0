package com.notisblokk.audiencias.model.enums;

/**
 * Enum que define o status atual da audiência.
 *
 * Baseado no sistema TJSP Audiências.
 */
public enum StatusAudiencia {

    DESIGNADA("Designada"),
    REALIZADA("Realizada"),
    PARCIALMENTE_REALIZADA("Parcialmente Realizada"),
    CANCELADA("Cancelada"),
    REDESIGNADA("Redesignada");

    private final String descricao;

    StatusAudiencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o enum a partir da descrição.
     *
     * @param descricao A descrição do status
     * @return O enum correspondente, ou null se não encontrado
     */
    public static StatusAudiencia fromDescricao(String descricao) {
        for (StatusAudiencia status : values()) {
            if (status.getDescricao().equalsIgnoreCase(descricao)) {
                return status;
            }
        }
        return null;
    }
}
