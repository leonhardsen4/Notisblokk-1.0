package com.notisblokk.model;

/**
 * Enumeração que define os estados possíveis de uma sessão de usuário.
 *
 * <p>Uma sessão pode estar em um dos seguintes estados:</p>
 * <ul>
 *   <li><b>ACTIVE:</b> Sessão ativa, usuário está logado</li>
 *   <li><b>LOGGED_OUT:</b> Sessão encerrada por logout do usuário</li>
 *   <li><b>EXPIRED:</b> Sessão expirada por inatividade ou timeout</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public enum SessionStatus {

    /**
     * Sessão ativa - usuário está logado no sistema.
     */
    ACTIVE("Ativa"),

    /**
     * Sessão encerrada - usuário fez logout.
     */
    LOGGED_OUT("Encerrada"),

    /**
     * Sessão expirada - timeout ou inatividade.
     */
    EXPIRED("Expirada");

    private final String displayName;

    /**
     * Construtor do enum.
     *
     * @param displayName nome amigável para exibição
     */
    SessionStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Retorna o nome amigável do status.
     *
     * @return String nome para exibição
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converte uma string para o enum correspondente.
     *
     * @param value valor em string (ACTIVE, LOGGED_OUT ou EXPIRED)
     * @return SessionStatus enum correspondente
     * @throws IllegalArgumentException se o valor não for válido
     */
    public static SessionStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Status não pode ser nulo");
        }

        try {
            return SessionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Status inválido: " + value + ". Use ACTIVE, LOGGED_OUT ou EXPIRED."
            );
        }
    }

    /**
     * Verifica se a sessão está ativa.
     *
     * @return boolean true se status for ACTIVE, false caso contrário
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Verifica se a sessão foi encerrada (logout ou expirada).
     *
     * @return boolean true se status for LOGGED_OUT ou EXPIRED, false caso contrário
     */
    public boolean isTerminated() {
        return this == LOGGED_OUT || this == EXPIRED;
    }
}