package com.notisblokk.config;

import io.javalin.rendering.FileRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Configuração do Thymeleaf Template Engine.
 *
 * <p>Configura o Thymeleaf para renderizar templates HTML com suporte a:</p>
 * <ul>
 *   <li>Templates em resources/templates/</li>
 *   <li>Modo HTML5</li>
 *   <li>Cache desabilitado em desenvolvimento</li>
 *   <li>Encoding UTF-8</li>
 *   <li>Integração com Javalin</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class ThymeleafConfig {

    private static final Logger logger = LoggerFactory.getLogger(ThymeleafConfig.class);
    private static TemplateEngine templateEngine;

    /**
     * Inicializa o Thymeleaf Template Engine.
     *
     * @return TemplateEngine engine configurado
     */
    public static TemplateEngine getTemplateEngine() {
        if (templateEngine == null) {
            logger.info("Inicializando Thymeleaf Template Engine...");

            // Configurar o resolver
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("/templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode(TemplateMode.HTML);
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(false); // Desabilitar cache em desenvolvimento
            resolver.setCheckExistence(true);

            // Criar o template engine
            templateEngine = new TemplateEngine();
            templateEngine.setTemplateResolver(resolver);

            logger.info("Thymeleaf Template Engine inicializado com sucesso");
        }

        return templateEngine;
    }

    /**
     * Renderiza um template Thymeleaf com o modelo fornecido.
     *
     * @param ctx contexto do Javalin
     * @param templatePath caminho do template (relativo a templates/)
     * @param model modelo de dados para o template
     * @return String HTML renderizado
     */
    public static String render(io.javalin.http.Context ctx, String templatePath, Map<String, Object> model) {
        TemplateEngine engine = getTemplateEngine();

        // Criar contexto do Thymeleaf
        org.thymeleaf.context.Context thymeleafContext = new org.thymeleaf.context.Context();

        // Adicionar variáveis do modelo ao contexto
        if (model != null) {
            model.forEach(thymeleafContext::setVariable);
        }

        // Adicionar variáveis globais
        thymeleafContext.setVariable("appName", AppConfig.getAppName());
        thymeleafContext.setVariable("appVersion", AppConfig.getAppVersion());
        thymeleafContext.setVariable("contextPath", ctx.contextPath());

        // Renderizar template
        return engine.process(templatePath, thymeleafContext);
    }

    /**
     * Cria um FileRenderer personalizado para Javalin.
     *
     * @return FileRenderer renderer customizado
     */
    public static FileRenderer createJavalinRenderer() {
        return new FileRenderer() {
            @NotNull
            @Override
            public String render(@NotNull String filePath, @NotNull Map<String, Object> model, @NotNull io.javalin.http.Context ctx) {
                // Remover extensão .html se fornecida (Thymeleaf adiciona automaticamente)
                String templatePath = filePath.replace(".html", "");

                return ThymeleafConfig.render(ctx, templatePath, model);
            }
        };
    }

    /**
     * Limpa o cache de templates (útil para desenvolvimento).
     */
    public static void clearCache() {
        if (templateEngine != null) {
            logger.info("Limpando cache de templates Thymeleaf");
            templateEngine.clearTemplateCache();
        }
    }
}
