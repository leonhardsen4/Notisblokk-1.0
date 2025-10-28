package com.notisblokk.controller;

import com.notisblokk.model.User;
import com.notisblokk.service.ConfiguracaoService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller responsável pelas configurações do sistema.
 *
 * <p>Gerencia:</p>
 * <ul>
 *   <li>Preferências de tema</li>
 *   <li>Configurações de notificações</li>
 *   <li>Configurações de segurança</li>
 *   <li>Configurações de backup</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class ConfiguracoesController {

    private static final Logger logger = LoggerFactory.getLogger(ConfiguracoesController.class);
    private final ConfiguracaoService configuracaoService;

    public ConfiguracoesController() {
        this.configuracaoService = new ConfiguracaoService();
    }

    /**
     * GET /configuracoes
     * Exibe a página de configurações.
     */
    public void index(Context ctx) {
        User currentUser = SessionUtil.getCurrentUser(ctx);

        // Verificar se usuário está autenticado
        if (currentUser == null) {
            ctx.redirect("/auth/login");
            return;
        }

        Map<String, Object> model = new HashMap<>();

        // Adicionar atributos de sessão (inclui isAuthenticated, user, theme, etc)
        model.putAll(SessionUtil.getSessionAttributes(ctx));

        model.put("title", "Configurações - Notisblokk");
        model.put("user", currentUser);
        model.put("theme", SessionUtil.getTheme(ctx));

        // Limpar configurações obsoletas
        configuracaoService.limparConfiguracoesObsoletas(currentUser.getId());

        // Carregar configurações do usuário
        Map<String, String> config = configuracaoService.buscarConfiguracoes(currentUser.getId());
        model.put("config", config);

        // Mensagens
        String success = ctx.sessionAttribute("configSuccess");
        String error = ctx.sessionAttribute("configError");

        if (success != null) {
            model.put("success", success);
            ctx.sessionAttribute("configSuccess", null);
        }
        if (error != null) {
            model.put("error", error);
            ctx.sessionAttribute("configError", null);
        }

        ctx.contentType("text/html; charset=utf-8");
        ctx.render("configuracoes/index", model);
    }

    /**
     * POST /configuracoes/salvar
     * Salva as configurações do usuário.
     */
    public void salvar(Context ctx) {
        try {
            User currentUser = SessionUtil.getCurrentUser(ctx);

            // Verificar se usuário está autenticado
            if (currentUser == null) {
                ctx.redirect("/auth/login");
                return;
            }

            // Obter todas as configurações do formulário
            Map<String, String> configuracoes = new HashMap<>();

            // Notificações
            String notifEmail = ctx.formParam("notif_email");
            String notifToast = ctx.formParam("notif_toast");
            String notifDiasCritico = ctx.formParam("notif_dias_critico");
            String notifDiasUrgente = ctx.formParam("notif_dias_urgente");
            String notifDiasAtencao = ctx.formParam("notif_dias_atencao");

            configuracoes.put("notif_email", notifEmail != null ? "true" : "false");
            configuracoes.put("notif_toast", notifToast != null ? "true" : "false");
            configuracoes.put("notif_dias_critico", notifDiasCritico != null ? notifDiasCritico : "0");
            configuracoes.put("notif_dias_urgente", notifDiasUrgente != null ? notifDiasUrgente : "3");
            configuracoes.put("notif_dias_atencao", notifDiasAtencao != null ? notifDiasAtencao : "5");

            // Segurança
            String senhaExpiraMeses = ctx.formParam("senha_expira_meses");
            String senhaAvisoAntecedencia = ctx.formParam("senha_aviso_antecedencia");

            configuracoes.put("senha_expira_meses", senhaExpiraMeses != null ? senhaExpiraMeses : "3");
            configuracoes.put("senha_aviso_antecedencia", senhaAvisoAntecedencia != null ? senhaAvisoAntecedencia : "10");

            // Backup
            String backupAuto = ctx.formParam("backup_auto");
            String backupPeriodicidade = ctx.formParam("backup_periodicidade");

            configuracoes.put("backup_auto", backupAuto != null ? "true" : "false");
            configuracoes.put("backup_periodicidade", backupPeriodicidade != null ? backupPeriodicidade : "7");

            // Salvar no banco
            configuracaoService.salvarConfiguracoes(currentUser.getId(), configuracoes);

            logger.info("Configurações salvas com sucesso para usuário: {}", currentUser.getUsername());

            ctx.sessionAttribute("configSuccess", "Configurações salvas com sucesso!");
            ctx.redirect("/configuracoes");

        } catch (Exception e) {
            logger.error("Erro ao salvar configurações", e);
            ctx.sessionAttribute("configError", "Erro ao salvar configurações: " + e.getMessage());
            ctx.redirect("/configuracoes");
        }
    }

    /**
     * POST /configuracoes/resetar
     * Restaura configurações padrão.
     */
    public void resetar(Context ctx) {
        try {
            User currentUser = SessionUtil.getCurrentUser(ctx);

            // Verificar se usuário está autenticado
            if (currentUser == null) {
                ctx.redirect("/auth/login");
                return;
            }

            configuracaoService.resetarConfiguracoes(currentUser.getId());

            logger.info("Configurações resetadas para usuário: {}", currentUser.getUsername());

            // Resetar tema na sessão
            ctx.sessionAttribute("theme", "light");

            ctx.sessionAttribute("configSuccess", "Configurações restauradas para o padrão!");
            ctx.redirect("/configuracoes");

        } catch (Exception e) {
            logger.error("Erro ao resetar configurações", e);
            ctx.sessionAttribute("configError", "Erro ao resetar configurações: " + e.getMessage());
            ctx.redirect("/configuracoes");
        }
    }

    /**
     * GET /api/configuracoes
     * Retorna as configurações do usuário logado via API.
     */
    public void buscarConfiguracoesAPI(Context ctx) {
        try {
            User currentUser = SessionUtil.getCurrentUser(ctx);
            if (currentUser == null) {
                ctx.json(java.util.Map.of(
                    "success", false,
                    "message", "Usuário não autenticado"
                ));
                return;
            }

            java.util.Map<String, String> config = configuracaoService.buscarConfiguracoes(currentUser.getId());

            ctx.json(java.util.Map.of(
                "success", true,
                "dados", config
            ));

            logger.debug("Configurações retornadas via API para usuário: {}", currentUser.getUsername());

        } catch (Exception e) {
            logger.error("Erro ao buscar configurações via API", e);
            ctx.status(500);
            ctx.json(java.util.Map.of(
                "success", false,
                "message", "Erro ao buscar configurações: " + e.getMessage()
            ));
        }
    }
}
