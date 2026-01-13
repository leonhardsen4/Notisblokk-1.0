package com.notisblokk.controller;

import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller responsável pela visualização da página da calculadora.
 *
 * <p>Renderiza a interface HTML da calculadora com histórico de cálculos.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class CalculadoraViewController {

    private static final Logger logger = LoggerFactory.getLogger(CalculadoraViewController.class);

    /**
     * GET /calculadora
     * Exibe a página da calculadora.
     */
    public void index(Context ctx) {
        try {
            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");

            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Calculadora - Notisblokk");

            logger.debug("Página da calculadora acessada por: {}", SessionUtil.getCurrentUserDisplayName(ctx));

            ctx.render("calculadora/index", model);

        } catch (Exception e) {
            logger.error("Erro ao exibir página da calculadora", e);
            ctx.status(500);
            ctx.result("Erro ao carregar calculadora: " + e.getMessage());
        }
    }
}
