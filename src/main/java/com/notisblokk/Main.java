package com.notisblokk;

import com.notisblokk.config.AppConfig;
import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.config.ThymeleafConfig;
import com.notisblokk.controller.*;
import com.notisblokk.audiencias.controller.*;
import com.notisblokk.scheduler.QuartzSchedulerManager;
import com.notisblokk.middleware.AdminMiddleware;
import com.notisblokk.middleware.AuthMiddleware;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.notisblokk.audiencias.util.*;
import com.notisblokk.util.SessionUtil;
import java.time.LocalDate;
import java.time.LocalTime;
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

            // Registrar conversores personalizados para formato brasileiro (dd/MM/yyyy)
            SimpleModule brazilianDateModule = new SimpleModule("BrazilianDateModule");
            brazilianDateModule.addSerializer(LocalDate.class, new BrazilianLocalDateSerializer());
            brazilianDateModule.addDeserializer(LocalDate.class, new BrazilianLocalDateDeserializer());
            brazilianDateModule.addSerializer(LocalTime.class, new BrazilianLocalTimeSerializer());
            brazilianDateModule.addDeserializer(LocalTime.class, new BrazilianLocalTimeDeserializer());
            objectMapper.registerModule(brazilianDateModule);

            logger.info("Conversores de data brasileiros registrados (dd/MM/yyyy e HH:mm)");

            config.jsonMapper(Já executei mvn clean compilenew JavalinJackson(objectMapper, true));

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
        StatusNotaController statusController = new StatusNotaController();
        NotaController notaController = new NotaController();
        NotificacaoController notificacaoController = new NotificacaoController();
        NotasViewController notasViewController = new NotasViewController();
        PerfilController perfilController = new PerfilController();
        ConfiguracoesController configuracoesController = new ConfiguracoesController();
        BackupController backupController = new BackupController();
        AnexoController anexoController = new AnexoController();

        // Controllers do módulo de Audiências
        AudienciasViewController audienciasViewController = new AudienciasViewController();
        VaraController varaController = new VaraController();
        JuizController juizController = new JuizController();
        PromotorController promotorController = new PromotorController();
        AdvogadoController advogadoController = new AdvogadoController();
        PessoaController pessoaController = new PessoaController();
        ParticipacaoAudienciaController participacaoController = new ParticipacaoAudienciaController();
        AudienciaController audienciaController = new AudienciaController();
        PautaController pautaController = new PautaController();
        HorariosLivresController horariosLivresController = new HorariosLivresController();

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
        app.get("/api/status/debug", ctx -> { ... });
        app.get("/api/notas/debug", ctx -> { ... });
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

        // Middleware para rotas de audiências (requer autenticação)
        app.before("/audiencias", AuthMiddleware.require());
        app.before("/audiencias/*", AuthMiddleware.require());
        app.before("/api/audiencias", AuthMiddleware.require());
        app.before("/api/audiencias/*", AuthMiddleware.require());

        // Dashboard
        app.get("/dashboard", dashboardController::index);

        // API - Dashboard Stats (AJAX)
        app.get("/api/dashboard/stats", dashboardController::getStats);

        // Anotações (Views)
        app.get("/notas", notasViewController::index);
        app.get("/notas/nova", notasViewController::novaNota);
        app.get("/notas/editar/{id}", notasViewController::editarNota);

        // Audiências (Views)
        app.get("/audiencias", audienciasViewController::index);
        app.get("/audiencias/nova", audienciasViewController::novaAudiencia);
        app.get("/audiencias/editar/{id}", audienciasViewController::editarAudiencia);
        app.get("/audiencias/advogados", audienciasViewController::advogados);
        app.get("/audiencias/pessoas", audienciasViewController::pessoas);

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

        // ========== API DE AUDIÊNCIAS (AUTENTICAÇÃO NECESSÁRIA) ==========

        System.out.println("DEBUG_AUDIENCIAS: Registrando rotas do módulo de audiências...");

        // Varas
        app.get("/api/audiencias/varas", varaController::listar);
        app.get("/api/audiencias/varas/{id}", varaController::buscarPorId);
        app.get("/api/audiencias/varas/buscar", varaController::buscarPorNome);
        app.post("/api/audiencias/varas", varaController::criar);
        app.put("/api/audiencias/varas/{id}", varaController::atualizar);
        app.delete("/api/audiencias/varas/{id}", varaController::deletar);

        // Juízes
        app.get("/api/audiencias/juizes", juizController::listar);
        app.get("/api/audiencias/juizes/{id}", juizController::buscarPorId);
        app.get("/api/audiencias/juizes/buscar", juizController::buscarPorNome);
        app.post("/api/audiencias/juizes", juizController::criar);
        app.put("/api/audiencias/juizes/{id}", juizController::atualizar);
        app.delete("/api/audiencias/juizes/{id}", juizController::deletar);

        // Promotores
        app.get("/api/audiencias/promotores", promotorController::listar);
        app.get("/api/audiencias/promotores/{id}", promotorController::buscarPorId);
        app.get("/api/audiencias/promotores/buscar", promotorController::buscarPorNome);
        app.post("/api/audiencias/promotores", promotorController::criar);
        app.put("/api/audiencias/promotores/{id}", promotorController::atualizar);
        app.delete("/api/audiencias/promotores/{id}", promotorController::deletar);

        // Advogados
        app.get("/api/audiencias/advogados", advogadoController::listar);
        app.get("/api/audiencias/advogados/{id}", advogadoController::buscarPorId);
        app.get("/api/audiencias/advogados/buscar", advogadoController::buscarPorNome);
        app.get("/api/audiencias/advogados/buscar-oab", advogadoController::buscarPorOAB);
        app.post("/api/audiencias/advogados", advogadoController::criar);
        app.put("/api/audiencias/advogados/{id}", advogadoController::atualizar);
        app.delete("/api/audiencias/advogados/{id}", advogadoController::deletar);

        // Pessoas
        app.get("/api/audiencias/pessoas", pessoaController::listar);
        app.get("/api/audiencias/pessoas/{id}", pessoaController::buscarPorId);
        app.get("/api/audiencias/pessoas/buscar", pessoaController::buscarPorNome);
        app.get("/api/audiencias/pessoas/buscar-cpf", pessoaController::buscarPorCPF);
        app.post("/api/audiencias/pessoas", pessoaController::criar);
        app.put("/api/audiencias/pessoas/{id}", pessoaController::atualizar);
        app.delete("/api/audiencias/pessoas/{id}", pessoaController::deletar);

        // Participações
        app.get("/api/audiencias/participacoes/audiencia/{audienciaId}", participacaoController::listarPorAudiencia);
        app.get("/api/audiencias/participacoes/pessoa/{pessoaId}", participacaoController::listarPorPessoa);
        app.get("/api/audiencias/participacoes/{id}", participacaoController::buscarPorId);
        app.post("/api/audiencias/participacoes", participacaoController::criar);
        app.put("/api/audiencias/participacoes/{id}", participacaoController::atualizar);
        app.delete("/api/audiencias/participacoes/{id}", participacaoController::deletar);

        // Pauta do dia (rotas mais específicas primeiro)
        app.get("/api/audiencias/pauta/{data}/vara/{varaId}", pautaController::pautaPorDataEVara);
        app.get("/api/audiencias/pauta/vara/{varaId}", pautaController::pautaDeHojePorVara);
        app.get("/api/audiencias/pauta/{data}", pautaController::pautaPorData);
        app.get("/api/audiencias/pauta", pautaController::pautaDeHoje);

        // Horários Livres
        app.post("/api/audiencias/horarios-livres", horariosLivresController::buscarHorariosLivres);
        app.get("/api/audiencias/horarios-livres/rapido", horariosLivresController::buscarHorariosLivresRapido);

        // Audiências (rotas específicas antes da genérica com {id})
        app.get("/api/audiencias/conflitos", audienciaController::verificarConflitos);
        app.get("/api/audiencias/data/{data}", audienciaController::buscarPorData);
        app.get("/api/audiencias/vara/{varaId}", audienciaController::buscarPorVara);
        app.get("/api/audiencias", audienciaController::listar);
        app.get("/api/audiencias/{id}", audienciaController::buscarPorId);
        app.post("/api/audiencias", audienciaController::criar);
        app.put("/api/audiencias/{id}", audienciaController::atualizar);
        app.delete("/api/audiencias/{id}", audienciaController::deletar);

        System.out.println("DEBUG_AUDIENCIAS: 36 rotas de audiências registradas com sucesso!");

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
