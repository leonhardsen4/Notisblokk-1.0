package com.notisblokk;

import com.notisblokk.config.AppConfig;
import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.config.ThymeleafConfig;
import com.notisblokk.controller.*;
import com.notisblokk.scheduler.QuartzSchedulerManager;
import com.notisblokk.middleware.AdminMiddleware;
import com.notisblokk.middleware.AuthMiddleware;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.notisblokk.util.SessionUtil;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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

            // Inicializar Quartz Scheduler para alertas por email
            logger.info("Inicializando Quartz Scheduler...");
            QuartzSchedulerManager schedulerManager = new QuartzSchedulerManager();
            schedulerManager.iniciar();

            // Criar aplicação Javalin
            Javalin app = createApp();

            // Configurar rotas
            configureRoutes(app);

            // Registrar shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Encerrando aplicação...");
                schedulerManager.parar();
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
        // Criar pastas necessárias se não existirem
        java.io.File uploadsDir = new java.io.File("uploads");
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
            logger.info("Pasta de uploads criada: {}", uploadsDir.getAbsolutePath());
        }

        java.io.File backupsDir = new java.io.File("backups");
        if (!backupsDir.exists()) {
            backupsDir.mkdirs();
            logger.info("Pasta de backups criada: {}", backupsDir.getAbsolutePath());
        }

        return Javalin.create(config -> {
            // Arquivos estáticos do classpath (CSS, JS, etc)
            config.staticFiles.add("/public", Location.CLASSPATH);

            // Arquivos de uploads (fotos de perfil, anexos, etc)
            // Serve arquivos de "uploads/" com prefixo "/uploads/" na URL
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/uploads";
                staticFiles.directory = "uploads";
                staticFiles.location = Location.EXTERNAL;
            });

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
        PerfilController perfilController = new PerfilController();
        ConfiguracoesController configuracoesController = new ConfiguracoesController();
        BackupController backupController = new BackupController();
        AnexoController anexoController = new AnexoController();

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

        app.get("/auth/verificar-email", authController::verificarEmail);

        app.get("/auth/nova-senha", authController::showNovaSenha);
        app.post("/auth/nova-senha", authController::processNovaSenha);

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
        app.before("/perfil", AuthMiddleware.require());
        app.before("/perfil/*", AuthMiddleware.require());
        app.before("/backup", AuthMiddleware.require());
        app.before("/api/backup/*", AuthMiddleware.require());
        app.before("/api/anexos/*", AuthMiddleware.require());

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
        app.get("/api/notas", notaController::listar); // Suporta paginação via query params
        app.get("/api/notas/paginado", notaController::listarPaginado); // Endpoint dedicado para paginação
        app.get("/api/notas/{id}", notaController::buscarPorId);
        app.get("/api/notas/etiqueta/{etiquetaId}", notaController::buscarPorEtiqueta);
        app.post("/api/notas", notaController::criar);
        app.put("/api/notas/{id}", notaController::atualizar);
        app.delete("/api/notas/{id}", notaController::deletar);

        // PDF de Notas
        app.get("/api/notas/{id}/pdf", notaController::gerarPDF);
        app.post("/api/notas/pdf/relatorio", notaController::gerarPDFRelatorio);

        // Notificações
        app.get("/api/notificacoes/alertas", notificacaoController::gerarAlertas);
        app.get("/api/notificacoes/estatisticas", notificacaoController::obterEstatisticas);

        // ========== PERFIL DO USUÁRIO ==========

        app.get("/perfil", perfilController::index);
        app.post("/perfil/senha", perfilController::alterarSenha);
        app.post("/perfil/email", perfilController::alterarEmail);
        app.post("/perfil/foto", perfilController::uploadFoto);
        app.post("/perfil/foto/remover", perfilController::removerFoto);

        // ========== CONFIGURAÇÕES ==========

        app.get("/configuracoes", configuracoesController::index);
        app.post("/configuracoes/salvar", configuracoesController::salvar);
        app.post("/configuracoes/resetar", configuracoesController::resetar);

        // Configurações - API
        app.get("/api/configuracoes", configuracoesController::buscarConfiguracoesAPI);

        // ========== BACKUP E ANEXOS ==========

        // Backup - Página
        app.get("/backup", backupController::index);

        // Backup - API
        app.post("/api/backup/manual", backupController::criarBackupManual);
        app.post("/api/backup/csv", backupController::exportarCSV);
        app.get("/api/backup/listar", backupController::listarBackups);
        app.get("/api/backup/download/{id}", backupController::downloadBackup);

        // Anexos
        app.post("/api/notas/{notaId}/anexos", anexoController::upload);
        app.get("/api/notas/{notaId}/anexos", anexoController::listar);
        app.get("/api/anexos/{id}/download", anexoController::download);
        app.get("/api/anexos/{id}/visualizar", anexoController::visualizar);
        app.delete("/api/anexos/{id}", anexoController::remover);

        // ========== SERVIR ARQUIVOS DE UPLOAD ==========

        // Rota para servir arquivos de upload (fotos de perfil, anexos, etc)
        app.get("/uploads/*", ctx -> {
            String path = ctx.path().replace("/uploads/", "");
            java.io.File file = new java.io.File("uploads", path);

            if (!file.exists() || !file.isFile()) {
                ctx.status(404);
                ctx.result("Arquivo não encontrado");
                logger.warn("Arquivo de upload não encontrado: {}", file.getAbsolutePath());
                return;
            }

            // Determinar tipo de conteúdo
            String contentType = "application/octet-stream";
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".png")) contentType = "image/png";
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (fileName.endsWith(".gif")) contentType = "image/gif";
            else if (fileName.endsWith(".webp")) contentType = "image/webp";
            else if (fileName.endsWith(".pdf")) contentType = "application/pdf";

            ctx.contentType(contentType);
            ctx.result(new java.io.FileInputStream(file));
            logger.debug("Servindo arquivo de upload: {}", file.getAbsolutePath());
        });

        // ========== PREFERÊNCIAS DO USUÁRIO ==========

        // Salvar tema na sessão
        app.post("/api/theme", ctx -> {
            try {
                String theme = ctx.formParam("theme");

                if (theme == null || (!theme.equals("light") && !theme.equals("dark"))) {
                    ctx.status(400);
                    ctx.json(Map.of(
                        "success", false,
                        "message", "Tema inválido. Use 'light' ou 'dark'."
                    ));
                    return;
                }

                SessionUtil.setTheme(ctx, theme);
                logger.info("Tema alterado para: {}", theme);

                ctx.json(Map.of(
                    "success", true,
                    "theme", theme
                ));
            } catch (Exception e) {
                logger.error("Erro ao salvar tema", e);
                ctx.status(500);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Erro ao salvar tema: " + e.getMessage()
                ));
            }
        });

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
