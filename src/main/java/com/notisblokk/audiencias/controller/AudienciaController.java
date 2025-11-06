package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.dto.AudienciaRequest;
import com.notisblokk.audiencias.model.Audiencia;
import com.notisblokk.audiencias.service.AudienciaService;
import com.notisblokk.audiencias.util.DateUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de Audiências Judiciais.
 *
 * <p>Controller principal do módulo de audiências, responsável por gerenciar
 * todas as operações relacionadas a audiências judiciais, incluindo:</p>
 * <ul>
 *   <li>GET /api/audiencias - Listar todas as audiências</li>
 *   <li>GET /api/audiencias/{id} - Buscar audiência por ID</li>
 *   <li>GET /api/audiencias/data/{data} - Buscar audiências por data (dd/MM/yyyy)</li>
 *   <li>GET /api/audiencias/vara/{varaId} - Buscar audiências por vara</li>
 *   <li>GET /api/audiencias/conflitos - Verificar conflitos de horário</li>
 *   <li>POST /api/audiencias - Criar nova audiência</li>
 *   <li>PUT /api/audiencias/{id} - Atualizar audiência</li>
 *   <li>DELETE /api/audiencias/{id} - Deletar audiência</li>
 * </ul>
 */
public class AudienciaController {

    private static final Logger logger = LoggerFactory.getLogger(AudienciaController.class);
    private final AudienciaService audienciaService;

    public AudienciaController() {
        this.audienciaService = new AudienciaService();
    }

    /**
     * GET /api/audiencias
     * Lista todas as audiências cadastradas.
     */
    public void listar(Context ctx) {
        try {
            System.out.println("DEBUG_AUDIENCIAS: AudienciaController.listar()");
            List<Audiencia> audiencias = audienciaService.listarTodas();

            ctx.json(Map.of(
                "success", true,
                "dados", audiencias
            ));

            logger.debug("DEBUG_AUDIENCIAS: Listadas {} audiências", audiencias.size());

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao listar audiências", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao listar audiências: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/{id}
     * Busca uma audiência por ID.
     */
    public void buscarPorId(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: AudienciaController.buscarPorId() - ID: " + id);

            Optional<Audiencia> audienciaOpt = audienciaService.buscarPorId(id);

            if (audienciaOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Audiência não encontrada"
                ));
                return;
            }

            ctx.json(Map.of(
                "success", true,
                "dados", audienciaOpt.get()
            ));

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar audiência", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar audiência: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/data/{data}
     * Busca audiências por data (formato: dd/MM/yyyy ou dd-MM-yyyy).
     */
    public void buscarPorData(Context ctx) {
        try {
            String dataStr = ctx.pathParam("data");
            // Aceitar tanto dd/MM/yyyy quanto dd-MM-yyyy
            dataStr = dataStr.replace("-", "/");

            System.out.println("DEBUG_AUDIENCIAS: AudienciaController.buscarPorData() - Data: " + dataStr);

            LocalDate data = DateUtil.parseDate(dataStr);
            if (data == null) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Data inválida. Use o formato: dd/MM/yyyy"
                ));
                return;
            }

            List<Audiencia> audiencias = audienciaService.buscarPorData(data);

            ctx.json(Map.of(
                "success", true,
                "dados", audiencias
            ));

            logger.debug("DEBUG_AUDIENCIAS: Encontradas {} audiências para a data {}", audiencias.size(), dataStr);

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar audiências por data", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar audiências: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/vara/{varaId}
     * Busca audiências por vara.
     */
    public void buscarPorVara(Context ctx) {
        try {
            Long varaId = Long.parseLong(ctx.pathParam("varaId"));
            System.out.println("DEBUG_AUDIENCIAS: AudienciaController.buscarPorVara() - VaraID: " + varaId);

            List<Audiencia> audiencias = audienciaService.buscarPorVara(varaId);

            ctx.json(Map.of(
                "success", true,
                "dados", audiencias
            ));

            logger.debug("DEBUG_AUDIENCIAS: Encontradas {} audiências para a vara {}", audiencias.size(), varaId);

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID de vara inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar audiências por vara", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar audiências: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/conflitos
     * Verifica conflitos de horário para uma possível audiência.
     *
     * Query params:
     * - data: dd/MM/yyyy
     * - horarioInicio: HH:mm ou HH:mm:ss
     * - duracao: minutos (int)
     * - varaId: ID da vara (long)
     * - audienciaId: ID da audiência a excluir da verificação (opcional)
     */
    public void verificarConflitos(Context ctx) {
        try {
            String dataStr = ctx.queryParam("data");
            String horarioInicioStr = ctx.queryParam("horarioInicio");
            String duracaoStr = ctx.queryParam("duracao");
            String varaIdStr = ctx.queryParam("varaId");
            String audienciaIdStr = ctx.queryParam("audienciaId");

            System.out.println("DEBUG_AUDIENCIAS: AudienciaController.verificarConflitos() - " +
                "Data: " + dataStr + ", Horário: " + horarioInicioStr + ", Duração: " + duracaoStr + " min");

            // Validar parâmetros obrigatórios
            if (dataStr == null || horarioInicioStr == null || duracaoStr == null || varaIdStr == null) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Parâmetros obrigatórios: data, horarioInicio, duracao, varaId"
                ));
                return;
            }

            // Parsear data e horário
            LocalDate data = DateUtil.parseDate(dataStr);
            LocalTime horarioInicio = DateUtil.parseTime(horarioInicioStr);
            Integer duracao = Integer.parseInt(duracaoStr);
            Long varaId = Long.parseLong(varaIdStr);
            Long audienciaId = audienciaIdStr != null ? Long.parseLong(audienciaIdStr) : null;

            if (data == null) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Data inválida. Use o formato: dd/MM/yyyy"
                ));
                return;
            }

            if (horarioInicio == null) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Horário inválido. Use o formato: HH:mm"
                ));
                return;
            }

            // Verificar conflitos
            List<Map<String, Object>> conflitos = audienciaService.verificarConflitosHorario(
                data, horarioInicio, duracao, varaId, audienciaId
            );

            ctx.json(Map.of(
                "success", true,
                "temConflito", !conflitos.isEmpty(),
                "conflitos", conflitos
            ));

            if (!conflitos.isEmpty()) {
                logger.warn("DEBUG_AUDIENCIAS: {} conflito(s) detectado(s)", conflitos.size());
            }

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "Parâmetros numéricos inválidos"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao verificar conflitos", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao verificar conflitos: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/audiencias
     * Cria uma nova audiência.
     */
    public void criar(Context ctx) {
        try {
            // Receber DTO com IDs
            AudienciaRequest request = ctx.bodyAsClass(AudienciaRequest.class);
            System.out.println("DEBUG_AUDIENCIAS: AudienciaController.criar() - " +
                "Processo: " + request.getNumeroProcesso() +
                ", Data: " + request.getDataAudiencia() +
                ", Participantes: " + (request.getParticipantes() != null ? request.getParticipantes().size() : 0));

            // Service converte DTO para Audiencia e salva
            Audiencia audiencia = audienciaService.criarFromRequest(request);

            ctx.status(201);
            ctx.json(Map.of(
                "success", true,
                "message", "Audiência criada com sucesso",
                "dados", audiencia
            ));

            logger.info("DEBUG_AUDIENCIAS: Audiência criada - ID: {}, Processo: {}",
                audiencia.getId(), audiencia.getNumeroProcesso());

        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            // Conflito de horário
            ctx.status(409);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao criar audiência", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao criar audiência: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/audiencias/{id}
     * Atualiza uma audiência existente.
     */
    public void atualizar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            // Receber DTO com IDs
            AudienciaRequest request = ctx.bodyAsClass(AudienciaRequest.class);
            System.out.println("DEBUG_AUDIENCIAS: AudienciaController.atualizar() - ID: " + id);

            // Service converte DTO para Audiencia e atualiza
            Audiencia audiencia = audienciaService.atualizarFromRequest(id, request);

            ctx.json(Map.of(
                "success", true,
                "message", "Audiência atualizada com sucesso",
                "dados", audiencia
            ));

            logger.info("DEBUG_AUDIENCIAS: Audiência atualizada - ID: {}", id);

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
        } catch (IllegalStateException e) {
            // Conflito de horário
            ctx.status(409);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao atualizar audiência", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao atualizar audiência: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/audiencias/{id}
     * Deleta uma audiência.
     * ATENÇÃO: Participações e representações associadas serão deletadas em cascata.
     */
    public void deletar(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            System.out.println("DEBUG_AUDIENCIAS: AudienciaController.deletar() - ID: " + id);

            audienciaService.deletar(id);

            ctx.json(Map.of(
                "success", true,
                "message", "Audiência deletada com sucesso"
            ));

            logger.warn("DEBUG_AUDIENCIAS: Audiência deletada - ID: {}", id);

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
            logger.error("DEBUG_AUDIENCIAS: Erro ao deletar audiência", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao deletar audiência: " + e.getMessage()
            ));
        }
    }
}
