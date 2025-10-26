package com.notisblokk.controller;

import com.notisblokk.model.NotaDTO;
import com.notisblokk.service.NotaService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller responsável pelo gerenciamento de notas.
 *
 * <p>Gerencia os endpoints REST para operações CRUD de notas:</p>
 * <ul>
 *   <li>GET /api/notas - Listar todas as notas</li>
 *   <li>GET /api/notas/{id} - Buscar nota por ID</li>
 *   <li>GET /api/notas/etiqueta/{etiquetaId} - Buscar notas por etiqueta</li>
 *   <li>POST /api/notas - Criar nova nota</li>
 *   <li>PUT /api/notas/{id} - Atualizar nota</li>
 *   <li>DELETE /api/notas/{id} - Deletar nota</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class NotaController {

    private static final Logger logger = LoggerFactory.getLogger(NotaController.class);
    private final NotaService notaService;

    /**
     * Construtor padrão.
     */
    public NotaController() {
        this.notaService = new NotaService();
    }

    /**
     * Classe interna para receber dados da requisição de criar/atualizar nota.
     */
    private static class NotaRequest {
        public Long etiquetaId;
        public Long statusId;
        public String titulo;
        public String conteudo;
        public String prazoFinal; // Formato: yyyy-MM-dd
    }

    /**
     * GET /api/notas
     * Lista todas as notas como DTOs completos.
     */
    public void listar(Context ctx) {
        try {
            List<NotaDTO> notas = notaService.listarTodas();

            ctx.json(Map.of(
                "success", true,
                "dados", notas
            ));

            logger.debug("Listadas {} notas", notas.size());

        } catch (Exception e) {
            logger.error("Erro ao listar notas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar notas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/notas/{id}
     * Busca uma nota por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Optional<NotaDTO> notaOpt = notaService.buscarPorId(id);

            if (notaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nota não encontrada"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", notaOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao buscar nota", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar nota: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/notas/etiqueta/{etiquetaId}
     * Busca notas por etiqueta.
     */
    public void buscarPorEtiqueta(Context ctx) {
        try {
            Long etiquetaId = Long.parseLong(ctx.pathParam("etiquetaId"));
            List<NotaDTO> notas = notaService.listarPorEtiqueta(etiquetaId);

            ctx.json(Map.of(
                "success", true,
                "dados", notas
            ));

            logger.debug("Encontradas {} notas da etiqueta {}", notas.size(), etiquetaId);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID da etiqueta inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao buscar notas por etiqueta", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar notas: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/notas
     * Cria uma nova nota.
     */
    public void criar(Context ctx) {
        try {
            // Obter dados do JSON
            NotaRequest request = ctx.bodyAsClass(NotaRequest.class);

            // Obter sessão e usuário atual
            Long sessaoId = SessionUtil.getCurrentSessionId(ctx);
            Long usuarioId = SessionUtil.getCurrentUserId(ctx);

            // Criar nota
            NotaDTO nota = notaService.criar(
                request.etiquetaId,
                request.statusId,
                request.titulo,
                request.conteudo,
                request.prazoFinal,
                sessaoId,
                usuarioId
            );

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Nota criada com sucesso",
                "dados", nota
            ));

            logger.info("Nota criada: {} por usuário {}", request.titulo, usuarioId);

        } catch (Exception e) {
            logger.error("Erro ao criar nota", e);
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/notas/{id}
     * Atualiza uma nota existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Obter dados do JSON
            NotaRequest request = ctx.bodyAsClass(NotaRequest.class);

            // Atualizar nota
            NotaDTO nota = notaService.atualizar(
                id,
                request.etiquetaId,
                request.statusId,
                request.titulo,
                request.conteudo,
                request.prazoFinal
            );

            ctx.json(Map.of(
                "success", true,
                "message", "Nota atualizada com sucesso",
                "dados", nota
            ));

            logger.info("Nota ID {} atualizada", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao atualizar nota", e);
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/notas/{id}
     * Deleta uma nota.
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));

            // Verificar se nota existe
            Optional<NotaDTO> notaOpt = notaService.buscarPorId(id);
            if (notaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Nota não encontrada"
                ));
                return;
            }

            // Deletar nota
            notaService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Nota deletada com sucesso"
            ));

            logger.warn("Nota ID {} deletada", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao deletar nota", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar nota: " + e.getMessage()
            ));
        }
    }
}
