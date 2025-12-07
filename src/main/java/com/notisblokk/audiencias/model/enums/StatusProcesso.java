package com.notisblokk.audiencias.model.enums;

/**
 * Enum que define o status de um processo judicial.
 *
 * Indica o estado atual de tramitação do processo.
 */
public enum StatusProcesso {

    EM_ANDAMENTO("Em Andamento"),
    AGUARDANDO_AUDIENCIA("Aguardando Audiência"),
    SENTENCIADO("Sentenciado"),
    ARQUIVADO("Arquivado"),
    SUSPENSO("Suspenso"),
    EXTINTO("Extinto");

    private final String descricao;

    StatusProcesso(String descricao) {
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
    public static StatusProcesso fromDescricao(String descricao) {
        for (StatusProcesso status : values()) {
            if (status.getDescricao().equalsIgnoreCase(descricao)) {
                return status;
            }
        }
        return null;
    }
}
