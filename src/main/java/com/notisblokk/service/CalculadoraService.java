package com.notisblokk.service;

import com.notisblokk.model.HistoricoCalculadora;
import com.notisblokk.repository.HistoricoCalculadoraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Serviço responsável pela lógica de negócio da calculadora.
 *
 * <p>Implementa operações matemáticas, validações e gerenciamento de histórico.</p>
 *
 * <p><b>Operações suportadas:</b></p>
 * <ul>
 *   <li>Básicas: + (adição), - (subtração), * (multiplicação), / (divisão)</li>
 *   <li>Avançadas: % (porcentagem), √ (raiz quadrada), x² (quadrado)</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class CalculadoraService {

    private static final Logger logger = LoggerFactory.getLogger(CalculadoraService.class);
    private static final int HISTORICO_LIMITE_PADRAO = 100;
    private static final Pattern NUMERO_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private final HistoricoCalculadoraRepository historicoRepository;

    /**
     * Construtor padrão.
     */
    public CalculadoraService() {
        this.historicoRepository = new HistoricoCalculadoraRepository();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param historicoRepository repositório de histórico
     */
    public CalculadoraService(HistoricoCalculadoraRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    /**
     * Obtém o histórico de cálculos do usuário (últimos 100 registros).
     *
     * @param usuarioId ID do usuário
     * @return List<HistoricoCalculadora> lista do histórico (mais recentes primeiro)
     * @throws Exception se houver erro ao buscar histórico
     */
    public List<HistoricoCalculadora> obterHistorico(Long usuarioId) throws Exception {
        try {
            return historicoRepository.buscarUltimos(HISTORICO_LIMITE_PADRAO, usuarioId);
        } catch (SQLException e) {
            logger.error("Erro ao obter histórico de cálculos do usuário ID {}", usuarioId, e);
            throw new Exception("Erro ao obter histórico: " + e.getMessage(), e);
        }
    }

    /**
     * Realiza um cálculo matemático, valida e salva no histórico.
     *
     * @param expressao expressão matemática (ex: "5+3", "10*2", "sqrt(16)", "4^2")
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return HistoricoCalculadora registro do cálculo salvo
     * @throws Exception se houver erro de validação ou cálculo
     */
    public HistoricoCalculadora calcular(String expressao, Long sessaoId, Long usuarioId) throws Exception {
        // Validar expressão
        validarExpressao(expressao);

        // Processar e calcular
        ResultadoCalculo resultado = processarExpressao(expressao);

        // Criar registro de histórico
        HistoricoCalculadora historico = new HistoricoCalculadora();
        historico.setExpressao(expressao);
        historico.setResultado(resultado.valor);
        historico.setTipoOperacao(resultado.tipoOperacao);

        // Salvar no banco
        try {
            HistoricoCalculadora salvo = historicoRepository.salvar(historico, sessaoId, usuarioId);
            logger.info("Cálculo realizado: {} = {} (tipo: {})", expressao, resultado.valor, resultado.tipoOperacao);
            return salvo;

        } catch (SQLException e) {
            logger.error("Erro ao salvar cálculo no histórico", e);
            throw new Exception("Erro ao salvar cálculo: " + e.getMessage(), e);
        }
    }

    /**
     * Limpa todo o histórico de cálculos do usuário.
     *
     * @param usuarioId ID do usuário
     * @return int número de registros deletados
     * @throws Exception se houver erro ao limpar histórico
     */
    public int limparHistorico(Long usuarioId) throws Exception {
        try {
            int deletados = historicoRepository.limparHistorico(usuarioId);
            logger.info("Histórico limpo para usuário ID {}: {} registros", usuarioId, deletados);
            return deletados;

        } catch (SQLException e) {
            logger.error("Erro ao limpar histórico do usuário ID {}", usuarioId, e);
            throw new Exception("Erro ao limpar histórico: " + e.getMessage(), e);
        }
    }

    /**
     * Deleta um item específico do histórico.
     *
     * @param id ID do registro a deletar
     * @throws Exception se houver erro ao deletar
     */
    public void deletarItem(Long id) throws Exception {
        try {
            historicoRepository.deletar(id);
            logger.info("Item do histórico ID {} deletado", id);

        } catch (SQLException e) {
            logger.error("Erro ao deletar item do histórico ID {}", id, e);
            throw new Exception("Erro ao deletar item: " + e.getMessage(), e);
        }
    }

    /**
     * Valida uma expressão matemática antes de calcular.
     *
     * @param expressao expressão a validar
     * @throws Exception se a expressão for inválida
     */
    private void validarExpressao(String expressao) throws Exception {
        if (expressao == null || expressao.trim().isEmpty()) {
            throw new Exception("Expressão não pode estar vazia");
        }

        if (expressao.length() > 500) {
            throw new Exception("Expressão muito longa (máximo 500 caracteres)");
        }

        // Verificar caracteres perigosos (prevenir injeção)
        if (expressao.matches(".*[;<>|&$`].*")) {
            throw new Exception("Expressão contém caracteres inválidos");
        }
    }

    /**
     * Processa e calcula uma expressão matemática.
     *
     * @param expressao expressão matemática
     * @return ResultadoCalculo resultado com valor e tipo de operação
     * @throws Exception se houver erro no cálculo
     */
    private ResultadoCalculo processarExpressao(String expressao) throws Exception {
        String expr = expressao.trim();

        // Operações especiais: raiz quadrada
        if (expr.toLowerCase().startsWith("sqrt(") && expr.endsWith(")")) {
            String numStr = expr.substring(5, expr.length() - 1).trim();
            double numero = parseNumero(numStr);

            if (numero < 0) {
                throw new Exception("Não é possível calcular raiz quadrada de número negativo");
            }

            double resultado = Math.sqrt(numero);
            return new ResultadoCalculo(resultado, "RAIZ");
        }

        // Operações especiais: quadrado (x^2)
        if (expr.contains("^2")) {
            String numStr = expr.replace("^2", "").trim();
            double numero = parseNumero(numStr);
            double resultado = numero * numero;
            return new ResultadoCalculo(resultado, "QUADRADO");
        }

        // Operações binárias: detectar operador
        String operador = detectarOperador(expr);
        if (operador == null) {
            throw new Exception("Operador não encontrado na expressão: " + expr);
        }

        // Dividir expressão em dois números
        String[] partes = expr.split("\\" + operador, 2);
        if (partes.length != 2) {
            throw new Exception("Expressão inválida: " + expr);
        }

        double numero1 = parseNumero(partes[0].trim());
        double numero2 = parseNumero(partes[1].trim());

        // Executar operação
        double resultado = executarOperacao(numero1, numero2, operador);
        String tipoOperacao = getTipoOperacao(operador);

        return new ResultadoCalculo(resultado, tipoOperacao);
    }

    /**
     * Detecta qual operador está presente na expressão.
     *
     * @param expressao expressão matemática
     * @return String operador encontrado ou null
     */
    private String detectarOperador(String expressao) {
        // Ordem de precedência: % primeiro (porcentagem), depois *, /, +, -
        if (expressao.contains("%")) return "%";
        if (expressao.contains("*")) return "*";
        if (expressao.contains("/")) return "/";

        // Para + e -, verificar que não seja sinal de número negativo
        int indexMais = expressao.indexOf('+');
        int indexMenos = expressao.lastIndexOf('-'); // último '-' para evitar número negativo

        if (indexMais > 0) return "+";
        if (indexMenos > 0) return "-";

        return null;
    }

    /**
     * Converte string para número, validando formato.
     *
     * @param numeroStr string representando número
     * @return double número parseado
     * @throws Exception se o formato for inválido
     */
    private double parseNumero(String numeroStr) throws Exception {
        if (!NUMERO_PATTERN.matcher(numeroStr).matches()) {
            throw new Exception("Número inválido: " + numeroStr);
        }

        try {
            return Double.parseDouble(numeroStr);
        } catch (NumberFormatException e) {
            throw new Exception("Erro ao converter número: " + numeroStr, e);
        }
    }

    /**
     * Executa uma operação matemática binária.
     *
     * @param a primeiro operando
     * @param b segundo operando
     * @param operador operador (+, -, *, /, %)
     * @return double resultado da operação
     * @throws Exception se houver erro na operação
     */
    private double executarOperacao(double a, double b, String operador) throws Exception {
        return switch (operador) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> {
                if (b == 0) {
                    throw new Exception("Divisão por zero não permitida");
                }
                yield a / b;
            }
            case "%" -> (a * b) / 100; // a% de b
            default -> throw new Exception("Operador não suportado: " + operador);
        };
    }

    /**
     * Converte operador para nome do tipo de operação.
     *
     * @param operador símbolo do operador
     * @return String nome da operação
     */
    private String getTipoOperacao(String operador) {
        return switch (operador) {
            case "+" -> "SOMA";
            case "-" -> "SUBTRACAO";
            case "*" -> "MULTIPLICACAO";
            case "/" -> "DIVISAO";
            case "%" -> "PORCENTAGEM";
            default -> "DESCONHECIDA";
        };
    }

    /**
     * Classe interna para representar resultado de cálculo com tipo de operação.
     */
    private static class ResultadoCalculo {
        final double valor;
        final String tipoOperacao;

        ResultadoCalculo(double valor, String tipoOperacao) {
            this.valor = valor;
            this.tipoOperacao = tipoOperacao;
        }
    }
}
