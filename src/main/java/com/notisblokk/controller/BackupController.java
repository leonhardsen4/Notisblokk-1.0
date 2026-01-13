package com.notisblokk.controller;

import com.notisblokk.model.User;
import com.notisblokk.service.BackupService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller responsável pelo gerenciamento de backups.
 *
 * <p>Gerencia:</p>
 * <ul>
 *   <li>Criação de backup manual do banco de dados</li>
 *   <li>Exportação de tarefas para CSV</li>
 *   <li>Listagem de backups existentes</li>
 *   <li>Download de backups</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class BackupController {

    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);
    private final BackupService backupService;

    public BackupController() {
        this.backupService = new BackupService();
    }

    /**
     * GET /backup
     * Exibe a página de gerenciamento de backups.
     */
    public void index(Context ctx) {
        Map<String, Object> model = new HashMap<>();

        // Adicionar atributos de sessão
        model.putAll(SessionUtil.getSessionAttributes(ctx));

        model.put("title", "Backup e Exportação - Notisblokk");

        // Mensagens
        String success = ctx.sessionAttribute("backupSuccess");
        String error = ctx.sessionAttribute("backupError");

        if (success != null) {
            model.put("success", success);
            ctx.sessionAttribute("backupSuccess", null);
        }
        if (error != null) {
            model.put("error", error);
            ctx.sessionAttribute("backupError", null);
        }

        ctx.contentType("text/html; charset=utf-8");
        ctx.render("backup/index", model);
    }

    /**
     * POST /api/backup/manual
     * Cria um backup manual do banco de dados.
     */
    public void criarBackupManual(Context ctx) {
        try {
            User currentUser = SessionUtil.getCurrentUser(ctx);

            String caminhoBackup = backupService.criarBackup(currentUser.getId(), "MANUAL");

            logger.info("Backup manual criado por {}: {}", currentUser.getUsername(), caminhoBackup);

            ctx.json(Map.of(
                "success", true,
                "message", "Backup criado com sucesso!",
                "caminho", caminhoBackup
            ));

        } catch (Exception e) {
            logger.error("Erro ao criar backup manual", e);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar backup: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/backup/csv
     * Exporta todas as tarefas para arquivo CSV.
     */
    public void exportarCSV(Context ctx) {
        try {
            User currentUser = SessionUtil.getCurrentUser(ctx);

            String caminhoCSV = backupService.exportarTarefasCSV(currentUser.getId());

            logger.info("Export CSV criado por {}: {}", currentUser.getUsername(), caminhoCSV);

            ctx.json(Map.of(
                "success", true,
                "message", "Tarefas exportadas para CSV com sucesso!",
                "caminho", caminhoCSV
            ));

        } catch (Exception e) {
            logger.error("Erro ao exportar CSV", e);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao exportar CSV: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/backup/listar
     * Lista todos os backups registrados.
     */
    public void listarBackups(Context ctx) {
        try {
            List<BackupService.BackupInfo> backups = backupService.listarBackups();

            ctx.json(Map.of(
                "success", true,
                "dados", backups
            ));

        } catch (Exception e) {
            logger.error("Erro ao listar backups", e);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar backups: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/backup/download/{id}
     * Faz download de um backup específico.
     */
    public void downloadBackup(Context ctx) {
        try {
            Long backupId = Long.parseLong(ctx.pathParam("id"));

            // Buscar informações do backup
            List<BackupService.BackupInfo> backups = backupService.listarBackups();
            BackupService.BackupInfo backup = backups.stream()
                .filter(b -> b.id.equals(backupId))
                .findFirst()
                .orElse(null);

            if (backup == null) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "message", "Backup não encontrado"
                ));
                return;
            }

            // Verificar se arquivo existe
            File file = new File(backup.caminhoArquivo);
            if (!file.exists()) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "message", "Arquivo de backup não encontrado"
                ));
                return;
            }

            // Enviar arquivo
            ctx.contentType("application/octet-stream");
            ctx.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            ctx.result(Files.readAllBytes(file.toPath()));

            logger.info("Download de backup ID {} realizado", backupId);

        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                "success", false,
                "message", "ID de backup inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao fazer download de backup", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "message", "Erro ao fazer download: " + e.getMessage()
            ));
        }
    }
}
