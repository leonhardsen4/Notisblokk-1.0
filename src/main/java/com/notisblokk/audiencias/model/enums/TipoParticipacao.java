package com.notisblokk.audiencias.model.enums;

/**
 * Enum que define o tipo de participação de uma pessoa na audiência.
 *
 * Baseado no sistema TJSP Audiências.
 */
public enum TipoParticipacao {

    AUTOR("Autor"),
    REU("Réu"),
    VITIMA("Vítima"),
    VITIMA_FATAL("Vítima Fatal"),
    REPRESENTANTE_LEGAL("Representante Legal"),
    TESTEMUNHA_COMUM("Testemunha Comum"),
    TESTEMUNHA_ACUSACAO("Testemunha de Acusação"),
    TESTEMUNHA_DEFESA("Testemunha de Defesa"),
    ASSISTENTE_ACUSACAO("Assistente de Acusação"),
    PERITO("Perito"),
    TERCEIRO("Terceiro"),
    OUTROS("Outros");

    private final String descricao;

    TipoParticipacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o enum a partir da descrição.
     *
     * @param descricao A descrição do tipo de participação
     * @return O enum correspondente, ou null se não encontrado
     */
    public static TipoParticipacao fromDescricao(String descricao) {
        for (TipoParticipacao tipo : values()) {
            if (tipo.getDescricao().equalsIgnoreCase(descricao)) {
                return tipo;
            }
        }
        return null;
    }
}
