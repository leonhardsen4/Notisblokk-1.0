package com.notisblokk.controller;

import com.notisblokk.model.Session;
import com.notisblokk.service.SessionService;
import com.notisblokk.service.UserService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller responsável pelo dashboard principal.
 *
 * <p>Exibe estatísticas e informações gerais do sistema para usuários autenticados.</p>
 *
 * <p><b>Estatísticas exibidas:</b></p>
 * <ul>
 *   <li>Total de usuários</li>
 *   <li>Usuários ativos</li>
 *   <li>Total de sessões</li>
 *   <li>Sessões ativas</li>
 *   <li>Últimos acessos</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final UserService userService;
    private final SessionService sessionService;

    /**
     * Construtor padrão.
     */
    public DashboardController() {
        this.userService = new UserService();
        this.sessionService = new SessionService();
    }

    /**
     * GET /dashboard
     * Exibe o dashboard principal com estatísticas do sistema.
     */
    public void index(Context ctx) {
        try {
            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Dashboard - Notisblokk");

            // Estatísticas
            long totalUsuarios = userService.contarTotal();
            long usuariosAtivos = userService.contarAtivos();
            long totalSessoes = sessionService.contarTotalSessoes();
            long sessoesAtivas = sessionService.contarSessoesAtivas();

            model.put("totalUsuarios", totalUsuarios);
            model.put("usuariosAtivos", usuariosAtivos);
            model.put("usuariosInativos", totalUsuarios - usuariosAtivos);
            model.put("totalSessoes", totalSessoes);
            model.put("sessoesAtivas", sessoesAtivas);

            // Calcular percentuais
            double percentualAtivos = totalUsuarios > 0
                ? (usuariosAtivos * 100.0 / totalUsuarios)
                : 0;
            model.put("percentualAtivos", String.format("%.1f%%", percentualAtivos));

            double percentualSessoesAtivas = totalSessoes > 0
                ? (sessoesAtivas * 100.0 / totalSessoes)
                : 0;
            model.put("percentualSessoesAtivas", String.format("%.1f%%", percentualSessoesAtivas));

            // Últimas sessões (últimas 10)
            List<Session> ultimasSessoes = sessionService.listarUltimasSessoes(10);
            model.put("ultimasSessoes", ultimasSessoes);

            // Status do sistema
            model.put("sistemaStatus", "Operacional");
            model.put("sistemaStatusClass", "success");

            // Informações adicionais
            model.put("poolStatus", com.notisblokk.config.DatabaseConfig.getPoolStats());

            logger.debug("Dashboard acessado por: {}", SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("dashboard/index", model);

        } catch (Exception e) {
            logger.error("Erro ao carregar dashboard", e);
            ctx.status(500);
            ctx.result("Erro ao carregar dashboard: " + e.getMessage());
        }
    }

    /**
     * GET /api/dashboard/stats
     * Retorna estatísticas do dashboard em formato JSON (para AJAX).
     */
    public void getStats(Context ctx) {
        try {
            Map<String, Object> stats = new HashMap<>();

            stats.put("totalUsuarios", userService.contarTotal());
            stats.put("usuariosAtivos", userService.contarAtivos());
            stats.put("totalSessoes", sessionService.contarTotalSessoes());
            stats.put("sessoesAtivas", sessionService.contarSessoesAtivas());
            stats.put("poolStatus", com.notisblokk.config.DatabaseConfig.getPoolStats());

            ctx.json(stats);

        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao obter estatísticas"
            ));
        }
    }
}
