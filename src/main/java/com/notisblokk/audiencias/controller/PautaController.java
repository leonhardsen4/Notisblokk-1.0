package com.notisblokk.audiencias.controller;

import com.notisblokk.audiencias.model.Audiencia;
import com.notisblokk.audiencias.service.AudienciaService;
import com.notisblokk.audiencias.util.DateUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para gerenciamento de Pauta de Audiências.
 *
 * <p>Endpoints disponíveis:</p>
 * <ul>
 *   <li>GET /api/audiencias/pauta - Pauta do dia (audiências de hoje)</li>
 *   <li>GET /api/audiencias/pauta/{data} - Pauta de uma data específica</li>
 *   <li>GET /api/audiencias/pauta/vara/{varaId} - Pauta do dia filtrada por vara</li>
 *   <li>GET /api/audiencias/pauta/{data}/vara/{varaId} - Pauta de uma data e vara específicas</li>
 * </ul>
 */
public class PautaController {

    private static final Logger logger = LoggerFactory.getLogger(PautaController.class);
    private final AudienciaService audienciaService;

    public PautaController() {
        this.audienciaService = new AudienciaService();
    }

    /**
     * GET /api/audiencias/pauta
     * Retorna a pauta do dia (audiências de hoje).
     */
    public void pautaDeHoje(Context ctx) {
        try {
            LocalDate hoje = DateUtil.hoje();
            System.out.println("DEBUG_AUDIENCIAS: PautaController.pautaDeHoje() - Data: " + DateUtil.formatDate(hoje));

            List<Audiencia> audiencias = audienciaService.buscarPorData(hoje);

            // Ordenar por horário de início
            audiencias.sort((a1, a2) -> {
                if (a1.getHorarioInicio() == null) return 1;
                if (a2.getHorarioInicio() == null) return -1;
                return a1.getHorarioInicio().compareTo(a2.getHorarioInicio());
            });

            ctx.json(Map.of(
                "success", true,
                "data", DateUtil.formatDate(hoje),
                "diaSemana", hoje.getDayOfWeek().toString(),
                "total", audiencias.size(),
                "audiencias", audiencias
            ));

            logger.debug("DEBUG_AUDIENCIAS: Pauta de hoje: {} audiência(s)", audiencias.size());

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar pauta de hoje", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar pauta: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/pauta/{data}
     * Retorna a pauta de uma data específica (formato: dd/MM/yyyy ou dd-MM-yyyy).
     */
    public void pautaPorData(Context ctx) {
        try {
            String dataStr = ctx.pathParam("data");
            // Aceitar tanto dd/MM/yyyy quanto dd-MM-yyyy
            dataStr = dataStr.replace("-", "/");

            System.out.println("DEBUG_AUDIENCIAS: PautaController.pautaPorData() - Data: " + dataStr);

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

            // Ordenar por horário de início
            audiencias.sort((a1, a2) -> {
                if (a1.getHorarioInicio() == null) return 1;
                if (a2.getHorarioInicio() == null) return -1;
                return a1.getHorarioInicio().compareTo(a2.getHorarioInicio());
            });

            ctx.json(Map.of(
                "success", true,
                "data", DateUtil.formatDate(data),
                "diaSemana", data.getDayOfWeek().toString(),
                "total", audiencias.size(),
                "audiencias", audiencias
            ));

            logger.debug("DEBUG_AUDIENCIAS: Pauta de {}: {} audiência(s)", dataStr, audiencias.size());

        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar pauta por data", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar pauta: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/pauta/vara/{varaId}
     * Retorna a pauta do dia filtrada por vara.
     */
    public void pautaDeHojePorVara(Context ctx) {
        try {
            Long varaId = Long.parseLong(ctx.pathParam("varaId"));
            LocalDate hoje = DateUtil.hoje();

            System.out.println("DEBUG_AUDIENCIAS: PautaController.pautaDeHojePorVara() - " +
                "Data: " + DateUtil.formatDate(hoje) + ", VaraID: " + varaId);

            List<Audiencia> todasAudiencias = audienciaService.buscarPorData(hoje);

            // Filtrar por vara
            List<Audiencia> audienciasDaVara = todasAudiencias.stream()
                .filter(a -> a.getVara() != null && a.getVara().getId().equals(varaId))
                .sorted((a1, a2) -> {
                    if (a1.getHorarioInicio() == null) return 1;
                    if (a2.getHorarioInicio() == null) return -1;
                    return a1.getHorarioInicio().compareTo(a2.getHorarioInicio());
                })
                .toList();

            ctx.json(Map.of(
                "success", true,
                "data", DateUtil.formatDate(hoje),
                "diaSemana", hoje.getDayOfWeek().toString(),
                "varaId", varaId,
                "total", audienciasDaVara.size(),
                "audiencias", audienciasDaVara
            ));

            logger.debug("DEBUG_AUDIENCIAS: Pauta de hoje para vara {}: {} audiência(s)", varaId, audienciasDaVara.size());

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID de vara inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar pauta por vara", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar pauta: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/audiencias/pauta/{data}/vara/{varaId}
     * Retorna a pauta de uma data e vara específicas.
     */
    public void pautaPorDataEVara(Context ctx) {
        try {
            String dataStr = ctx.pathParam("data");
            dataStr = dataStr.replace("-", "/");
            Long varaId = Long.parseLong(ctx.pathParam("varaId"));

            System.out.println("DEBUG_AUDIENCIAS: PautaController.pautaPorDataEVara() - " +
                "Data: " + dataStr + ", VaraID: " + varaId);

            LocalDate data = DateUtil.parseDate(dataStr);
            if (data == null) {
                ctx.status(400);
                ctx.json(Map.of(
                    "success", false,
                    "message", "Data inválida. Use o formato: dd/MM/yyyy"
                ));
                return;
            }

            List<Audiencia> todasAudiencias = audienciaService.buscarPorData(data);

            // Filtrar por vara
            List<Audiencia> audienciasDaVara = todasAudiencias.stream()
                .filter(a -> a.getVara() != null && a.getVara().getId().equals(varaId))
                .sorted((a1, a2) -> {
                    if (a1.getHorarioInicio() == null) return 1;
                    if (a2.getHorarioInicio() == null) return -1;
                    return a1.getHorarioInicio().compareTo(a2.getHorarioInicio());
                })
                .toList();

            ctx.json(Map.of(
                "success", true,
                "data", DateUtil.formatDate(data),
                "diaSemana", data.getDayOfWeek().toString(),
                "varaId", varaId,
                "total", audienciasDaVara.size(),
                "audiencias", audienciasDaVara
            ));

            logger.debug("DEBUG_AUDIENCIAS: Pauta de {} para vara {}: {} audiência(s)",
                dataStr, varaId, audienciasDaVara.size());

        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.json(Map.of(
                "success", false,
                "message", "ID de vara inválido"
            ));
        } catch (Exception e) {
            logger.error("DEBUG_AUDIENCIAS: Erro ao buscar pauta por data e vara", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao buscar pauta: " + e.getMessage()
            ));
        }
    }
}
