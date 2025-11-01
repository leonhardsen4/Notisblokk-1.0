package com.notisblokk.controller;

import com.notisblokk.model.Session;
import com.notisblokk.model.User;
import com.notisblokk.service.SessionService;
import com.notisblokk.service.UserService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller responsável pelo gerenciamento de sessões.
 *
 * <p>Funcionalidades:</p>
 * <ul>
 *   <li>Listagem de sessões com filtros e ordenação</li>
 *   <li>Encerramento de sessões ativas</li>
 *   <li>Estatísticas de sessões</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-27
 */
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
    private final SessionService sessionService;
    private final UserService userService;

    public SessionController() {
        this.sessionService = new SessionService();
        this.userService = new UserService();
    }

    /**
     * GET /admin/sessions
     * Exibe a página de gerenciamento de sessões.
     */
    public void index(Context ctx) {
        try {
            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            model.put("title", "Gerenciamento de Sessões - Notisblokk");

            // Mensagens
            String success = ctx.sessionAttribute("sessionSuccess");
            String error = ctx.sessionAttribute("sessionError");

            if (success != null) {
                model.put("success", success);
                ctx.sessionAttribute("sessionSuccess", null);
            }
            if (error != null) {
                model.put("error", error);
                ctx.sessionAttribute("sessionError", null);
            }

            ctx.contentType("text/html; charset=utf-8");
            ctx.render("sessions/index", model);

        } catch (Exception e) {
            logger.error("Erro ao carregar página de sessões", e);
            ctx.status(500);
            ctx.result("Erro ao carregar página: " + e.getMessage());
        }
    }

    /**
     * GET /api/sessions/listar
     * Lista todas as sessões do sistema com informações de usuário.
     */
    public void listar(Context ctx) {
        try {
            List<Session> sessions = sessionService.listarUltimasSessoes(1000); // Últimas 1000 sessões

            // Buscar informações de usuários para enriquecer os dados
            Map<Long, User> usuariosMap = new HashMap<>();
            for (Session session : sessions) {
                if (!usuariosMap.containsKey(session.getUserId())) {
                    try {
                        Optional<User> user = userService.buscarPorId(session.getUserId());
                        user.ifPresent(u -> usuariosMap.put(u.getId(), u));
                    } catch (Exception e) {
                        logger.warn("Erro ao buscar usuário ID {}", session.getUserId());
                    }
                }
            }

            // Criar lista enriquecida com nome do usuário
            List<Map<String, Object>> sessionsEnriquecidas = sessions.stream().map(session -> {
                Map<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("id", session.getId());
                sessionMap.put("userId", session.getUserId());

                User user = usuariosMap.get(session.getUserId());
                sessionMap.put("userName", user != null ? user.getFullName() : "Usuário Desconhecido");
                sessionMap.put("userEmail", user != null ? user.getEmail() : "");

                sessionMap.put("loginTime", session.getFormattedLoginTime());
                sessionMap.put("logoutTime", session.getFormattedLogoutTime());
                sessionMap.put("ipAddress", session.getIpAddress());
                sessionMap.put("browser", session.getBrowser());
                sessionMap.put("userAgent", session.getUserAgent());
                sessionMap.put("status", session.getStatus().name());
                sessionMap.put("statusDisplay", session.getStatusDisplayName());
                sessionMap.put("active", session.isActive());
                sessionMap.put("duration", session.getFormattedDuration());

                return sessionMap;
            }).collect(Collectors.toList());

            ctx.json(Map.of(
                "success", true,
                "dados", sessionsEnriquecidas
            ));

        } catch (Exception e) {
            logger.error("Erro ao listar sessões", e);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar sessões: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/sessions/stats
     * Retorna estatísticas sobre as sessões.
     */
    public void obterEstatisticas(Context ctx) {
        try {
            long totalSessoes = sessionService.contarTotalSessoes();
            long sessoesAtivas = sessionService.contarSessoesAtivas();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSessoes", totalSessoes);
            stats.put("sessoesAtivas", sessoesAtivas);
            stats.put("sessoesInativas", totalSessoes - sessoesAtivas);

            double percentualAtivas = totalSessoes > 0
                ? (sessoesAtivas * 100.0 / totalSessoes)
                : 0;
            stats.put("percentualAtivas", String.format("%.1f%%", percentualAtivas));

            ctx.json(Map.of(
                "success", true,
                "dados", stats
            ));

        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas de sessões", e);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao obter estatísticas: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/sessions/{id}/encerrar
     * Encerra uma sessão específica.
     */
    public void encerrar(Context ctx) {
        try {
            Long sessionId = Long.parseLong(ctx.pathParam("id"));

            // Verificar se a sessão existe
            Optional<Session> sessionOpt = sessionService.buscarPorId(sessionId);
            if (sessionOpt.isEmpty()) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "message", "Sessão não encontrada"
                ));
                return;
            }

            Session session = sessionOpt.get();

            // Não permitir encerrar sessão já encerrada
            if (!session.isActive()) {
                ctx.status(400).json(Map.of(
                    "success", false,
                    "message", "Sessão já está encerrada"
                ));
                return;
            }

            // Encerrar a sessão
            sessionService.encerrarSessao(sessionId);

            logger.info("Sessão {} encerrada pelo administrador {}",
                       sessionId, SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.json(Map.of(
                "success", true,
                "message", "Sessão encerrada com sucesso"
            ));

        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                "success", false,
                "message", "ID de sessão inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao encerrar sessão", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "message", "Erro ao encerrar sessão: " + e.getMessage()
            ));
        }
    }
}
