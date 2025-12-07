package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.dto.ProcessoDTO;
import com.notisblokk.audiencias.dto.ProcessoParticipanteDTO;
import com.notisblokk.audiencias.dto.ProcessoRequestDTO;
import com.notisblokk.audiencias.model.enums.TipoParticipacao;
import com.notisblokk.audiencias.service.ProcessoService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de Processos Judiciais.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>GET /api/processos - Listar todos os processos</li>
 *   <li>GET /api/processos/{id} - Buscar processo por ID</li>
 *   <li>POST /api/processos - Criar novo processo</li>
 *   <li>PUT /api/processos/{id} - Atualizar processo</li>
 *   <li>DELETE /api/processos/{id} - Deletar processo</li>
 *   <li>GET /api/processos/{id}/participantes - Listar participantes do processo</li>
 *   <li>POST /api/processos/{id}/participantes - Adicionar participante ao processo</li>
 *   <li>DELETE /api/processos/{id}/participantes/{participanteId} - Remover participante</li>
 * </ul>
 */
public class ProcessoController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessoController.class);
    private final ProcessoService processoService;

    public ProcessoController() {
        this.processoService = new ProcessoService();
    }

    /**
     * GET /api/processos
     * Lista todos os processos cadastrados.
     */
    public void listar(Context ctx) {
        try {
            logger.debug("ProcessoController.listar()");
            List<ProcessoDTO> processos = processoService.listar();

            ctx.json(Map.of(
                "success", true,
                "dados", processos
            ));

            logger.debug("Listados {} processos", processos.size());

        } catch (Exception e) {
            logger.error("Erro ao listar processos", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar processos: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/processos/{id}
     * Busca um processo por ID com participantes.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            logger.debug("ProcessoController.buscarPorId() - ID: {}", id);

            Optional<ProcessoDTO> processoOpt = processoService.buscarPorId(id);

            if (processoOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Processo não encontrado"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", processoOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao buscar processo", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar processo: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/processos
     * Cria um novo processo.
     */
    public void criar(Context ctx) {
        try {
            ProcessoRequestDTO request = ctx.bodyAsClass(ProcessoRequestDTO.class);
            logger.debug("ProcessoController.criar() - Número: {}", request.getNumeroProcesso());

            ProcessoDTO processo = processoService.criar(request);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Processo criado com sucesso",
                "dados", processo
            ));

            logger.info("Processo criado - ID: {}, Número: {}", processo.getId(), processo.getNumeroProcesso());

        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Erro ao criar processo", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar processo: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/processos/{id}
     * Atualiza um processo existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ProcessoRequestDTO request = ctx.bodyAsClass(ProcessoRequestDTO.class);
            logger.debug("ProcessoController.atualizar() - ID: {}", id);

            ProcessoDTO processo = processoService.atualizar(id, request);

            ctx.json(Map.of(
                "success", true,
                "message", "Processo atualizado com sucesso",
                "dados", processo
            ));

            logger.info("Processo atualizado - ID: {}", id);

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
            logger.error("Erro ao atualizar processo", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar processo: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/processos/{id}
     * Deleta um processo.
     * ATENÇÃO: Audiências e participantes serão deletados em CASCADE.
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            logger.debug("ProcessoController.deletar() - ID: {}", id);

            processoService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Processo deletado com sucesso"
            ));

            logger.warn("Processo deletado - ID: {}", id);

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
            logger.error("Erro ao deletar processo", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar processo: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/processos/{id}/participantes
     * Lista todos os participantes de um processo.
     */
    public void listarParticipantes(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            logger.debug("ProcessoController.listarParticipantes() - Processo ID: {}", id);

            List<ProcessoParticipanteDTO> participantes = processoService.listarParticipantes(id);

            ctx.json(Map.of(
                "success", true,
                "dados", participantes
            ));

            logger.debug("Listados {} participantes do processo ID: {}", participantes.size(), id);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("Erro ao listar participantes do processo", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar participantes: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/processos/{id}/participantes
     * Adiciona um participante ao processo.
     *
     * Body JSON esperado:
     * {
     *   "pessoaId": 1,
     *   "tipoParticipacao": "REU"
     * }
     */
    public void adicionarParticipante(Context ctx) {
        try {
            Long processoId = Long.parseLong(ctx.pathParam("id"));

            // Parsear body
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            Long pessoaId = ((Number) body.get("pessoaId")).longValue();
            String tipoStr = (String) body.get("tipoParticipacao");
            TipoParticipacao tipo = TipoParticipacao.valueOf(tipoStr);

            logger.debug("ProcessoController.adicionarParticipante() - Processo: {}, Pessoa: {}, Tipo: {}",
                processoId, pessoaId, tipo);

            ProcessoParticipanteDTO participante = processoService.adicionarParticipante(processoId, pessoaId, tipo);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Participante adicionado com sucesso",
                "dados", participante
            ));

            logger.info("Participante adicionado ao processo {} - Pessoa ID: {}", processoId, pessoaId);

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
            logger.error("Erro ao adicionar participante ao processo", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao adicionar participante: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/processos/{id}/participantes/{participanteId}
     * Remove um participante do processo.
     */
    public void removerParticipante(Context ctx) {
        try {
            Long processoId = Long.parseLong(ctx.pathParam("id"));
            Long participanteId = Long.parseLong(ctx.pathParam("participanteId"));

            logger.debug("ProcessoController.removerParticipante() - Processo: {}, Participante: {}",
                processoId, participanteId);

            processoService.removerParticipante(processoId, participanteId);

            ctx.json(Map.of(
                "success", true,
                "message", "Participante removido com sucesso"
            ));

            logger.warn("Participante {} removido do processo {}", participanteId, processoId);

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
            logger.error("Erro ao remover participante do processo", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao remover participante: " + e.getMessage()
            ));
        }
    }
}
