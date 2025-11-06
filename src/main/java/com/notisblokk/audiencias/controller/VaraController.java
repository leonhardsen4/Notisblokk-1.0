package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.model.Vara;
import com.notisblokk.audiencias.service.VaraService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de Varas Judiciais.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>GET /api/audiencias/varas - Listar todas as varas</li>
 *   <li>GET /api/audiencias/varas/{id} - Buscar vara por ID</li>
 *   <li>GET /api/audiencias/varas/buscar?nome=... - Buscar varas por nome</li>
 *   <li>POST /api/audiencias/varas - Criar nova vara</li>
 *   <li>PUT /api/audiencias/varas/{id} - Atualizar vara</li>
 *   <li>DELETE /api/audiencias/varas/{id} - Deletar vara</li>
 * </ul>
 */
public class VaraController {

    private static final Logger logger = LoggerFactory.getLogger(VaraController.class);
    private final VaraService varaService;

    public VaraController() {
        this.varaService = new VaraService();
    }

    /**
     * GET /api/audiencias/varas
     * Lista todas as varas cadastradas.
     */
    public void listar(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: VaraController.listar()");
            List<Vara> varas = varaService.listarTodas();

            ctx.json(Map.of(
                "success", true,
                "dados", varas
            ));

            logger.debug("DEBUG_AUDIENCIAS: Listadas {} varas", varas.size());

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao listar varas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar varas: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/varas/{id}
     * Busca uma vara por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: VaraController.buscarPorId() - ID: " + id);

            Optional<Vara> varaOpt = varaService.buscarPorId(id);

            if (varaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Vara não encontrada"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", varaOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar vara", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar vara: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/varas/buscar?nome=...
     * Busca varas por nome (busca parcial).
     */
    public void buscarPorNome(Context ctx) {
        try {
            String nome = ctx.queryParam("nome");
            System.out.println("DEBUG_AUDIENCIAS: VaraController.buscarPorNome() - Nome: " + nome);

            if (nome == null || nome.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro 'nome' é obrigatório"
                ));
                return;
            }

            List<Vara> varas = varaService.buscarPorNome(nome);

            ctx.json(Map.of(
                "success", true,
                "dados", varas
            ));

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar varas por nome", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar varas: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/audiencias/varas
     * Cria uma nova vara.
     */
    public void criar(Context ctx) {
        try {
            Vara vara = ctx.bodyAsClass(Vara.class);
            System.out.println("DEBUG_AUDIENCIAS: VaraController.criar() - Nome: " + vara.getNome());

            vara = varaService.criar(vara);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Vara criada com sucesso",
                "dados", vara
            ));

            logger.info("DEBUG_AUDIENCIAS: Vara criada - ID: {}, Nome: {}", vara.getId(), vara.getNome());

        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao criar vara", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar vara: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/audiencias/varas/{id}
     * Atualiza uma vara existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Vara vara = ctx.bodyAsClass(Vara.class);
            System.out.println("DEBUG_AUDIENCIAS: VaraController.atualizar() - ID: " + id);

            vara = varaService.atualizar(id, vara);

            ctx.json(Map.of(
                "success", true,
                "message", "Vara atualizada com sucesso",
                "dados", vara
            ));

            logger.info("DEBUG_AUDIENCIAS: Vara atualizada - ID: {}", id);

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
            logger.error("DEBUG_AUDIENCIAS: Erro ao atualizar vara", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar vara: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/audiencias/varas/{id}
     * Deleta uma vara.
     * ATENÇÃO: Falha se houver audiências associadas (ON DELETE RESTRICT).
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: VaraController.deletar() - ID: " + id);

            varaService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Vara deletada com sucesso"
            ));

            logger.warn("DEBUG_AUDIENCIAS: Vara deletada - ID: {}", id);

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
            logger.error("DEBUG_AUDIENCIAS: Erro ao deletar vara", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar vara: " + e.getMessage()
            ));
        }
    }
}
