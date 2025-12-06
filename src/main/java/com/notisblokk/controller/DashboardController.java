package com.notisblokk.controller;

import com.notisblokk.model.NotaDTO;
import com.notisblokk.service.NotaService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller responsável pelo dashboard principal.
 *
 * <p>Exibe estatísticas e informações gerais do sistema para usuários autenticados.</p>
 *
 * <p><b>Estatísticas exibidas:</b></p>
 * <ul>
 *   <li>Total de anotações</li>
 *   <li>Anotações com prazo vencido</li>
 *   <li>Anotações urgentes (próximas do vencimento)</li>
 *   <li>Lista de anotações vencidas ou próximas do vencimento</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final NotaService notaService;

    /**
     * Construtor padrão.
     */
    public DashboardController() {
        this.notaService = new NotaService();
    }

    /**
     * GET /dashboard
     * Exibe o dashboard principal com estatísticas do sistema.
     */
    public void index(Context ctx) {
        try {
            Map<String, Object> model = new HashMap<>();

            // Adicionar atributos de sessão
            model.putAll(SessionUtil.getSessionAttributes(ctx));

            // Título
            model.put("title", "Dashboard - Notisblokk");

            // Buscar todas as notas
            List<NotaDTO> todasNotas = notaService.listarTodas();
            LocalDate hoje = LocalDate.now();

            // Calcular estatísticas de notas
            long totalNotas = todasNotas.size();

            // Notas vencidas (prazo já passou) - excluindo resolvidas/canceladas
            List<NotaDTO> notasVencidas = todasNotas.stream()
                .filter(nota -> !isNotaResolvida(nota)) // Excluir resolvidas/canceladas
                .filter(nota -> nota.getPrazoFinal() != null && nota.getPrazoFinal().isBefore(hoje))
                .collect(Collectors.toList());

            // Notas urgentes (vencendo nos próximos 7 dias) - excluindo resolvidas/canceladas
            List<NotaDTO> notasUrgentes = todasNotas.stream()
                .filter(nota -> !isNotaResolvida(nota)) // Excluir resolvidas/canceladas
                .filter(nota -> {
                    if (nota.getPrazoFinal() == null) return false;
                    long diasRestantes = ChronoUnit.DAYS.between(hoje, nota.getPrazoFinal());
                    return diasRestantes >= 0 && diasRestantes <= 7;
                })
                .collect(Collectors.toList());

            // Notas vencidas ou urgentes (para exibir na lista) - excluindo resolvidas/canceladas
            List<Map<String, Object>> notasAlerta = todasNotas.stream()
                .filter(nota -> !isNotaResolvida(nota)) // Excluir resolvidas/canceladas
                .filter(nota -> {
                    if (nota.getPrazoFinal() == null) return false;
                    long diasRestantes = ChronoUnit.DAYS.between(hoje, nota.getPrazoFinal());
                    return diasRestantes <= 7; // Vencidas ou vencendo em até 7 dias
                })
                .sorted((a, b) -> a.getPrazoFinal().compareTo(b.getPrazoFinal()))
                .limit(10) // Mostrar até 10 notas
                .map(nota -> {
                    Map<String, Object> alertaMap = new HashMap<>();
                    alertaMap.put("id", nota.getId());
                    alertaMap.put("titulo", nota.getTitulo());
                    alertaMap.put("prazoFinal", nota.getPrazoFinal().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                    long diasRestantes = ChronoUnit.DAYS.between(hoje, nota.getPrazoFinal());
                    alertaMap.put("diasRestantes", diasRestantes);

                    String statusTexto;
                    String nivel;
                    if (diasRestantes < 0) {
                        long diasAtrasados = Math.abs(diasRestantes);
                        statusTexto = diasAtrasados == 1 ? "Atrasado 1 dia" : "Atrasado " + diasAtrasados + " dias";
                        nivel = "CRITICO";
                    } else if (diasRestantes == 0) {
                        statusTexto = "Vence hoje!";
                        nivel = "URGENTE";
                    } else if (diasRestantes <= 3) {
                        statusTexto = diasRestantes == 1 ? "Vence em 1 dia" : "Vence em " + diasRestantes + " dias";
                        nivel = "URGENTE";
                    } else {
                        statusTexto = "Vence em " + diasRestantes + " dias";
                        nivel = "ALERTA";
                    }

                    alertaMap.put("statusTexto", statusTexto);
                    alertaMap.put("nivel", nivel);
                    alertaMap.put("etiqueta", nota.getEtiqueta() != null ? nota.getEtiqueta().getNome() : "Sem etiqueta");
                    alertaMap.put("status", nota.getStatus() != null ? nota.getStatus().getNome() : "Sem status");

                    return alertaMap;
                })
                .collect(Collectors.toList());

            model.put("totalNotas", totalNotas);
            model.put("notasVencidas", notasVencidas.size());
            model.put("notasUrgentes", notasUrgentes.size());
            model.put("notasAlerta", notasAlerta);

            // Status do sistema
            model.put("sistemaStatus", "Operacional");
            model.put("sistemaStatusClass", "success");

            logger.debug("Dashboard acessado por: {}", SessionUtil.getCurrentUserDisplayName(ctx));

            // Configurar charset UTF-8
            ctx.contentType("text/html; charset=utf-8");
            ctx.render("dashboard/index", model);

        } catch (Exception e) {
            logger.error("Erro ao carregar dashboard", e);
            ctx.status(500);
            ctx.result("Erro ao carregar dashboard: " + e.getMessage());
        }
    }

    /**
     * GET /api/dashboard/stats
     * Retorna estatísticas do dashboard em formato JSON (para AJAX).
     */
    public void getStats(Context ctx) {
        try {
            List<NotaDTO> todasNotas = notaService.listarTodas();
            LocalDate hoje = LocalDate.now();

            long totalNotas = todasNotas.size();
            long notasVencidas = todasNotas.stream()
                .filter(nota -> !isNotaResolvida(nota)) // Excluir resolvidas/canceladas
                .filter(nota -> nota.getPrazoFinal() != null && nota.getPrazoFinal().isBefore(hoje))
                .count();
            long notasUrgentes = todasNotas.stream()
                .filter(nota -> !isNotaResolvida(nota)) // Excluir resolvidas/canceladas
                .filter(nota -> {
                    if (nota.getPrazoFinal() == null) return false;
                    long diasRestantes = ChronoUnit.DAYS.between(hoje, nota.getPrazoFinal());
                    return diasRestantes >= 0 && diasRestantes <= 7;
                })
                .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalNotas", totalNotas);
            stats.put("notasVencidas", notasVencidas);
            stats.put("notasUrgentes", notasUrgentes);

            ctx.json(stats);

        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas", e);
            ctx.status(500);
            ctx.json(Map.of(
                "success", false,
                "message", "Erro ao obter estatísticas"
            ));
        }
    }

    /**
     * Verifica se uma nota está resolvida ou cancelada.
     *
     * <p>Notas com esses status não devem aparecer em alertas de prazo,
     * pois já foram concluídas de alguma forma.</p>
     *
     * @param nota nota a ser verificada
     * @return true se a nota está resolvida ou cancelada, false caso contrário
     */
    private boolean isNotaResolvida(NotaDTO nota) {
        if (nota.getStatus() == null || nota.getStatus().getNome() == null) {
            return false;
        }
        String statusNome = nota.getStatus().getNome().toLowerCase();
        return statusNome.contains("resolvid") || statusNome.contains("cancelad");
    }
}
