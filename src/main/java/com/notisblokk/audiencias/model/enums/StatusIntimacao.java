package com.notisblokk.audiencias.model.enums;

/**
 * Enum que define o status de intimação de um participante em uma audiência.
 *
 * Controla se o participante já foi intimado, está aguardando ou ainda não foi intimado.
 */
public enum StatusIntimacao {

    INTIMADA("Intimada"),
    AGUARDANDO_INTIMACAO("Aguardando Intimação"),
    NAO_INTIMADA("Não Intimada");

    private final String descricao;

    StatusIntimacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o enum a partir da descrição.
     *
     * @param descricao A descrição do status de intimação
     * @return O enum correspondente, ou null se não encontrado
     */
    public static StatusIntimacao fromDescricao(String descricao) {
        for (StatusIntimacao status : values()) {
            if (status.getDescricao().equalsIgnoreCase(descricao)) {
                return status;
            }
        }
        return null;
    }
}
