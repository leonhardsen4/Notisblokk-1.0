package com.notisblokk;

import com.notisblokk.config.AppConfig;
import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.config.ThymeleafConfig;
import com.notisblokk.controller.AuthController;
import com.notisblokk.controller.DashboardController;
import com.notisblokk.controller.UserController;
import com.notisblokk.middleware.AdminMiddleware;
import com.notisblokk.middleware.AuthMiddleware;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe principal da aplicação Notisblokk 1.0.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Inicializar o banco de dados</li>
 *   <li>Configurar o servidor Javalin</li>
 *   <li>Registrar todas as rotas</li>
 *   <li>Configurar middlewares</li>
 *   <li>Configurar tratamento de erros</li>
 *   <li>Iniciar o servidor</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("  NOTISBLOKK 1.0");
        logger.info("  Sistema de Gerenciamento");
        logger.info("========================================");

        try {
            // Imprimir configurações
            AppConfig.printConfig();

            // Inicializar banco de dados
            logger.info("Inicializando banco de dados...");
            DatabaseConfig.initialize();

            // Criar aplicação Javalin
            Javalin app = createApp();

            // Configurar rotas
            configureRoutes(app);

            // Registrar shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Encerrando aplicação...");
                DatabaseConfig.close();
                app.stop();
                logger.info("Aplicação encerrada");
            }));

            // Iniciar servidor
            int port = AppConfig.getServerPort();
            app.start(port);

            logger.info("========================================");
            logger.info("  Servidor iniciado com sucesso!");
            logger.info("  URL: http://{}:{}", AppConfig.getServerHost(), port);
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("Erro fatal ao iniciar aplicação", e);
            System.exit(1);
        }
    }

    /**
     * Cria e configura a aplicação Javalin.
     *
     * @return Javalin aplicação configurada
     */
    private static Javalin createApp() {
        return Javalin.create(config -> {
            // Arquivos estáticos
            config.staticFiles.add("/public", Location.CLASSPATH);

            // Configurar Thymeleaf como template engine
            config.fileRenderer(ThymeleafConfig.createJavalinRenderer());

            // Configurar CORS (para desenvolvimento - restringir em produção)
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });

            // Habilitar logs de requisições
            config.bundledPlugins.enableDevLogging();

            // Configurar tamanho máximo de requisições (5MB)
            config.http.maxRequestSize = 5 * 1024 * 1024L;

            // Session configuration
            config.jetty.modifyServletContextHandler(handler -> {
                handler.getSessionHandler().setMaxInactiveInterval(
                    AppConfig.getSessionTimeoutMinutes() * 60
                );
            });

        });
    }

    /**
     * Configura todas as rotas da aplicação.
     *
     * @param app instância do Javalin
     */
    private static void configureRoutes(Javalin app) {
        logger.info("Configurando rotas...");

        // Criar instâncias dos controllers
        AuthController authController = new AuthController();
        DashboardController dashboardController = new DashboardController();
        UserController userController = new UserController();

        // ========== ROTAS PÚBLICAS ==========

        // Raiz - redireciona para login ou dashboard
        app.get("/", ctx -> {
            if (com.notisblokk.util.SessionUtil.isAuthenticated(ctx)) {
                ctx.redirect("/dashboard");
            } else {
                ctx.redirect("/auth/login");
            }
        });

        // ========== AUTENTICAÇÃO ==========

        app.get("/auth/login", authController::showLogin);
        app.post("/auth/login", authController::processLogin);

        app.get("/auth/register", authController::showRegister);
        app.post("/auth/register", authController::processRegister);

        app.get("/auth/recover-password", authController::showRecoverPassword);
        app.post("/auth/recover-password", authController::processRecoverPassword);

        app.get("/auth/logout", authController::logout);

        // ========== ROTAS PROTEGIDAS (AUTENTICAÇÃO NECESSÁRIA) ==========

        // Dashboard
        app.get("/dashboard", AuthMiddleware.require(), dashboardController::index);

        // API - Dashboard Stats (AJAX)
        app.get("/api/dashboard/stats", AuthMiddleware.require(), dashboardController::getStats);

        // ========== ROTAS ADMINISTRATIVAS (APENAS ADMIN) ==========

        // Usuários
        app.get("/admin/users", AuthMiddleware.require(), AdminMiddleware.require(), userController::list);
        app.post("/admin/users", AuthMiddleware.require(), AdminMiddleware.require(), userController::create);
        app.put("/admin/users/{id}", AuthMiddleware.require(), AdminMiddleware.require(), userController::update);
        app.patch("/admin/users/{id}/toggle", AuthMiddleware.require(), AdminMiddleware.require(), userController::toggleStatus);
        app.delete("/admin/users/{id}", AuthMiddleware.require(), AdminMiddleware.require(), userController::delete);

        // API - Usuários (JSON)
        app.get("/api/users", AuthMiddleware.require(), AdminMiddleware.require(), userController::listJson);
        app.get("/api/users/{id}", AuthMiddleware.require(), AdminMiddleware.require(), userController::getUser);

        // ========== TRATAMENTO DE ERROS ==========

        // 404 - Página não encontrada
        app.error(404, ctx -> {
            logger.warn("Página não encontrada: {} (IP: {})",
                       ctx.path(), com.notisblokk.util.SessionUtil.getClientIp(ctx));

            if (ctx.path().startsWith("/api/")) {
                ctx.json(java.util.Map.of(
                    "success", false,
                    "message", "Endpoint não encontrado"
                ));
            } else {
                ctx.status(404);
                ctx.result("Página não encontrada - 404");
            }
        });

        // 500 - Erro interno do servidor
        app.error(500, ctx -> {
            logger.error("Erro interno do servidor: {} (IP: {})",
                        ctx.path(), com.notisblokk.util.SessionUtil.getClientIp(ctx));

            if (ctx.path().startsWith("/api/")) {
                ctx.json(java.util.Map.of(
                    "success", false,
                    "message", "Erro interno do servidor"
                ));
            } else {
                ctx.status(500);
                ctx.result("Erro interno do servidor - 500");
            }
        });

        // Exception handler global
        app.exception(Exception.class, (e, ctx) -> {
            logger.error("Exceção não tratada na rota: {}", ctx.path(), e);
            ctx.status(500);

            if (ctx.path().startsWith("/api/")) {
                ctx.json(java.util.Map.of(
                    "success", false,
                    "message", "Erro: " + e.getMessage()
                ));
            } else {
                ctx.result("Erro ao processar requisição: " + e.getMessage());
            }
        });

        logger.info("Rotas configuradas com sucesso");
    }
}
