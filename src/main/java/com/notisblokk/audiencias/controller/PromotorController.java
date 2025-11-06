package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.model.Promotor;
import com.notisblokk.audiencias.service.PromotorService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de Promotores de Justiça.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>GET /api/audiencias/promotores - Listar todos os promotores</li>
 *   <li>GET /api/audiencias/promotores/{id} - Buscar promotor por ID</li>
 *   <li>GET /api/audiencias/promotores/buscar?nome=... - Buscar promotores por nome</li>
 *   <li>POST /api/audiencias/promotores - Criar novo promotor</li>
 *   <li>PUT /api/audiencias/promotores/{id} - Atualizar promotor</li>
 *   <li>DELETE /api/audiencias/promotores/{id} - Deletar promotor</li>
 * </ul>
 */
public class PromotorController {

    private static final Logger logger = LoggerFactory.getLogger(PromotorController.class);
    private final PromotorService promotorService;

    public PromotorController() {
        this.promotorService = new PromotorService();
    }

    /**
     * GET /api/audiencias/promotores
     * Lista todos os promotores cadastrados.
     */
    public void listar(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: PromotorController.listar()");
            List<Promotor> promotores = promotorService.listarTodos();

            ctx.json(Map.of(
                "success", true,
                "dados", promotores
            ));

            logger.debug("DEBUG_AUDIENCIAS: Listados {} promotores", promotores.size());

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao listar promotores", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar promotores: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/promotores/{id}
     * Busca um promotor por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: PromotorController.buscarPorId() - ID: " + id);

            Optional<Promotor> promotorOpt = promotorService.buscarPorId(id);

            if (promotorOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Promotor não encontrado"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", promotorOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar promotor", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar promotor: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/promotores/buscar?nome=...
     * Busca promotores por nome (busca parcial).
     */
    public void buscarPorNome(Context ctx) {
        try {
            String nome = ctx.queryParam("nome");
            System.out.println("DEBUG_AUDIENCIAS: PromotorController.buscarPorNome() - Nome: " + nome);

            if (nome == null || nome.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro 'nome' é obrigatório"
                ));
                return;
            }

            List<Promotor> promotores = promotorService.buscarPorNome(nome);

            ctx.json(Map.of(
                "success", true,
                "dados", promotores
            ));

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar promotores por nome", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar promotores: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/audiencias/promotores
     * Cria um novo promotor.
     */
    public void criar(Context ctx) {
        try {
            Promotor promotor = ctx.bodyAsClass(Promotor.class);
            System.out.println("DEBUG_AUDIENCIAS: PromotorController.criar() - Nome: " + promotor.getNome());

            promotor = promotorService.criar(promotor);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Promotor criado com sucesso",
                "dados", promotor
            ));

            logger.info("DEBUG_AUDIENCIAS: Promotor criado - ID: {}, Nome: {}", promotor.getId(), promotor.getNome());

        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao criar promotor", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar promotor: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/audiencias/promotores/{id}
     * Atualiza um promotor existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Promotor promotor = ctx.bodyAsClass(Promotor.class);
            System.out.println("DEBUG_AUDIENCIAS: PromotorController.atualizar() - ID: " + id);

            promotor = promotorService.atualizar(id, promotor);

            ctx.json(Map.of(
                "success", true,
                "message", "Promotor atualizado com sucesso",
                "dados", promotor
            ));

            logger.info("DEBUG_AUDIENCIAS: Promotor atualizado - ID: {}", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao atualizar promotor", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar promotor: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/audiencias/promotores/{id}
     * Deleta um promotor.
     * NOTA: Audiências associadas terão promotor_id definido como NULL (ON DELETE SET NULL).
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: PromotorController.deletar() - ID: " + id);

            promotorService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Promotor deletado com sucesso"
            ));

            logger.warn("DEBUG_AUDIENCIAS: Promotor deletado - ID: {}", id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(404);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao deletar promotor", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar promotor: " + e.getMessage()
            ));
        }
    }
}
