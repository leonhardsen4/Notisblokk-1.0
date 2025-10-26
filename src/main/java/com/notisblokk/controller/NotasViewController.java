package com.notisblokk.controller;

import com.notisblokk.model.NotaDTO;
import com.notisblokk.service.NotaService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller responsável pela visualização da página de anotações.
 *
 * <p>Renderiza a interface HTML para gerenciamento de notas.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class NotasViewController {

    private static final Logger logger = LoggerFactory.getLogger(NotasViewController.class);
    private final NotaService notaService;

    /**
     * Construtor padrão.
     */
    public NotasViewController() {
        this.notaService = new NotaService();
    }

    /**
     * GET /notas
     * Exibe a página de gerenciamento de anotações.
     */
    public void index(Context ctx) {
        try {
            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Anotações - Notisblokk");

            logger.debug("Página de anotações acessada por: {}", SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("notas/index", model);

        } catch (Exception e) {
            logger.error("Erro ao exibir página de anotações", e);
            ctx.status(500);
            ctx.result("Erro ao carregar página de anotações: " + e.getMessage());
        }
    }

    /**
     * GET /notas/nova
     * Exibe o formulário para criar uma nova nota.
     */
    public void novaNota(Context ctx) {
        try {
            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Nova Nota - Notisblokk");

            logger.debug("Formulário de nova nota acessado por: {}", SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("notas/form", model);

        } catch (Exception e) {
            logger.error("Erro ao exibir formulário de nova nota", e);
            ctx.status(500);
            ctx.result("Erro ao carregar formulário: " + e.getMessage());
        }
    }

    /**
     * GET /notas/editar/:id
     * Exibe o formulário para editar uma nota existente.
     */
    public void editarNota(Context ctx) {
        try {
            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");

            Long id = Long.parseLong(ctx.pathParam("id"));

            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Buscar nota
            Optional<NotaDTO> notaOpt = notaService.buscarPorId(id);
            if (notaOpt.isEmpty()) {
                ctx.status(404);
                ctx.result("Nota não encontrada");
                return;
            }

            model.put("nota", notaOpt.get());
            model.put("title", "Editar Nota - Notisblokk");

            logger.debug("Formulário de edição de nota {} acessado por: {}", id, SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("notas/form", model);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.result("ID inválido");
        } catch (Exception e) {
            logger.error("Erro ao exibir formulário de edição de nota", e);
            ctx.status(500);
            ctx.result("Erro ao carregar formulário: " + e.getMessage());
        }
    }
}
