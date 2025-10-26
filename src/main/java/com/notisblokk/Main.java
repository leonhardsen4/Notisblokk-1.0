package com.notisblokk;

import com.notisblokk.config.AppConfig;
import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.config.ThymeleafConfig;
import com.notisblokk.controller.AuthController;
import com.notisblokk.controller.DashboardController;
import com.notisblokk.controller.UserController;
import com.notisblokk.controller.EtiquetaController;
import com.notisblokk.controller.StatusNotaController;
import com.notisblokk.controller.NotaController;
import com.notisblokk.controller.NotificacaoController;
import com.notisblokk.controller.NotasViewController;
import com.notisblokk.middleware.AdminMiddleware;
import com.notisblokk.middleware.AuthMiddleware;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.bundled.CorsPluginConfig;
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
            String host = AppConfig.getServerHost();
            int port = AppConfig.getServerPort();
            app.start(host, port);

            logger.info("========================================");
            logger.info("  Servidor iniciado com sucesso!");
            logger.info("  Escutando em: {}:{}", host, port);
            logger.info("  Acesso local: http://localhost:{}", port);
            logger.info("  Acesso em rede: http://<seu-ip>:{}", port);
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
                cors.addRule(CorsPluginConfig.CorsRule::anyHost);
            });

            // Configurar Jackson para serializar LocalDateTime corretamente
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            config.jsonMapper(new JavalinJackson());

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
        EtiquetaController etiquetaController = new EtiquetaController();
        StatusNotaController statusController = new StatusNotaController();
        NotaController notaController = new NotaController();
        NotificacaoController notificacaoController = new NotificacaoController();
        NotasViewController notasViewController = new NotasViewController();

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

        // ========== ROTAS DE DEBUG (SEM AUTENTICAÇÃO) ==========
        app.get("/api/status/debug", ctx -> {
            try {
                java.sql.Connection conn = com.notisblokk.config.DatabaseConfig.getConnection();
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery("SELECT id, nome, cor_hex FROM status_nota ORDER BY nome ASC");

                java.util.List<java.util.Map<String, Object>> statusList = new java.util.ArrayList<>();
                while (rs.next()) {
                    java.util.Map<String, Object> status = new java.util.HashMap<>();
                    status.put("id", rs.getLong("id"));
                    status.put("nome", rs.getString("nome"));
                    status.put("corHex", rs.getString("cor_hex"));
                    statusList.add(status);
                }

                rs.close();
                stmt.close();
                conn.close();

                logger.info("DEBUG: {} status encontrados no banco", statusList.size());

                ctx.json(java.util.Map.of(
                    "success", true,
                    "total", statusList.size(),
                    "dados", statusList
                ));
            } catch (Exception e) {
                logger.error("DEBUG: Erro ao buscar status", e);
                ctx.json(java.util.Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
            }
        });

        app.get("/api/notas/debug", ctx -> {
            try {
                java.sql.Connection conn = com.notisblokk.config.DatabaseConfig.getConnection();
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery(
                    "SELECT n.id, n.titulo, n.conteudo, n.prazo_final, n.data_criacao, " +
                    "e.id as etiqueta_id, e.nome as etiqueta_nome, " +
                    "s.id as status_id, s.nome as status_nome, s.cor_hex as status_cor " +
                    "FROM notas n " +
                    "LEFT JOIN etiquetas e ON n.etiqueta_id = e.id " +
                    "LEFT JOIN status_nota s ON n.status_id = s.id " +
                    "ORDER BY n.prazo_final ASC"
                );

                java.util.List<java.util.Map<String, Object>> notas = new java.util.ArrayList<>();
                while (rs.next()) {
                    java.util.Map<String, Object> nota = new java.util.HashMap<>();
                    nota.put("id", rs.getLong("id"));
                    nota.put("titulo", rs.getString("titulo"));
                    nota.put("conteudo", rs.getString("conteudo"));
                    nota.put("prazoFinal", rs.getString("prazo_final"));
                    nota.put("dataCriacao", rs.getString("data_criacao"));

                    java.util.Map<String, Object> etiqueta = new java.util.HashMap<>();
                    etiqueta.put("id", rs.getLong("etiqueta_id"));
                    etiqueta.put("nome", rs.getString("etiqueta_nome"));
                    nota.put("etiqueta", etiqueta);

                    java.util.Map<String, Object> status = new java.util.HashMap<>();
                    status.put("id", rs.getLong("status_id"));
                    status.put("nome", rs.getString("status_nome"));
                    status.put("corHex", rs.getString("status_cor"));
                    nota.put("status", status);

                    notas.add(nota);
                }

                rs.close();
                stmt.close();
                conn.close();

                logger.info("DEBUG: {} notas encontradas no banco", notas.size());

                ctx.json(java.util.Map.of(
                    "success", true,
                    "total", notas.size(),
                    "dados", notas
                ));
            } catch (Exception e) {
                logger.error("DEBUG: Erro ao buscar notas", e);
                ctx.json(java.util.Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "stackTrace", e.getStackTrace()[0].toString()
                ));
            }
        });

        // ========== ROTAS PROTEGIDAS (AUTENTICAÇÃO NECESSÁRIA) ==========

        // Middleware para rotas protegidas
        app.before("/dashboard", AuthMiddleware.require());
        app.before("/api/dashboard/*", AuthMiddleware.require());
        app.before("/admin/*", AuthMiddleware.require());
        app.before("/admin/*", AdminMiddleware.require());
        app.before("/api/users", AuthMiddleware.require());
        app.before("/api/users/*", AuthMiddleware.require());
        app.before("/api/users", AdminMiddleware.require());
        app.before("/api/users/*", AdminMiddleware.require());

        // Middleware para rotas de anotações (requer autenticação)
        app.before("/notas", AuthMiddleware.require());
        app.before("/notas/*", AuthMiddleware.require());
        app.before("/api/etiquetas", AuthMiddleware.require());
        app.before("/api/etiquetas/*", AuthMiddleware.require());
        app.before("/api/status", AuthMiddleware.require());
        app.before("/api/status/*", AuthMiddleware.require());
        app.before("/api/notas", AuthMiddleware.require());
        app.before("/api/notas/*", AuthMiddleware.require());
        app.before("/api/notificacoes/*", AuthMiddleware.require());

        // Dashboard
        app.get("/dashboard", dashboardController::index);

        // API - Dashboard Stats (AJAX)
        app.get("/api/dashboard/stats", dashboardController::getStats);

        // Anotações (Views)
        app.get("/notas", notasViewController::index);
        app.get("/notas/nova", notasViewController::novaNota);
        app.get("/notas/editar/{id}", notasViewController::editarNota);

        // ========== ROTAS ADMINISTRATIVAS (APENAS ADMIN) ==========

        // Usuários
        app.get("/admin/users", userController::list);
        app.post("/admin/users", userController::create);
        app.put("/admin/users/{id}", userController::update);
        app.patch("/admin/users/{id}/toggle", userController::toggleStatus);
        app.delete("/admin/users/{id}", userController::delete);

        // API - Usuários (JSON)
        app.get("/api/users", userController::listJson);
        app.get("/api/users/{id}", userController::getUser);

        // ========== API DE ANOTAÇÕES (AUTENTICAÇÃO NECESSÁRIA) ==========

        // Etiquetas
        app.get("/api/etiquetas", etiquetaController::listar);
        app.get("/api/etiquetas/{id}", etiquetaController::buscarPorId);
        app.post("/api/etiquetas", etiquetaController::criar);
        app.put("/api/etiquetas/{id}", etiquetaController::atualizar);
        app.delete("/api/etiquetas/{id}", etiquetaController::deletar);

        // Status
        app.get("/api/status", statusController::listar);
        app.get("/api/status/{id}", statusController::buscarPorId);
        app.post("/api/status", statusController::criar);
        app.put("/api/status/{id}", statusController::atualizar);
        app.delete("/api/status/{id}", statusController::deletar);

        // Notas
        app.get("/api/notas", notaController::listar);
        app.get("/api/notas/{id}", notaController::buscarPorId);
        app.get("/api/notas/etiqueta/{etiquetaId}", notaController::buscarPorEtiqueta);
        app.post("/api/notas", notaController::criar);
        app.put("/api/notas/{id}", notaController::atualizar);
        app.delete("/api/notas/{id}", notaController::deletar);

        // Notificações
        app.get("/api/notificacoes/alertas", notificacaoController::gerarAlertas);
        app.get("/api/notificacoes/estatisticas", notificacaoController::obterEstatisticas);

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
