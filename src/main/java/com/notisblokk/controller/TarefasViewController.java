package com.notisblokk.controller;

import com.notisblokk.model.TarefaDTO;
import com.notisblokk.service.TarefaService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller responsável pela visualização da página de tarefas.
 *
 * <p>Renderiza a interface HTML para gerenciamento de tarefas.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class TarefasViewController {

    private static final Logger logger = LoggerFactory.getLogger(TarefasViewController.class);
    private final TarefaService tarefaService;

    /**
     * Construtor padrão.
     */
    public TarefasViewController() {
        this.tarefaService = new TarefaService();
    }

    /**
     * GET /tarefas
     * Exibe a página de gerenciamento de tarefas.
     */
    public void index(Context ctx) {
        try {
            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Tarefas - Notisblokk");

            logger.debug("Página de tarefas acessada por: {}", SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("tarefas/index", model);

        } catch (Exception e) {
            logger.error("Erro ao exibir página de tarefas", e);
            ctx.status(500);
            ctx.result("Erro ao carregar página de tarefas: " + e.getMessage());
        }
    }

    /**
     * GET /tarefas/nova
     * Exibe o formulário para criar uma nova tarefa.
     */
    public void novaTarefa(Context ctx) {
        try {
            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Nova Tarefa - Notisblokk");

            logger.debug("Formulário de nova tarefa acessado por: {}", SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("tarefas/form", model);

        } catch (Exception e) {
            logger.error("Erro ao exibir formulário de nova tarefa", e);
            ctx.status(500);
            ctx.result("Erro ao carregar formulário: " + e.getMessage());
        }
    }

    /**
     * GET /tarefas/editar/:id
     * Exibe o formulário para editar uma tarefa existente.
     */
    public void editarTarefa(Context ctx) {
        try {
            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");

            Long id = Long.parseLong(ctx.pathParam("id"));

            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Buscar tarefa
            Optional<TarefaDTO> tarefaOpt = tarefaService.buscarPorId(id);
            if (tarefaOpt.isEmpty()) {
                ctx.status(404);
                ctx.result("Tarefa não encontrada");
                return;
            }

            model.put("tarefa", tarefaOpt.get());
            model.put("title", "Editar Tarefa - Notisblokk");

            logger.debug("Formulário de edição de tarefa {} acessado por: {}", id, SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("tarefas/form", model);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.result("ID inválido");
        } catch (Exception e) {
            logger.error("Erro ao exibir formulário de edição de tarefa", e);
            ctx.status(500);
            ctx.result("Erro ao carregar formulário: " + e.getMessage());
        }
    }
}
