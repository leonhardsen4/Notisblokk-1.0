package com.notisblokk.audiencias.controller;

import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para renderizar views (HTML) do módulo de Processos.
 *
 * <p>Rotas de view (Thymeleaf):</p>
 * <ul>
 *   <li>GET /processos - Lista de processos</li>
 *   <li>GET /processos/{id} - Detalhes do processo</li>
 * </ul>
 */
public class ProcessosViewController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessosViewController.class);

    /**
     * GET /processos
     * Renderiza a página de listagem de processos.
     */
    public void index(Context ctx) {
        logger.debug("ProcessosViewController.index()");

        // Verificar autenticação (middleware já fez isso, mas por garantia)
        if (!SessionUtil.isAuthenticated(ctx)) {
            ctx.redirect("/auth/login");
            return;
        }

        // Passar dados do usuário para o template
        ctx.render("processos/index.html", Map.of(
            "username", SessionUtil.getUsername(ctx),
            "isAdmin", SessionUtil.isAdmin(ctx)
        ));
    }

    /**
     * GET /processos/{id}
     * Renderiza a página de detalhes de um processo específico.
     */
    public void detalhes(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            logger.debug("ProcessosViewController.detalhes() - ID: {}", id);

            // Verificar autenticação
            if (!SessionUtil.isAuthenticated(ctx)) {
                ctx.redirect("/auth/login");
                return;
            }

            // Passar dados do usuário e ID do processo para o template
            ctx.render("processos/detalhes.html", Map.of(
                "username", SessionUtil.getUsername(ctx),
                "isAdmin", SessionUtil.isAdmin(ctx),
                "processoId", id
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.result("ID de processo inválido");
        }
    }
}
