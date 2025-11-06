package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.dto.HorariosLivresRequest;
import com.notisblokk.audiencias.dto.TimeSlot;
import com.notisblokk.audiencias.service.HorariosLivresService;
import com.notisblokk.audiencias.util.DateUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para busca de horários livres para agendamento de audiências
 *
 * Endpoints:
 * - POST /api/audiencias/horarios-livres - Busca horários livres com parâmetros customizados
 * - GET /api/audiencias/horarios-livres/rapido - Busca rápida com parâmetros via query string
 */
public class HorariosLivresController {
    private static final Logger logger = LoggerFactory.getLogger(HorariosLivresController.class);

    private final HorariosLivresService service;

    public HorariosLivresController() {
        this.service = new HorariosLivresService();
    }

    /**
     * POST /api/audiencias/horarios-livres
     * Busca horários livres com parâmetros completos via JSON
     *
     * Body JSON:
     * {
     *   "dataInicio": "2025-01-15",
     *   "dataFim": "2025-01-31",
     *   "varaId": 1,
     *   "duracaoMinutos": 60,
     *   "bufferAntesMinutos": 10,
     *   "bufferDepoisMinutos": 10,
     *   "gradeMinutos": 15,
     *   "gapMinimoMinutos": 5
     * }
     *
     * @param ctx Contexto HTTP Javalin
     */
    public void buscarHorariosLivres(Context ctx) {
        try {
            logger.debug("DEBUG_AUDIENCIAS: HorariosLivresController.buscarHorariosLivres() - Recebendo requisição");

            // Parse do body JSON
            HorariosLivresRequest request = ctx.bodyAsClass(HorariosLivresRequest.class);

            logger.debug("DEBUG_AUDIENCIAS: Request recebido: {}", request);

            // Validar datas
            if (request.getDataInicio() == null || request.getDataInicio().isEmpty()) {
                throw new IllegalArgumentException("Data de início é obrigatória");
            }
            if (request.getDataFim() == null || request.getDataFim().isEmpty()) {
                throw new IllegalArgumentException("Data de fim é obrigatória");
            }
            if (request.getDuracaoMinutos() <= 0) {
                throw new IllegalArgumentException("Duração em minutos é obrigatória");
            }

            // Calcular horários livres
            List<TimeSlot> slots = service.calcularHorariosLivres(request);

            logger.debug("DEBUG_AUDIENCIAS: {} slots calculados", slots.size());

            // Retornar resposta
            ctx.status(200);
            ctx.json(Map.of(
                "success", true,
                "dados", slots,
                "message", slots.size() + " horários disponíveis encontrados",
                "total", slots.size()
            ));

        } catch (IllegalArgumentException e) {
            logger.error("DEBUG_AUDIENCIAS: Erro de validação: {}", e.getMessage());
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar horários livres", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar horários livres: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/horarios-livres/rapido
     * Busca rápida de horários livres com parâmetros via query string
     *
     * Parâmetros obrigatórios:
     * - dataInicio: dd/MM/yyyy ou dd-MM-yyyy
     * - dataFim: dd/MM/yyyy ou dd-MM-yyyy
     * - duracao: duração em minutos
     *
     * Parâmetros opcionais:
     * - varaId: ID da vara (opcional, null = todas as varas)
     * - buffer: buffer antes/depois em minutos (padrão: 10)
     * - grade: grade de arredondamento em minutos (padrão: 15)
     *
     * @param ctx Contexto HTTP Javalin
     */
    public void buscarHorariosLivresRapido(Context ctx) {
        try {
            logger.debug("DEBUG_AUDIENCIAS: HorariosLivresController.buscarHorariosLivresRapido()");

            // Parâmetros obrigatórios
            String dataInicioStr = ctx.queryParam("dataInicio");
            String dataFimStr = ctx.queryParam("dataFim");
            String duracaoStr = ctx.queryParam("duracao");

            if (dataInicioStr == null || dataFimStr == null || duracaoStr == null) {
                throw new IllegalArgumentException(
                    "Parâmetros obrigatórios: dataInicio, dataFim, duracao"
                );
            }

            // Parse de parâmetros
            int duracao = Integer.parseInt(duracaoStr);

            // Parâmetros opcionais
            String varaIdStr = ctx.queryParam("varaId");

            String bufferStr = ctx.queryParam("buffer");
            int buffer = bufferStr != null ? Integer.parseInt(bufferStr) : 10;

            String gradeStr = ctx.queryParam("grade");
            int grade = gradeStr != null ? Integer.parseInt(gradeStr) : 15;

            // Montar request (datas já estão como String)
            HorariosLivresRequest request = new HorariosLivresRequest();
            request.setDataInicio(dataInicioStr);
            request.setDataFim(dataFimStr);
            request.setVaraId(varaIdStr != null ? varaIdStr : "");
            request.setDuracaoMinutos(duracao);
            request.setBufferAntesMinutos(buffer);
            request.setBufferDepoisMinutos(buffer);
            request.setGradeMinutos(grade);
            request.setGapMinimoMinutos(5);

            logger.debug("DEBUG_AUDIENCIAS: Request montado: {}", request);

            // Calcular horários livres
            List<TimeSlot> slots = service.calcularHorariosLivres(request);

            logger.debug("DEBUG_AUDIENCIAS: {} slots calculados", slots.size());

            // Retornar resposta
            ctx.status(200);
            ctx.json(Map.of(
                "success", true,
                "dados", slots,
                "message", slots.size() + " horários disponíveis encontrados",
                "total", slots.size()
            ));

        } catch (IllegalArgumentException e) {
            logger.error("DEBUG_AUDIENCIAS: Erro de validação: {}", e.getMessage());
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", e.getMessage()
            ));

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar horários livres", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar horários livres: " + e.getMessage()
            ));
        }
    }
}
