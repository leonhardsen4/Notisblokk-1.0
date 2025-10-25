package com.notisblokk.model;

/**
 * Enumeração que define os papéis (roles) de usuários no sistema.
 *
 * <p>O sistema possui dois tipos de usuários:</p>
 * <ul>
 *   <li><b>ADMIN:</b> Administrador com acesso completo ao sistema, incluindo
 *       gerenciamento de usuários e configurações</li>
 *   <li><b>OPERATOR:</b> Operador com acesso limitado às funcionalidades
 *       básicas do sistema</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public enum UserRole {

    /**
     * Administrador do sistema com acesso total.
     */
    ADMIN("Administrador"),

    /**
     * Operador com acesso limitado.
     */
    OPERATOR("Operador");

    private final String displayName;

    /**
     * Construtor do enum.
     *
     * @param displayName nome amigável para exibição
     */
    UserRole(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Retorna o nome amigável do papel.
     *
     * @return String nome para exibição
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converte uma string para o enum correspondente.
     *
     * @param value valor em string (ADMIN ou OPERATOR)
     * @return UserRole enum correspondente
     * @throws IllegalArgumentException se o valor não for válido
     */
    public static UserRole fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role não pode ser nulo");
        }

        try {
            return UserRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role inválido: " + value + ". Use ADMIN ou OPERATOR.");
        }
    }

    /**
     * Verifica se o papel é de administrador.
     *
     * @return boolean true se for ADMIN, false caso contrário
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * Verifica se o papel é de operador.
     *
     * @return boolean true se for OPERATOR, false caso contrário
     */
    public boolean isOperator() {
        return this == OPERATOR;
    }
}