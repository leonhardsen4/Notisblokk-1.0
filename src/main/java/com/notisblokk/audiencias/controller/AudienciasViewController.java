package com.notisblokk.audiencias.controller;

import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller responsável pela visualização da página de audiências.
 *
 * <p>Renderiza a interface HTML para gerenciamento de audiências judiciais.</p>
 */
public class AudienciasViewController {

    private static final Logger logger = LoggerFactory.getLogger(AudienciasViewController.class);

    /**
     * GET /audiencias
     * Exibe a página principal de gerenciamento de audiências.
     */
    public void index(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: AudienciasViewController.index()");

            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Audiências - Notisblokk");

            // Renderizar template Thymeleaf
            ctx.render("audiencias/index.html", model);

            logger.debug("DEBUG_AUDIENCIAS: Página de audiências renderizada com sucesso");

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao renderizar página de audiências", e);
            ctx.status(500);
            ctx.result("Erro ao carregar página de audiências: " + e.getMessage());
        }
    }

    /**
     * GET /audiencias/nova
     * Exibe o formulário de nova audiência.
     */
    public void novaAudiencia(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: AudienciasViewController.novaAudiencia()");

            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();
            model.putAll(SessionUtil.getSessionAttributes(ctx));
            model.put("title", "Nova Audiência - Notisblokk");

            // Renderizar template audiencias/form.html
            ctx.render("audiencias/form.html", model);

            logger.debug("DEBUG_AUDIENCIAS: Formulário de nova audiência renderizado");

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao renderizar formulário", e);
            ctx.status(500);
            ctx.result("Erro ao carregar formulário: " + e.getMessage());
        }
    }

    /**
     * GET /audiencias/editar/{id}
     * Exibe o formulário de edição de audiência.
     */
    public void editarAudiencia(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: AudienciasViewController.editarAudiencia() - ID: " + id);

            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();
            model.putAll(SessionUtil.getSessionAttributes(ctx));
            model.put("title", "Editar Audiência - Notisblokk");
            model.put("audienciaId", id);

            // Renderizar template audiencias/form.html
            ctx.render("audiencias/form.html", model);

            logger.debug("DEBUG_AUDIENCIAS: Formulário de edição renderizado - ID: {}", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.result("ID de audiência inválido");
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao renderizar formulário de edição", e);
            ctx.status(500);
            ctx.result("Erro ao carregar formulário: " + e.getMessage());
        }
    }

    /**
     * GET /audiencias/advogados
     * Exibe a página de gerenciamento de advogados.
     */
    public void advogados(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: AudienciasViewController.advogados()");

            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();
            model.putAll(SessionUtil.getSessionAttributes(ctx));
            model.put("title", "Advogados - Notisblokk");

            ctx.render("audiencias/advogados.html", model);

            logger.debug("DEBUG_AUDIENCIAS: Página de advogados renderizada");

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao renderizar página de advogados", e);
            ctx.status(500);
            ctx.result("Erro ao carregar página: " + e.getMessage());
        }
    }

    /**
     * GET /audiencias/pessoas
     * Exibe a página de gerenciamento de pessoas.
     */
    public void pessoas(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: AudienciasViewController.pessoas()");

            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();
            model.putAll(SessionUtil.getSessionAttributes(ctx));
            model.put("title", "Pessoas - Notisblokk");

            ctx.render("audiencias/pessoas.html", model);

            logger.debug("DEBUG_AUDIENCIAS: Página de pessoas renderizada");

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao renderizar página de pessoas", e);
            ctx.status(500);
            ctx.result("Erro ao carregar página: " + e.getMessage());
        }
    }
}
