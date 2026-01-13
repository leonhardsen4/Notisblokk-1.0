package com.notisblokk.controller;

import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller responsável pela visualização da página do bloco de notas.
 *
 * <p>Renderiza a interface HTML do bloco de notas com editor Markdown.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class BlocoNotaViewController {

    private static final Logger logger = LoggerFactory.getLogger(BlocoNotaViewController.class);

    /**
     * GET /bloco-notas
     * Exibe a página do bloco de notas.
     */
    public void index(Context ctx) {
        try {
            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Bloco de Notas - Notisblokk");

            logger.debug("Página do bloco de notas acessada por: {}", SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("bloco-notas/index", model);

        } catch (Exception e) {
            logger.error("Erro ao exibir página do bloco de notas", e);
            ctx.status(500);
            ctx.result("Erro ao carregar bloco de notas: " + e.getMessage());
        }
    }
}
