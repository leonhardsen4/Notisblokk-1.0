package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.model.Juiz;
import com.notisblokk.audiencias.service.JuizService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de Juízes.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>GET /api/audiencias/juizes - Listar todos os juízes</li>
 *   <li>GET /api/audiencias/juizes/{id} - Buscar juiz por ID</li>
 *   <li>GET /api/audiencias/juizes/buscar?nome=... - Buscar juízes por nome</li>
 *   <li>POST /api/audiencias/juizes - Criar novo juiz</li>
 *   <li>PUT /api/audiencias/juizes/{id} - Atualizar juiz</li>
 *   <li>DELETE /api/audiencias/juizes/{id} - Deletar juiz</li>
 * </ul>
 */
public class JuizController {

    private static final Logger logger = LoggerFactory.getLogger(JuizController.class);
    private final JuizService juizService;

    public JuizController() {
        this.juizService = new JuizService();
    }

    /**
     * GET /api/audiencias/juizes
     * Lista todos os juízes cadastrados.
     */
    public void listar(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: JuizController.listar()");
            List<Juiz> juizes = juizService.listarTodos();

            ctx.json(Map.of(
                "success", true,
                "dados", juizes
            ));

            logger.debug("DEBUG_AUDIENCIAS: Listados {} juízes", juizes.size());

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao listar juízes", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar juízes: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/juizes/{id}
     * Busca um juiz por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: JuizController.buscarPorId() - ID: " + id);

            Optional<Juiz> juizOpt = juizService.buscarPorId(id);

            if (juizOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Juiz não encontrado"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", juizOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar juiz", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar juiz: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/juizes/buscar?nome=...
     * Busca juízes por nome (busca parcial).
     */
    public void buscarPorNome(Context ctx) {
        try {
            String nome = ctx.queryParam("nome");
            System.out.println("DEBUG_AUDIENCIAS: JuizController.buscarPorNome() - Nome: " + nome);

            if (nome == null || nome.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro 'nome' é obrigatório"
                ));
                return;
            }

            List<Juiz> juizes = juizService.buscarPorNome(nome);

            ctx.json(Map.of(
                "success", true,
                "dados", juizes
            ));

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar juízes por nome", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar juízes: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/audiencias/juizes
     * Cria um novo juiz.
     */
    public void criar(Context ctx) {
        try {
            Juiz juiz = ctx.bodyAsClass(Juiz.class);
            System.out.println("DEBUG_AUDIENCIAS: JuizController.criar() - Nome: " + juiz.getNome());

            juiz = juizService.criar(juiz);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Juiz criado com sucesso",
                "dados", juiz
            ));

            logger.info("DEBUG_AUDIENCIAS: Juiz criado - ID: {}, Nome: {}", juiz.getId(), juiz.getNome());

        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao criar juiz", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar juiz: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/audiencias/juizes/{id}
     * Atualiza um juiz existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Juiz juiz = ctx.bodyAsClass(Juiz.class);
            System.out.println("DEBUG_AUDIENCIAS: JuizController.atualizar() - ID: " + id);

            juiz = juizService.atualizar(id, juiz);

            ctx.json(Map.of(
                "success", true,
                "message", "Juiz atualizado com sucesso",
                "dados", juiz
            ));

            logger.info("DEBUG_AUDIENCIAS: Juiz atualizado - ID: {}", id);

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
            logger.error("DEBUG_AUDIENCIAS: Erro ao atualizar juiz", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar juiz: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/audiencias/juizes/{id}
     * Deleta um juiz.
     * NOTA: Audiências associadas terão juiz_id definido como NULL (ON DELETE SET NULL).
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: JuizController.deletar() - ID: " + id);

            juizService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Juiz deletado com sucesso"
            ));

            logger.warn("DEBUG_AUDIENCIAS: Juiz deletado - ID: {}", id);

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
            logger.error("DEBUG_AUDIENCIAS: Erro ao deletar juiz", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar juiz: " + e.getMessage()
            ));
        }
    }
}
