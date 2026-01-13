package com.notisblokk.controller;

import com.notisblokk.model.HistoricoCalculadora;
import com.notisblokk.service.CalculadoraService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller responsável pelo gerenciamento da calculadora.
 *
 * <p>Gerencia os endpoints REST para operações da calculadora:</p>
 * <ul>
 *   <li>GET /api/calculadora/historico - Listar histórico de cálculos</li>
 *   <li>POST /api/calculadora/calcular - Realizar cálculo</li>
 *   <li>DELETE /api/calculadora/historico - Limpar todo o histórico</li>
 *   <li>DELETE /api/calculadora/historico/{id} - Deletar item específico</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class CalculadoraController {

    private static final Logger logger = LoggerFactory.getLogger(CalculadoraController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final CalculadoraService calculadoraService;

    /**
     * Construtor padrão.
     */
    public CalculadoraController() {
        this.calculadoraService = new CalculadoraService();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param calculadoraService serviço de calculadora
     */
    public CalculadoraController(CalculadoraService calculadoraService) {
        this.calculadoraService = calculadoraService;
    }

    /**
     * GET /api/calculadora/historico
     * Retorna o histórico de cálculos do usuário (últimos 100 registros).
     */
    public void obterHistorico(Context ctx) {
        try {
            // Obter usuário da sessão
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            if (usuarioId == null) {
                ctx.status(401);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não autenticado"
                ));
                return;
            }

            // Buscar histórico
            List<HistoricoCalculadora> historico = calculadoraService.obterHistorico(usuarioId);

            // Formatar datas para brasileiro
            List<Map<String, Object>> historicoFormatado = historico.stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("expressao", item.getExpressao());
                    map.put("resultado", item.getResultado());
                    map.put("tipoOperacao", item.getTipoOperacao());

                    if (item.getDataCriacao() != null) {
                        map.put("dataCriacao", item.getDataCriacao().format(FORMATTER));
                    }

                    return map;
                })
                .toList();

            ctx.json(Map.of(
                "success", true,
                "dados", historicoFormatado
            ));

            logger.debug("Histórico retornado: {} registros para usuário ID {}", historico.size(), usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao obter histórico de cálculos", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao obter histórico: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/calculadora/calcular
     * Realiza um cálculo matemático e salva no histórico.
     *
     * Payload esperado:
     * {
     *   "expressao": "5+3"
     * }
     *
     * Resposta:
     * {
     *   "success": true,
     *   "dados": {
     *     "expressao": "5+3",
     *     "resultado": 8.0,
     *     "tipoOperacao": "SOMA"
     *   }
     * }
     */
    public void calcular(Context ctx) {
        try {
            // Obter sessão e usuário
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            if (sessaoId == null || usuarioId == null) {
                ctx.status(401);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não autenticado"
                ));
                return;
            }

            // Obter expressão do body
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String expressao = (String) body.get("expressao");

            if (expressao == null || expressao.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Expressão é obrigatória"
                ));
                return;
            }

            // Calcular e salvar
            HistoricoCalculadora resultado = calculadoraService.calcular(expressao, sessaoId, usuarioId);

            // Preparar resposta
            Map<String, Object> dados = new HashMap<>();
            dados.put("id", resultado.getId());
            dados.put("expressao", resultado.getExpressao());
            dados.put("resultado", resultado.getResultado());
            dados.put("tipoOperacao", resultado.getTipoOperacao());

            if (resultado.getDataCriacao() != null) {
                dados.put("dataCriacao", resultado.getDataCriacao().format(FORMATTER));
            }

            ctx.json(Map.of(
                "success", true,
                "message", "Cálculo realizado com sucesso",
                "dados", dados
            ));

            logger.info("Cálculo realizado: {} = {} (usuário ID: {})",
                    expressao, resultado.getResultado(), usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao realizar cálculo", e);

            // Determinar código de status HTTP baseado na mensagem de erro
            int status = e.getMessage().contains("vazia") ||
                         e.getMessage().contains("inválido") ||
                         e.getMessage().contains("não permitida") ||
                         e.getMessage().contains("muito longa") ? 400 : 500;

            ctx.status(status);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/calculadora/historico
     * Limpa todo o histórico de cálculos do usuário.
     */
    public void limparHistorico(Context ctx) {
        try {
            // Obter usuário da sessão
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            if (usuarioId == null) {
                ctx.status(401);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Usuário não autenticado"
                ));
                return;
            }

            // Limpar histórico
            int deletados = calculadoraService.limparHistorico(usuarioId);

            ctx.json(Map.of(
                "success", true,
                "message", "Histórico limpo com sucesso",
                "dados", Map.of("registrosDeletados", deletados)
            ));

            logger.info("Histórico limpo para usuário ID {}: {} registros deletados", usuarioId, deletados);

        } catch (Exception e) {
            logger.error("Erro ao limpar histórico", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao limpar histórico: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/calculadora/historico/{id}
     * Deleta um item específico do histórico.
     */
    public void deletarItem(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Deletar item
            calculadoraService.deletarItem(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Item deletado com sucesso"
            ));

            logger.info("Item do histórico ID {} deletado", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao deletar item do histórico", e);

            // Determinar código de status HTTP baseado na mensagem de erro
            int status = e.getMessage().contains("não encontrado") ? 404 : 500;

            ctx.status(status);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
