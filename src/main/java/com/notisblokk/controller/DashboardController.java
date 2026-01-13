package com.notisblokk.controller;

import com.notisblokk.model.TarefaDTO;
import com.notisblokk.service.TarefaService;
import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
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
 *   <li>Total de tarefas</li>
 *   <li>Tarefas com prazo vencido</li>
 *   <li>Tarefas urgentes (próximas do vencimento)</li>
 *   <li>Lista de tarefas vencidas ou próximas do vencimento</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final TarefaService tarefaService;

    /**
     * Construtor padrão.
     */
    public DashboardController() {
        this.tarefaService = new TarefaService();
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

            // Buscar todas as tarefas
            List<TarefaDTO> todasTarefas = tarefaService.listarTodas();
            LocalDate hoje = LocalDate.now();

            // Calcular estatísticas de tarefas
            long totalTarefas = todasTarefas.size();

            // Tarefas vencidas (prazo já passou) - excluindo resolvidas/canceladas
            List<TarefaDTO> tarefasVencidas = todasTarefas.stream()
                .filter(tarefa -> !isTarefaResolvida(tarefa)) // Excluir resolvidas/canceladas
                .filter(tarefa -> tarefa.getPrazoFinal() != null && tarefa.getPrazoFinal().isBefore(hoje))
                .collect(Collectors.toList());

            // Tarefas urgentes (vencendo nos próximos 7 dias) - excluindo resolvidas/canceladas
            List<TarefaDTO> tarefasUrgentes = todasTarefas.stream()
                .filter(tarefa -> !isTarefaResolvida(tarefa)) // Excluir resolvidas/canceladas
                .filter(tarefa -> {
                    if (tarefa.getPrazoFinal() == null) return false;
                    long diasRestantes = ChronoUnit.DAYS.between(hoje, tarefa.getPrazoFinal());
                    return diasRestantes >= 0 && diasRestantes <= 7;
                })
                .collect(Collectors.toList());

            // Tarefas vencidas ou urgentes (para exibir na lista) - excluindo resolvidas/canceladas
            List<Map<String, Object>> tarefasAlerta = todasTarefas.stream()
                .filter(tarefa -> !isTarefaResolvida(tarefa)) // Excluir resolvidas/canceladas
                .filter(tarefa -> {
                    if (tarefa.getPrazoFinal() == null) return false;
                    long diasRestantes = ChronoUnit.DAYS.between(hoje, tarefa.getPrazoFinal());
                    return diasRestantes <= 7; // Vencidas ou vencendo em até 7 dias
                })
                .sorted((a, b) -> a.getPrazoFinal().compareTo(b.getPrazoFinal()))
                .limit(10) // Mostrar até 10 tarefas
                .map(tarefa -> {
                    Map<String, Object> alertaMap = new HashMap<>();
                    alertaMap.put("id", tarefa.getId());
                    alertaMap.put("titulo", tarefa.getTitulo());
                    alertaMap.put("prazoFinal", tarefa.getPrazoFinal().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                    long diasRestantes = ChronoUnit.DAYS.between(hoje, tarefa.getPrazoFinal());
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
                    alertaMap.put("etiqueta", tarefa.getEtiqueta() != null ? tarefa.getEtiqueta().getNome() : "Sem etiqueta");
                    alertaMap.put("status", tarefa.getStatus() != null ? tarefa.getStatus().getNome() : "Sem status");

                    return alertaMap;
                })
                .collect(Collectors.toList());

            model.put("totalNotas", totalTarefas);
            model.put("notasVencidas", tarefasVencidas.size());
            model.put("notasUrgentes", tarefasUrgentes.size());
            model.put("notasAlerta", tarefasAlerta);

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
            List<TarefaDTO> todasTarefas = tarefaService.listarTodas();
            LocalDate hoje = LocalDate.now();

            long totalTarefas = todasTarefas.size();
            long tarefasVencidas = todasTarefas.stream()
                .filter(tarefa -> !isTarefaResolvida(tarefa)) // Excluir resolvidas/canceladas
                .filter(tarefa -> tarefa.getPrazoFinal() != null && tarefa.getPrazoFinal().isBefore(hoje))
                .count();
            long tarefasUrgentes = todasTarefas.stream()
                .filter(tarefa -> !isTarefaResolvida(tarefa)) // Excluir resolvidas/canceladas
                .filter(tarefa -> {
                    if (tarefa.getPrazoFinal() == null) return false;
                    long diasRestantes = ChronoUnit.DAYS.between(hoje, tarefa.getPrazoFinal());
                    return diasRestantes >= 0 && diasRestantes <= 7;
                })
                .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalNotas", totalTarefas);
            stats.put("notasVencidas", tarefasVencidas);
            stats.put("notasUrgentes", tarefasUrgentes);

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
     * Verifica se uma tarefa está resolvida ou cancelada.
     *
     * <p>Tarefas com esses status não devem aparecer em alertas de prazo,
     * pois já foram concluídas de alguma forma.</p>
     *
     * @param tarefa tarefa a ser verificada
     * @return true se a tarefa está resolvida ou cancelada, false caso contrário
     */
    private boolean isTarefaResolvida(TarefaDTO tarefa) {
        if (tarefa.getStatus() == null || tarefa.getStatus().getNome() == null) {
            return false;
        }
        String statusNome = tarefa.getStatus().getNome().toLowerCase();
        return statusNome.contains("resolvid") || statusNome.contains("cancelad");
    }
}
