package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.model.Advogado;
import com.notisblokk.audiencias.service.AdvogadoService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de Advogados.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>GET /api/audiencias/advogados - Listar todos os advogados</li>
 *   <li>GET /api/audiencias/advogados/{id} - Buscar advogado por ID</li>
 *   <li>GET /api/audiencias/advogados/buscar?nome=... - Buscar advogados por nome</li>
 *   <li>GET /api/audiencias/advogados/buscar?oab=... - Buscar advogados por OAB</li>
 *   <li>POST /api/audiencias/advogados - Criar novo advogado</li>
 *   <li>PUT /api/audiencias/advogados/{id} - Atualizar advogado</li>
 *   <li>DELETE /api/audiencias/advogados/{id} - Deletar advogado</li>
 * </ul>
 */
public class AdvogadoController {

    private static final Logger logger = LoggerFactory.getLogger(AdvogadoController.class);
    private final AdvogadoService advogadoService;

    public AdvogadoController() {
        this.advogadoService = new AdvogadoService();
    }

    /**
     * GET /api/audiencias/advogados
     * Lista todos os advogados cadastrados.
     */
    public void listar(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: AdvogadoController.listar()");
            List<Advogado> advogados = advogadoService.listarTodos();

            ctx.json(Map.of(
                "success", true,
                "dados", advogados
            ));

            logger.debug("DEBUG_AUDIENCIAS: Listados {} advogados", advogados.size());

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao listar advogados", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar advogados: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/advogados/{id}
     * Busca um advogado por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: AdvogadoController.buscarPorId() - ID: " + id);

            Optional<Advogado> advogadoOpt = advogadoService.buscarPorId(id);

            if (advogadoOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Advogado não encontrado"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", advogadoOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar advogado", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar advogado: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/advogados/buscar?nome=...
     * Busca advogados por nome (busca parcial).
     */
    public void buscarPorNome(Context ctx) {
        try {
            String nome = ctx.queryParam("nome");
            System.out.println("DEBUG_AUDIENCIAS: AdvogadoController.buscarPorNome() - Nome: " + nome);

            if (nome == null || nome.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro 'nome' é obrigatório"
                ));
                return;
            }

            List<Advogado> advogados = advogadoService.buscarPorNome(nome);

            ctx.json(Map.of(
                "success", true,
                "dados", advogados
            ));

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar advogados por nome", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar advogados: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/advogados/buscar-oab?oab=...
     * Busca advogados por OAB (busca exata).
     */
    public void buscarPorOAB(Context ctx) {
        try {
            String oab = ctx.queryParam("oab");
            System.out.println("DEBUG_AUDIENCIAS: AdvogadoController.buscarPorOAB() - OAB: " + oab);

            if (oab == null || oab.trim().isEmpty()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetro 'oab' é obrigatório"
                ));
                return;
            }

            List<Advogado> advogados = advogadoService.buscarPorOAB(oab);

            ctx.json(Map.of(
                "success", true,
                "dados", advogados
            ));

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar advogados por OAB", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar advogados: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/audiencias/advogados
     * Cria um novo advogado.
     */
    public void criar(Context ctx) {
        try {
            Advogado advogado = ctx.bodyAsClass(Advogado.class);
            System.out.println("DEBUG_AUDIENCIAS: AdvogadoController.criar() - Nome: " + advogado.getNome() + ", OAB: " + advogado.getOab());

            advogado = advogadoService.criar(advogado);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Advogado criado com sucesso",
                "dados", advogado
            ));

            logger.info("DEBUG_AUDIENCIAS: Advogado criado - ID: {}, Nome: {}, OAB: {}",
                advogado.getId(), advogado.getNome(), advogado.getOab());

        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao criar advogado", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar advogado: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/audiencias/advogados/{id}
     * Atualiza um advogado existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Advogado advogado = ctx.bodyAsClass(Advogado.class);
            System.out.println("DEBUG_AUDIENCIAS: AdvogadoController.atualizar() - ID: " + id);

            advogado = advogadoService.atualizar(id, advogado);

            ctx.json(Map.of(
                "success", true,
                "message", "Advogado atualizado com sucesso",
                "dados", advogado
            ));

            logger.info("DEBUG_AUDIENCIAS: Advogado atualizado - ID: {}", id);

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
            logger.error("DEBUG_AUDIENCIAS: Erro ao atualizar advogado", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar advogado: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/audiencias/advogados/{id}
     * Deleta um advogado.
     * ATENÇÃO: Representações associadas serão deletadas em cascata.
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: AdvogadoController.deletar() - ID: " + id);

            advogadoService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Advogado deletado com sucesso"
            ));

            logger.warn("DEBUG_AUDIENCIAS: Advogado deletado - ID: {}", id);

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
            logger.error("DEBUG_AUDIENCIAS: Erro ao deletar advogado", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar advogado: " + e.getMessage()
            ));
        }
    }
}
