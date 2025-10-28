package com.notisblokk.controller;

import com.notisblokk.model.User;
import com.notisblokk.service.FileUploadService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Controller responsável pelo gerenciamento de anexos em notas.
 *
 * <p>Gerencia:</p>
 * <ul>
 *   <li>Upload de arquivos anexados a notas</li>
 *   <li>Listagem de anexos de uma nota</li>
 *   <li>Download de anexos</li>
 *   <li>Remoção de anexos</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class AnexoController {

    private static final Logger logger = LoggerFactory.getLogger(AnexoController.class);
    private final FileUploadService fileUploadService;

    public AnexoController() {
        this.fileUploadService = new FileUploadService();
    }

    /**
     * POST /api/notas/{notaId}/anexos
     * Faz upload de um anexo para uma nota.
     */
    public void upload(Context ctx) {
        try {
            Long notaId = Long.parseLong(ctx.pathParam("notaId"));
            User currentUser = SessionUtil.getCurrentUser(ctx);
            UploadedFile file = ctx.uploadedFile("arquivo");

            if (file == null) {
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nenhum arquivo selecionado"
                ));
                return;
            }

            // Upload do arquivo
            FileUploadService.AnexoInfo anexo = fileUploadService.uploadArquivo(
                notaId,
                file.filename(),
                file.content(),
                currentUser.getId()
            );

            logger.info("Anexo criado para nota {}: {}", notaId, file.filename());

            ctx.json(Map.of(
                "success", true,
                "message", "Arquivo anexado com sucesso!",
                "anexo", anexo
            ));

        } catch (NumberFormatException e) {
            ctx.json(Map.of(
                "success", false,
                "message", "ID de nota inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao fazer upload de anexo", e);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao anexar arquivo: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/notas/{notaId}/anexos
     * Lista todos os anexos de uma nota.
     */
    public void listar(Context ctx) {
        try {
            Long notaId = Long.parseLong(ctx.pathParam("notaId"));

            List<FileUploadService.AnexoInfo> anexos = fileUploadService.listarAnexos(notaId);

            ctx.json(Map.of(
                "success", true,
                "dados", anexos
            ));

        } catch (NumberFormatException e) {
            ctx.json(Map.of(
                "success", false,
                "message", "ID de nota inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao listar anexos", e);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar anexos: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/anexos/{id}/download
     * Faz download de um anexo.
     */
    public void download(Context ctx) {
        try {
            Long anexoId = Long.parseLong(ctx.pathParam("id"));

            FileUploadService.AnexoInfo anexo = fileUploadService.buscarAnexo(anexoId);

            if (anexo == null) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "message", "Anexo não encontrado"
                ));
                return;
            }

            // Verificar se arquivo existe
            File file = new File(anexo.caminhoArquivo);
            if (!file.exists()) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "message", "Arquivo não encontrado"
                ));
                return;
            }

            // Enviar arquivo
            ctx.contentType(anexo.tipoMime != null ? anexo.tipoMime : "application/octet-stream");
            ctx.header("Content-Disposition", "attachment; filename=\"" + anexo.nomeArquivo + "\"");
            ctx.result(Files.readAllBytes(file.toPath()));

            logger.info("Download de anexo ID {}: {}", anexoId, anexo.nomeArquivo);

        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                "success", false,
                "message", "ID de anexo inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao fazer download de anexo", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "message", "Erro ao fazer download: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/anexos/{id}/visualizar
     * Visualiza um anexo inline (abre no navegador).
     */
    public void visualizar(Context ctx) {
        try {
            Long anexoId = Long.parseLong(ctx.pathParam("id"));

            FileUploadService.AnexoInfo anexo = fileUploadService.buscarAnexo(anexoId);

            if (anexo == null) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "message", "Anexo não encontrado"
                ));
                return;
            }

            // Verificar se arquivo existe
            File file = new File(anexo.caminhoArquivo);
            if (!file.exists()) {
                ctx.status(404).json(Map.of(
                    "success", false,
                    "message", "Arquivo não encontrado"
                ));
                return;
            }

            // Enviar arquivo para visualização inline
            ctx.contentType(anexo.tipoMime != null ? anexo.tipoMime : "application/octet-stream");
            ctx.header("Content-Disposition", "inline; filename=\"" + anexo.nomeArquivo + "\"");
            ctx.result(Files.readAllBytes(file.toPath()));

            logger.info("Visualização de anexo ID {}: {}", anexoId, anexo.nomeArquivo);

        } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of(
                "success", false,
                "message", "ID de anexo inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao visualizar anexo", e);
            ctx.status(500).json(Map.of(
                "success", false,
                "message", "Erro ao visualizar: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/anexos/{id}
     * Remove um anexo.
     */
    public void remover(Context ctx) {
        try {
            Long anexoId = Long.parseLong(ctx.pathParam("id"));

            fileUploadService.removerAnexo(anexoId);

            logger.info("Anexo ID {} removido", anexoId);

            ctx.json(Map.of(
                "success", true,
                "message", "Anexo removido com sucesso!"
            ));

        } catch (NumberFormatException e) {
            ctx.json(Map.of(
                "success", false,
                "message", "ID de anexo inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao remover anexo", e);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao remover anexo: " + e.getMessage()
            ));
        }
    }
}
