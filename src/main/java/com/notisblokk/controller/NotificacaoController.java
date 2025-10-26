package com.notisblokk.controller;

import com.notisblokk.service.NotificacaoService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Controller responsável por notificações e alertas.
 *
 * <p>Gerencia os endpoints REST para obter alertas sobre notas pendentes:</p>
 * <ul>
 *   <li>GET /api/notificacoes/alertas - Listar alertas ordenados por urgência</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class NotificacaoController {

    private static final Logger logger = LoggerFactory.getLogger(NotificacaoController.class);
    private final NotificacaoService notificacaoService;

    /**
     * Construtor padrão.
     */
    public NotificacaoController() {
        this.notificacaoService = new NotificacaoService();
    }

    /**
     * GET /api/notificacoes/alertas
     * Gera e retorna alertas sobre notas pendentes, ordenados por urgência.
     *
     * <p>Os alertas incluem informações sobre:</p>
     * <ul>
     *   <li>Notas atrasadas (CRÍTICO)</li>
     *   <li>Notas vencendo em 0-1 dia (URGENTE)</li>
     *   <li>Notas vencendo em 2-3 dias (ATENÇÃO)</li>
     *   <li>Notas vencendo em 4-5 dias (AVISO)</li>
     * </ul>
     */
    public void gerarAlertas(Context ctx) {
        try {
            List<Map<String, Object>> alertas = notificacaoService.gerarAlertas();

            ctx.json(Map.of(
                "success", true,
                "dados", alertas,
                "total", alertas.size()
            ));

            logger.debug("Gerados {} alertas", alertas.size());

        } catch (Exception e) {
            logger.error("Erro ao gerar alertas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao gerar alertas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/notificacoes/estatisticas
     * Retorna estatísticas de alertas por nível de urgência.
     */
    public void obterEstatisticas(Context ctx) {
        try {
            Map<String, Long> estatisticas = notificacaoService.obterEstatisticasAlertas();

            ctx.json(Map.of(
                "success", true,
                "dados", estatisticas
            ));

            logger.debug("Estatísticas de alertas obtidas");

        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas de alertas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao obter estatísticas: " + e.getMessage()
            ));
        }
    }
}
