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

            config.jsonMapper(new JavalinJackson(objectMapper, true));

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
        SessionController sessionController = new SessionController();
        EtiquetaController etiquetaController = new EtiquetaController();
        StatusTarefaController statusTarefaController = new StatusTarefaController();
        TarefaController tarefaController = new TarefaController();
        NotificacaoController notificacaoController = new NotificacaoController();
        TarefasViewController tarefasViewController = new TarefasViewController();
        PerfilController perfilController = new PerfilController();
        ConfiguracoesController configuracoesController = new ConfiguracoesController();
        BackupController backupController = new BackupController();
        AnexoController anexoController = new AnexoController();

        // Ferramentas (Calculadora e Bloco de Notas)
        CalculadoraController calculadoraController = new CalculadoraController();
        CalculadoraViewController calculadoraViewController = new CalculadoraViewController();
        BlocoNotaController blocoNotaController = new BlocoNotaController();
        BlocoNotaViewController blocoNotaViewController = new BlocoNotaViewController();

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

        // ========== ROTAS DE DEBUG (REMOVIDAS POR SEGURANÇA) ==========
        // As rotas de debug foram removidas pois expunham dados sensíveis do banco de dados
        // sem autenticação. Se necessário para desenvolvimento, adicione AuthMiddleware e
        // AdminMiddleware antes de habilitar novamente.

        /*
        // DESABILITADO: Rota de debug expõe dados do banco sem autenticação
        app.get("/api/status-tarefa/debug", ctx -> { ... });
        app.get("/api/tarefas/debug", ctx -> { ... });
        */

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
        app.before("/api/sessions/*", AuthMiddleware.require());
        app.before("/api/sessions/*", AdminMiddleware.require());

        // Middleware para rotas de tarefas (requer autenticação)
        app.before("/tarefas", AuthMiddleware.require());
        app.before("/tarefas/*", AuthMiddleware.require());
        app.before("/api/etiquetas", AuthMiddleware.require());
        app.before("/api/etiquetas/*", AuthMiddleware.require());
        app.before("/api/status-tarefa", AuthMiddleware.require());
        app.before("/api/status-tarefa/*", AuthMiddleware.require());
        app.before("/api/tarefas", AuthMiddleware.require());
        app.before("/api/tarefas/*", AuthMiddleware.require());
        app.before("/api/notificacoes/*", AuthMiddleware.require());
        app.before("/perfil", AuthMiddleware.require());
        app.before("/perfil/*", AuthMiddleware.require());
        app.before("/configuracoes", AuthMiddleware.require());
        app.before("/configuracoes/*", AuthMiddleware.require());
        app.before("/api/configuracoes", AuthMiddleware.require());
        app.before("/api/configuracoes/*", AuthMiddleware.require());
        app.before("/backup", AuthMiddleware.require());
        app.before("/backup", AdminMiddleware.require());
        app.before("/api/backup/*", AuthMiddleware.require());
        app.before("/api/backup/*", AdminMiddleware.require());
        app.before("/api/anexos/*", AuthMiddleware.require());
        app.before("/uploads/*", AuthMiddleware.require());
        app.before("/api/theme", AuthMiddleware.require());

        // Middleware para ferramentas (requer autenticação)
        app.before("/calculadora", AuthMiddleware.require());
        app.before("/calculadora/*", AuthMiddleware.require());
        app.before("/api/calculadora/*", AuthMiddleware.require());
        app.before("/bloco-notas", AuthMiddleware.require());
        app.before("/bloco-notas/*", AuthMiddleware.require());
        app.before("/api/bloco-notas/*", AuthMiddleware.require());

        // Dashboard
        app.get("/dashboard", dashboardController::index);

        // API - Dashboard Stats (AJAX)
        app.get("/api/dashboard/stats", dashboardController::getStats);

        // Tarefas (Views)
        app.get("/tarefas", tarefasViewController::index);
        app.get("/tarefas/nova", tarefasViewController::novaTarefa);
        app.get("/tarefas/editar/{id}", tarefasViewController::editarTarefa);

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

        // Sessões
        app.get("/admin/sessions", sessionController::index);

        // API - Sessões (JSON)
        app.get("/api/sessions/listar", sessionController::listar);
        app.get("/api/sessions/stats", sessionController::obterEstatisticas);
        app.post("/api/sessions/{id}/encerrar", sessionController::encerrar);

        // ========== API DE TAREFAS (AUTENTICAÇÃO NECESSÁRIA) ==========

        // Etiquetas
        app.get("/api/etiquetas", etiquetaController::listar);
        app.get("/api/etiquetas/{id}", etiquetaController::buscarPorId);
        app.post("/api/etiquetas", etiquetaController::criar);
        app.put("/api/etiquetas/{id}", etiquetaController::atualizar);
        app.delete("/api/etiquetas/{id}", etiquetaController::deletar);

        // Status de Tarefas
        app.get("/api/status-tarefa", statusTarefaController::listar);
        app.get("/api/status-tarefa/{id}", statusTarefaController::buscarPorId);
        app.post("/api/status-tarefa", statusTarefaController::criar);
        app.put("/api/status-tarefa/{id}", statusTarefaController::atualizar);
        app.delete("/api/status-tarefa/{id}", statusTarefaController::deletar);

        // Tarefas
        app.get("/api/tarefas", tarefaController::listar); // Suporta paginação via query params
        app.get("/api/tarefas/paginado", tarefaController::listarPaginado); // Endpoint dedicado para paginação
        app.get("/api/tarefas/buscar", tarefaController::buscarPorTexto); // Busca por texto (query param: q)
        app.get("/api/tarefas/intervalo", tarefaController::buscarPorIntervaloPrazo); // Busca por intervalo de datas (query params: inicio, fim)
        app.get("/api/tarefas/{id}", tarefaController::buscarPorId);
        app.get("/api/tarefas/etiqueta/{etiquetaId}", tarefaController::buscarPorEtiqueta);
        app.post("/api/tarefas", tarefaController::criar);
        app.put("/api/tarefas/{id}", tarefaController::atualizar);
        app.delete("/api/tarefas/{id}", tarefaController::deletar);

        // PDF de Tarefas
        app.get("/api/tarefas/{id}/pdf", tarefaController::gerarPDF);
        app.post("/api/tarefas/pdf/relatorio", tarefaController::gerarPDFRelatorio);

        // Notificações
        app.get("/api/notificacoes/alertas", notificacaoController::gerarAlertas);
        app.get("/api/notificacoes/estatisticas", notificacaoController::obterEstatisticas);

        // ========== FERRAMENTAS (CALCULADORA E BLOCO DE NOTAS) ==========

        // Calculadora - Views
        app.get("/calculadora", calculadoraViewController::index);

        // Calculadora - API
        app.get("/api/calculadora/historico", calculadoraController::obterHistorico);
        app.post("/api/calculadora/calcular", calculadoraController::calcular);
        app.delete("/api/calculadora/historico", calculadoraController::limparHistorico);
        app.delete("/api/calculadora/historico/{id}", calculadoraController::deletarItem);

        // Bloco de Notas - Views
        app.get("/bloco-notas", blocoNotaViewController::index);

        // Bloco de Notas - API
        app.get("/api/bloco-notas", blocoNotaController::obter);
        app.post("/api/bloco-notas/salvar", blocoNotaController::salvar);
        app.get("/api/bloco-notas/exportar/txt", blocoNotaController::exportarTxt);
        app.get("/api/bloco-notas/exportar/md", blocoNotaController::exportarMarkdown);

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
        app.post("/api/tarefas/{tarefaId}/anexos", anexoController::upload);
        app.get("/api/tarefas/{tarefaId}/anexos", anexoController::listar);
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
