package com.notisblokk.audiencias.model.enums;

/**
 * Enum que define o status de oitiva (depoimento) de um participante em uma audiência.
 *
 * Controla se o participante está aguardando ser ouvido, houve desistência da oitiva,
 * ou se já foi ouvido em audiência anterior.
 */
public enum StatusOitiva {

    AGUARDANDO("Aguardando"),
    DESISTENCIA("Desistência"),
    JA_OUVIDA("Já Ouvida");

    private final String descricao;

    StatusOitiva(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o enum a partir da descrição.
     *
     * @param descricao A descrição do status de oitiva
     * @return O enum correspondente, ou null se não encontrado
     */
    public static StatusOitiva fromDescricao(String descricao) {
        for (StatusOitiva status : values()) {
            if (status.getDescricao().equalsIgnoreCase(descricao)) {
                return status;
            }
        }
        return null;
    }
}
