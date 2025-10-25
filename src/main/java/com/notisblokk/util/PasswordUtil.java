package com.notisblokk.util;

import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

/**
 * Utilitário para manipulação e validação de senhas.
 *
 * <p>Fornece métodos para:</p>
 * <ul>
 *   <li>Gerar hash BCrypt de senhas</li>
 *   <li>Verificar senhas contra hashes</li>
 *   <li>Validar força de senhas</li>
 *   <li>Gerar senhas aleatórias seguras</li>
 * </ul>
 *
 * <p><b>Critérios de senha forte:</b></p>
 * <ul>
 *   <li>Mínimo 8 caracteres</li>
 *   <li>Pelo menos uma letra maiúscula</li>
 *   <li>Pelo menos uma letra minúscula</li>
 *   <li>Pelo menos um número</li>
 *   <li>Pelo menos um caractere especial (@$!%*?&)</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class PasswordUtil {

    /**
     * Cost factor para o BCrypt (12 = ~250ms por hash em hardware moderno).
     * Maior = mais seguro mas mais lento.
     */
    private static final int BCRYPT_COST = 12;

    /**
     * Padrão regex para validação de senha forte.
     * Requer: 8+ caracteres, maiúscula, minúscula, número, caractere especial.
     */
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    /**
     * Caracteres permitidos para geração de senhas aleatórias.
     */
    private static final String PASSWORD_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$!%*?&";

    /**
     * Gera um hash BCrypt a partir de uma senha em texto plano.
     *
     * <p>Utiliza BCrypt com cost factor 12 para criar um hash seguro
     * que pode ser armazenado no banco de dados.</p>
     *
     * @param plainPassword senha em texto plano
     * @return String hash BCrypt da senha
     * @throws IllegalArgumentException se a senha for nula ou vazia
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou vazia");
        }

        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Verifica se uma senha em texto plano corresponde a um hash BCrypt.
     *
     * @param plainPassword senha em texto plano fornecida pelo usuário
     * @param hashedPassword hash BCrypt armazenado no banco
     * @return boolean true se a senha corresponder ao hash, false caso contrário
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Hash inválido
            return false;
        }
    }

    /**
     * Valida se uma senha atende aos critérios de força.
     *
     * <p><b>Critérios:</b></p>
     * <ul>
     *   <li>Mínimo 8 caracteres</li>
     *   <li>Pelo menos uma letra maiúscula</li>
     *   <li>Pelo menos uma letra minúscula</li>
     *   <li>Pelo menos um número</li>
     *   <li>Pelo menos um caractere especial (@$!%*?&)</li>
     * </ul>
     *
     * @param password senha a ser validada
     * @return boolean true se a senha for forte, false caso contrário
     */
    public static boolean isStrongPassword(String password) {
        if (password == null) {
            return false;
        }

        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Valida a força da senha e retorna mensagem descritiva.
     *
     * @param password senha a ser validada
     * @return ValidationResult resultado da validação com mensagem
     */
    public static ValidationResult validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Senha não pode ser vazia");
        }

        if (password.length() < 8) {
            return new ValidationResult(false, "Senha deve ter no mínimo 8 caracteres");
        }

        if (!password.matches(".*[A-Z].*")) {
            return new ValidationResult(false, "Senha deve conter pelo menos uma letra maiúscula");
        }

        if (!password.matches(".*[a-z].*")) {
            return new ValidationResult(false, "Senha deve conter pelo menos uma letra minúscula");
        }

        if (!password.matches(".*\\d.*")) {
            return new ValidationResult(false, "Senha deve conter pelo menos um número");
        }

        if (!password.matches(".*[@$!%*?&].*")) {
            return new ValidationResult(false, "Senha deve conter pelo menos um caractere especial (@$!%*?&)");
        }

        return new ValidationResult(true, "Senha forte");
    }

    /**
     * Gera uma senha aleatória forte.
     *
     * @param length comprimento da senha (mínimo 8)
     * @return String senha aleatória gerada
     * @throws IllegalArgumentException se length < 8
     */
    public static String generateRandomPassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Comprimento mínimo da senha é 8 caracteres");
        }

        StringBuilder password = new StringBuilder(length);
        java.security.SecureRandom random = new java.security.SecureRandom();

        // Garantir que tenha pelo menos um de cada tipo obrigatório
        password.append(getRandomChar("ABCDEFGHIJKLMNOPQRSTUVWXYZ", random)); // Maiúscula
        password.append(getRandomChar("abcdefghijklmnopqrstuvwxyz", random)); // Minúscula
        password.append(getRandomChar("0123456789", random));                 // Número
        password.append(getRandomChar("@$!%*?&", random));                    // Especial

        // Preencher o resto aleatoriamente
        for (int i = 4; i < length; i++) {
            password.append(getRandomChar(PASSWORD_CHARS, random));
        }

        // Embaralhar os caracteres
        return shuffleString(password.toString(), random);
    }

    /**
     * Obtém um caractere aleatório de uma string.
     *
     * @param chars string com caracteres possíveis
     * @param random gerador de números aleatórios
     * @return char caractere aleatório
     */
    private static char getRandomChar(String chars, java.security.SecureRandom random) {
        return chars.charAt(random.nextInt(chars.length()));
    }

    /**
     * Embaralha os caracteres de uma string.
     *
     * @param input string a ser embaralhada
     * @param random gerador de números aleatórios
     * @return String string embaralhada
     */
    private static String shuffleString(String input, java.security.SecureRandom random) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
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
