package com.notisblokk.util;

import java.util.regex.Pattern;

/**
 * Utilitário para validação de dados de entrada.
 *
 * <p>Fornece métodos para validar:</p>
 * <ul>
 *   <li>Endereços de email</li>
 *   <li>Nomes de usuário (username)</li>
 *   <li>Nomes completos</li>
 *   <li>Strings vazias/nulas</li>
 *   <li>Endereços IP</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class ValidationUtil {

    /**
     * Padrão regex para validação de email (RFC 5322 simplificado).
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Padrão regex para validação de username.
     * Permite: letras, números, underscore, hífen.
     * Deve começar com letra.
     * Tamanho: 3-50 caracteres.
     */
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z][a-zA-Z0-9_-]{2,49}$"
    );

    /**
     * Padrão regex para validação de nome completo.
     * Permite: letras, espaços, acentos, apóstrofos.
     * Tamanho: 2-100 caracteres.
     */
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile(
        "^[a-zA-ZÀ-ÿ\\s'-]{2,100}$"
    );

    /**
     * Padrão regex para validação de endereço IPv4.
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    /**
     * Valida se uma string é nula ou vazia (após trim).
     *
     * @param value string a ser validada
     * @return boolean true se for nula ou vazia, false caso contrário
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Valida se uma string não é nula nem vazia (após trim).
     *
     * @param value string a ser validada
     * @return boolean true se não for nula nem vazia, false caso contrário
     */
    public static boolean isNotEmpty(String value) {
        return !isNullOrEmpty(value);
    }

    /**
     * Valida se um email tem formato válido.
     *
     * @param email email a ser validado
     * @return boolean true se o email for válido, false caso contrário
     */
    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) {
            return false;
        }

        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Valida se um username tem formato válido.
     *
     * <p><b>Critérios:</b></p>
     * <ul>
     *   <li>3 a 50 caracteres</li>
     *   <li>Deve começar com letra</li>
     *   <li>Pode conter: letras, números, underscore (_), hífen (-)</li>
     * </ul>
     *
     * @param username username a ser validado
     * @return boolean true se o username for válido, false caso contrário
     */
    public static boolean isValidUsername(String username) {
        if (isNullOrEmpty(username)) {
            return false;
        }

        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * Valida se um nome completo tem formato válido.
     *
     * <p><b>Critérios:</b></p>
     * <ul>
     *   <li>2 a 100 caracteres</li>
     *   <li>Pode conter: letras, espaços, acentos, apóstrofos, hífens</li>
     * </ul>
     *
     * @param fullName nome completo a ser validado
     * @return boolean true se o nome for válido, false caso contrário
     */
    public static boolean isValidFullName(String fullName) {
        if (isNullOrEmpty(fullName)) {
            return false;
        }

        return FULL_NAME_PATTERN.matcher(fullName.trim()).matches();
    }

    /**
     * Valida se um endereço IP (IPv4) tem formato válido.
     *
     * @param ip endereço IP a ser validado
     * @return boolean true se o IP for válido, false caso contrário
     */
    public static boolean isValidIPv4(String ip) {
        if (isNullOrEmpty(ip)) {
            return false;
        }

        return IPV4_PATTERN.matcher(ip.trim()).matches();
    }

    /**
     * Valida se uma string tem tamanho entre min e max.
     *
     * @param value string a ser validada
     * @param min tamanho mínimo
     * @param max tamanho máximo
     * @return boolean true se o tamanho estiver no intervalo, false caso contrário
     */
    public static boolean hasLengthBetween(String value, int min, int max) {
        if (value == null) {
            return false;
        }

        int length = value.trim().length();
        return length >= min && length <= max;
    }

    /**
     * Valida email e retorna mensagem descritiva.
     *
     * @param email email a ser validado
     * @return ValidationResult resultado da validação com mensagem
     */
    public static ValidationResult validateEmail(String email) {
        if (isNullOrEmpty(email)) {
            return new ValidationResult(false, "Email não pode ser vazio");
        }

        if (!isValidEmail(email)) {
            return new ValidationResult(false, "Formato de email inválido");
        }

        return new ValidationResult(true, "Email válido");
    }

    /**
     * Valida username e retorna mensagem descritiva.
     *
     * @param username username a ser validado
     * @return ValidationResult resultado da validação com mensagem
     */
    public static ValidationResult validateUsername(String username) {
        if (isNullOrEmpty(username)) {
            return new ValidationResult(false, "Username não pode ser vazio");
        }

        String trimmed = username.trim();

        if (trimmed.length() < 3) {
            return new ValidationResult(false, "Username deve ter no mínimo 3 caracteres");
        }

        if (trimmed.length() > 50) {
            return new ValidationResult(false, "Username deve ter no máximo 50 caracteres");
        }

        if (!Character.isLetter(trimmed.charAt(0))) {
            return new ValidationResult(false, "Username deve começar com uma letra");
        }

        if (!isValidUsername(trimmed)) {
            return new ValidationResult(false, "Username pode conter apenas letras, números, underscore e hífen");
        }

        return new ValidationResult(true, "Username válido");
    }

    /**
     * Valida nome completo e retorna mensagem descritiva.
     *
     * @param fullName nome completo a ser validado
     * @return ValidationResult resultado da validação com mensagem
     */
    public static ValidationResult validateFullName(String fullName) {
        if (isNullOrEmpty(fullName)) {
            return new ValidationResult(false, "Nome completo não pode ser vazio");
        }

        String trimmed = fullName.trim();

        if (trimmed.length() < 2) {
            return new ValidationResult(false, "Nome completo deve ter no mínimo 2 caracteres");
        }

        if (trimmed.length() > 100) {
            return new ValidationResult(false, "Nome completo deve ter no máximo 100 caracteres");
        }

        if (!isValidFullName(trimmed)) {
            return new ValidationResult(false, "Nome completo contém caracteres inválidos");
        }

        return new ValidationResult(true, "Nome válido");
    }

    /**
     * Sanitiza uma string removendo espaços extras e trim.
     *
     * @param value string a ser sanitizada
     * @return String string sanitizada ou null se entrada for null
     */
    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }

        // Remove espaços no início/fim e múltiplos espaços internos
        return value.trim().replaceAll("\\s+", " ");
    }

    /**
     * Valida se dois valores são iguais (comparação de senhas, por exemplo).
     *
     * @param value1 primeiro valor
     * @param value2 segundo valor
     * @return boolean true se forem iguais, false caso contrário
     */
    public static boolean areEqual(String value1, String value2) {
        if (value1 == null && value2 == null) {
            return true;
        }

        if (value1 == null || value2 == null) {
            return false;
        }

        return value1.equals(value2);
    }

    /**
     * Classe para representar resultado de validação.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
