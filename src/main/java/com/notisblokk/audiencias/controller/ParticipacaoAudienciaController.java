package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.model.ParticipacaoAudiencia;
import com.notisblokk.audiencias.service.ParticipacaoAudienciaService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de Participações em Audiências.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>GET /api/audiencias/participacoes/audiencia/{audienciaId} - Listar participantes de uma audiência</li>
 *   <li>GET /api/audiencias/participacoes/pessoa/{pessoaId} - Listar audiências de uma pessoa</li>
 *   <li>GET /api/audiencias/participacoes/{id} - Buscar participação por ID</li>
 *   <li>POST /api/audiencias/participacoes - Criar nova participação</li>
 *   <li>PUT /api/audiencias/participacoes/{id} - Atualizar participação</li>
 *   <li>DELETE /api/audiencias/participacoes/{id} - Deletar participação</li>
 * </ul>
 */
public class ParticipacaoAudienciaController {

    private static final Logger logger = LoggerFactory.getLogger(ParticipacaoAudienciaController.class);
    private final ParticipacaoAudienciaService participacaoService;

    public ParticipacaoAudienciaController() {
        this.participacaoService = new ParticipacaoAudienciaService();
    }

    /**
     * GET /api/audiencias/participacoes/audiencia/{audienciaId}
     * Lista todos os participantes de uma audiência com dados completos.
     */
    public void listarPorAudiencia(Context ctx) {
        try {
            Long audienciaId = Long.parseLong(ctx.pathParam("audienciaId"));
            System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaController.listarPorAudiencia() - AudienciaID: " + audienciaId);

            // Usar método que retorna DTOs com dados completos
            var participantes = participacaoService.buscarDetalhesParticipantes(audienciaId);

            ctx.json(Map.of(
                "success", true,
                "dados", participantes
            ));

            logger.debug("DEBUG_AUDIENCIAS: Listadas {} participações da audiência {}", participantes.size(), audienciaId);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID de audiência inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao listar participações por audiência", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar participações: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/participacoes/pessoa/{pessoaId}
     * Lista todas as audiências que uma pessoa participa.
     */
    public void listarPorPessoa(Context ctx) {
        try {
            Long pessoaId = Long.parseLong(ctx.pathParam("pessoaId"));
            System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaController.listarPorPessoa() - PessoaID: " + pessoaId);

            List<ParticipacaoAudiencia> participacoes = participacaoService.buscarPorPessoa(pessoaId);

            ctx.json(Map.of(
                "success", true,
                "dados", participacoes
            ));

            logger.debug("DEBUG_AUDIENCIAS: Listadas {} participações da pessoa {}", participacoes.size(), pessoaId);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID de pessoa inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao listar participações por pessoa", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar participações: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/participacoes/{id}
     * Busca uma participação por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaController.buscarPorId() - ID: " + id);

            Optional<ParticipacaoAudiencia> participacaoOpt = participacaoService.buscarPorId(id);

            if (participacaoOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Participação não encontrada"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", participacaoOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar participação", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar participação: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/audiencias/participacoes
     * Cria uma nova participação.
     */
    public void criar(Context ctx) {
        try {
            ParticipacaoAudiencia participacao = ctx.bodyAsClass(ParticipacaoAudiencia.class);
            System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaController.criar() - " +
                "Audiência: " + (participacao.getAudiencia() != null ? participacao.getAudiencia().getId() : "null") +
                ", Pessoa: " + (participacao.getPessoa() != null ? participacao.getPessoa().getId() : "null"));

            participacao = participacaoService.criar(participacao);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Participação criada com sucesso",
                "dados", participacao
            ));

            logger.info("DEBUG_AUDIENCIAS: Participação criada - ID: {}", participacao.getId());

        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao criar participação", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar participação: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/audiencias/participacoes/{id}
     * Atualiza uma participação existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ParticipacaoAudiencia participacao = ctx.bodyAsClass(ParticipacaoAudiencia.class);
            System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaController.atualizar() - ID: " + id);

            participacao = participacaoService.atualizar(id, participacao);

            ctx.json(Map.of(
                "success", true,
                "message", "Participação atualizada com sucesso",
                "dados", participacao
            ));

            logger.info("DEBUG_AUDIENCIAS: Participação atualizada - ID: {}", id);

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
            logger.error("DEBUG_AUDIENCIAS: Erro ao atualizar participação", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar participação: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/audiencias/participacoes/{id}
     * Deleta uma participação.
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaController.deletar() - ID: " + id);

            participacaoService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Participação deletada com sucesso"
            ));

            logger.warn("DEBUG_AUDIENCIAS: Participação deletada - ID: {}", id);

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
            logger.error("DEBUG_AUDIENCIAS: Erro ao deletar participação", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar participação: " + e.getMessage()
            ));
        }
    }
}
