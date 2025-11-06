package com.notisblokk.audiencias.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utilitário para validações de dados do módulo de audiências.
 *
 * <p>Contém validações específicas para:</p>
 * <ul>
 *   <li>Número de processo judicial (formato CNJ)</li>
 *   <li>CPF</li>
 *   <li>Número de OAB</li>
 *   <li>Email</li>
 *   <li>Telefone</li>
 *   <li>Campos obrigatórios</li>
 * </ul>
 */
public class ValidationUtil {

    // Expressões regulares
    private static final Pattern NUMERO_PROCESSO_PATTERN =
        Pattern.compile("^\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}$");

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern TELEFONE_PATTERN =
        Pattern.compile("^\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}$");

    private static final Pattern OAB_PATTERN =
        Pattern.compile("^\\d{3,6}(/[A-Z]{2})?$");

    // ========================================================================
    // VALIDAÇÃO DE NÚMERO DE PROCESSO
    // ========================================================================

    /**
     * Valida número de processo judicial no formato CNJ.
     *
     * <p>Formato: NNNNNNN-NN.NNNN.N.NN.NNNN</p>
     * <p>Exemplo: 0001234-56.2025.8.26.0001</p>
     *
     * @param numeroProcesso número do processo a validar
     * @return true se válido, false caso contrário
     */
    public static boolean validarNumeroProcesso(String numeroProcesso) {
        if (numeroProcesso == null || numeroProcesso.trim().isEmpty()) {
            // DEBUG_AUDIENCIAS: Log de validação
            System.out.println("DEBUG_AUDIENCIAS: Validação de processo - string vazia");
            return false;
        }

        boolean valido = NUMERO_PROCESSO_PATTERN.matcher(numeroProcesso.trim()).matches();

        if (!valido) {
            // DEBUG_AUDIENCIAS: Log de formato inválido
            System.err.println("DEBUG_AUDIENCIAS: Número de processo inválido: " + numeroProcesso);
            System.err.println("DEBUG_AUDIENCIAS: Formato esperado: NNNNNNN-NN.NNNN.N.NN.NNNN");
        }

        return valido;
    }

    // ========================================================================
    // VALIDAÇÃO DE CPF
    // ========================================================================

    /**
     * Valida CPF com verificação de dígitos verificadores.
     *
     * <p>Aceita formatos: 999.999.999-99 ou 99999999999</p>
     *
     * @param cpf CPF a validar
     * @return true se válido, false caso contrário
     */
    public static boolean validarCPF(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            System.out.println("DEBUG_AUDIENCIAS: Validação de CPF - string vazia");
            return false;
        }

        // Remover formatação
        String cpfLimpo = cpf.replaceAll("[.\\-]", "");

        // Verificar se tem 11 dígitos
        if (!cpfLimpo.matches("\\d{11}")) {
            System.err.println("DEBUG_AUDIENCIAS: CPF inválido (formato): " + cpf);
            return false;
        }

        // Verificar se todos os dígitos são iguais (ex: 111.111.111-11)
        if (cpfLimpo.matches("(\\d)\\1{10}")) {
            System.err.println("DEBUG_AUDIENCIAS: CPF inválido (dígitos repetidos): " + cpf);
            return false;
        }

        // Calcular dígitos verificadores
        int[] digitos = new int[11];
        for (int i = 0; i < 11; i++) {
            digitos[i] = Character.getNumericValue(cpfLimpo.charAt(i));
        }

        // Calcular primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += digitos[i] * (10 - i);
        }
        int primeiroDigito = 11 - (soma % 11);
        if (primeiroDigito >= 10) primeiroDigito = 0;

        // Calcular segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += digitos[i] * (11 - i);
        }
        int segundoDigito = 11 - (soma % 11);
        if (segundoDigito >= 10) segundoDigito = 0;

        // Verificar dígitos
        boolean valido = (digitos[9] == primeiroDigito && digitos[10] == segundoDigito);

        if (!valido) {
            System.err.println("DEBUG_AUDIENCIAS: CPF inválido (dígito verificador): " + cpf);
        } else {
            System.out.println("DEBUG_AUDIENCIAS: CPF válido: " + cpf);
        }

        return valido;
    }

    /**
     * Formata CPF no padrão 999.999.999-99.
     *
     * @param cpf CPF sem formatação (apenas dígitos)
     * @return CPF formatado, ou string original se inválida
     */
    public static String formatarCPF(String cpf) {
        if (cpf == null) return null;

        String cpfLimpo = cpf.replaceAll("[.\\-]", "");
        if (cpfLimpo.matches("\\d{11}")) {
            return cpfLimpo.substring(0, 3) + "." +
                   cpfLimpo.substring(3, 6) + "." +
                   cpfLimpo.substring(6, 9) + "-" +
                   cpfLimpo.substring(9, 11);
        }
        return cpf;
    }

    // ========================================================================
    // VALIDAÇÃO DE OAB
    // ========================================================================

    /**
     * Valida número de OAB.
     *
     * <p>Formatos aceitos:</p>
     * <ul>
     *   <li>123456 (apenas número)</li>
     *   <li>123456/SP (número + UF)</li>
     * </ul>
     *
     * @param oab número de OAB a validar
     * @return true se válido, false caso contrário
     */
    public static boolean validarOAB(String oab) {
        if (oab == null || oab.trim().isEmpty()) {
            System.out.println("DEBUG_AUDIENCIAS: Validação de OAB - string vazia");
            return false;
        }

        boolean valido = OAB_PATTERN.matcher(oab.trim().toUpperCase()).matches();

        if (!valido) {
            System.err.println("DEBUG_AUDIENCIAS: OAB inválida: " + oab);
            System.err.println("DEBUG_AUDIENCIAS: Formato esperado: 123456 ou 123456/SP");
        }

        return valido;
    }

    // ========================================================================
    // VALIDAÇÃO DE EMAIL
    // ========================================================================

    /**
     * Valida endereço de email.
     *
     * @param email email a validar
     * @return true se válido, false caso contrário
     */
    public static boolean validarEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        boolean valido = EMAIL_PATTERN.matcher(email.trim()).matches();

        if (!valido) {
            System.err.println("DEBUG_AUDIENCIAS: Email inválido: " + email);
        }

        return valido;
    }

    // ========================================================================
    // VALIDAÇÃO DE TELEFONE
    // ========================================================================

    /**
     * Valida número de telefone.
     *
     * <p>Formatos aceitos:</p>
     * <ul>
     *   <li>(11) 1234-5678 (fixo)</li>
     *   <li>(11) 91234-5678 (celular)</li>
     *   <li>11 1234-5678</li>
     *   <li>1112345678</li>
     * </ul>
     *
     * @param telefone telefone a validar
     * @return true se válido, false caso contrário
     */
    public static boolean validarTelefone(String telefone) {
        if (telefone == null || telefone.trim().isEmpty()) {
            return false;
        }

        return TELEFONE_PATTERN.matcher(telefone.trim()).matches();
    }

    /**
     * Formata telefone no padrão (99) 9999-9999 ou (99) 99999-9999.
     *
     * @param telefone telefone sem formatação (apenas dígitos)
     * @return telefone formatado, ou string original se inválido
     */
    public static String formatarTelefone(String telefone) {
        if (telefone == null) return null;

        String telefoneLimpo = telefone.replaceAll("[^0-9]", "");

        if (telefoneLimpo.length() == 10) {
            // Formato fixo: (99) 9999-9999
            return "(" + telefoneLimpo.substring(0, 2) + ") " +
                   telefoneLimpo.substring(2, 6) + "-" +
                   telefoneLimpo.substring(6, 10);
        } else if (telefoneLimpo.length() == 11) {
            // Formato celular: (99) 99999-9999
            return "(" + telefoneLimpo.substring(0, 2) + ") " +
                   telefoneLimpo.substring(2, 7) + "-" +
                   telefoneLimpo.substring(7, 11);
        }

        return telefone;
    }

    // ========================================================================
    // VALIDAÇÕES GERAIS
    // ========================================================================

    /**
     * Valida se um campo obrigatório está preenchido.
     *
     * @param valor valor a validar
     * @param nomeCampo nome do campo para mensagem de erro
     * @param erros lista de erros onde adicionar mensagem
     * @return true se válido, false caso contrário
     */
    public static boolean validarObrigatorio(String valor, String nomeCampo, List<String> erros) {
        if (valor == null || valor.trim().isEmpty()) {
            String mensagem = nomeCampo + " é obrigatório";
            erros.add(mensagem);
            // DEBUG_AUDIENCIAS: Log de campo obrigatório não preenchido
            System.err.println("DEBUG_AUDIENCIAS: Validação falhou - " + mensagem);
            return false;
        }
        return true;
    }

    /**
     * Valida se um campo obrigatório (Object) está preenchido.
     *
     * @param valor valor a validar
     * @param nomeCampo nome do campo para mensagem de erro
     * @param erros lista de erros onde adicionar mensagem
     * @return true se válido, false caso contrário
     */
    public static boolean validarObrigatorio(Object valor, String nomeCampo, List<String> erros) {
        if (valor == null) {
            String mensagem = nomeCampo + " é obrigatório";
            erros.add(mensagem);
            // DEBUG_AUDIENCIAS: Log de campo obrigatório não preenchido
            System.err.println("DEBUG_AUDIENCIAS: Validação falhou - " + mensagem);
            return false;
        }
        return true;
    }

    /**
     * Valida tamanho mínimo de string.
     *
     * @param valor valor a validar
     * @param tamanhoMinimo tamanho mínimo
     * @param nomeCampo nome do campo
     * @param erros lista de erros
     * @return true se válido, false caso contrário
     */
    public static boolean validarTamanhoMinimo(String valor, int tamanhoMinimo,
                                               String nomeCampo, List<String> erros) {
        if (valor != null && valor.trim().length() < tamanhoMinimo) {
            String mensagem = nomeCampo + " deve ter pelo menos " + tamanhoMinimo + " caracteres";
            erros.add(mensagem);
            System.err.println("DEBUG_AUDIENCIAS: Validação falhou - " + mensagem);
            return false;
        }
        return true;
    }

    /**
     * Valida tamanho máximo de string.
     *
     * @param valor valor a validar
     * @param tamanhoMaximo tamanho máximo
     * @param nomeCampo nome do campo
     * @param erros lista de erros
     * @return true se válido, false caso contrário
     */
    public static boolean validarTamanhoMaximo(String valor, int tamanhoMaximo,
                                               String nomeCampo, List<String> erros) {
        if (valor != null && valor.trim().length() > tamanhoMaximo) {
            String mensagem = nomeCampo + " deve ter no máximo " + tamanhoMaximo + " caracteres";
            erros.add(mensagem);
            System.err.println("DEBUG_AUDIENCIAS: Validação falhou - " + mensagem);
            return false;
        }
        return true;
    }

    /**
     * Valida se um número inteiro está em um intervalo.
     *
     * @param valor valor a validar
     * @param minimo valor mínimo (inclusivo)
     * @param maximo valor máximo (inclusivo)
     * @param nomeCampo nome do campo
     * @param erros lista de erros
     * @return true se válido, false caso contrário
     */
    public static boolean validarIntervalo(Integer valor, int minimo, int maximo,
                                           String nomeCampo, List<String> erros) {
        if (valor != null && (valor < minimo || valor > maximo)) {
            String mensagem = nomeCampo + " deve estar entre " + minimo + " e " + maximo;
            erros.add(mensagem);
            System.err.println("DEBUG_AUDIENCIAS: Validação falhou - " + mensagem);
            return false;
        }
        return true;
    }

    /**
     * Cria uma nova lista de erros.
     *
     * @return Lista vazia para armazenar erros de validação
     */
    public static List<String> novaListaErros() {
        return new ArrayList<>();
    }

    /**
     * Verifica se há erros na lista.
     *
     * @param erros lista de erros
     * @return true se há erros, false caso contrário
     */
    public static boolean temErros(List<String> erros) {
        return erros != null && !erros.isEmpty();
    }

    /**
     * Retorna mensagem de erro formatada.
     *
     * @param erros lista de erros
     * @return String com todos os erros separados por vírgula
     */
    public static String formatarErros(List<String> erros) {
        if (erros == null || erros.isEmpty()) {
            return "";
        }
        return String.join(", ", erros);
    }
}
