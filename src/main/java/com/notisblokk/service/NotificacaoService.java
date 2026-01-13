package com.notisblokk.service;

import com.notisblokk.model.TarefaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável por gerar notificações e alertas sobre tarefas.
 *
 * <p>Analisa tarefas pendentes e gera alertas baseados nos prazos finais,
 * classificando por níveis de urgência.</p>
 *
 * <p><b>Níveis de alerta:</b></p>
 * <ul>
 *   <li>CRÍTICO: Tarefas atrasadas (prazo vencido)</li>
 *   <li>URGENTE: Vence em 0-1 dia</li>
 *   <li>ATENÇÃO: Vence em 2-3 dias</li>
 *   <li>AVISO: Vence em 4-5 dias</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class NotificacaoService {

    private static final Logger logger = LoggerFactory.getLogger(NotificacaoService.class);
    private final TarefaService tarefaService;

    /**
     * Construtor padrão.
     */
    public NotificacaoService() {
        this.tarefaService = new TarefaService();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param tarefaService serviço de tarefas
     */
    public NotificacaoService(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    /**
     * Gera alertas sobre tarefas pendentes, ordenados por urgência.
     *
     * <p>Filtra apenas tarefas com status pendente (não inclui "Resolvido" ou "Cancelado")
     * e classifica por nível de urgência baseado nos dias restantes até o prazo.</p>
     *
     * @return List<Map<String, Object>> lista de alertas ordenada por urgência
     * @throws Exception se houver erro ao gerar alertas
     */
    public List<Map<String, Object>> gerarAlertas() throws Exception {
        logger.info("Gerando alertas de tarefas");

        try {
            List<TarefaDTO> todasTarefas = tarefaService.listarTodas();
            List<Map<String, Object>> alertas = new ArrayList<>();

            // Filtrar apenas tarefas pendentes (excluir Resolvido e Cancelado)
            for (TarefaDTO tarefa : todasTarefas) {
                String statusNome = tarefa.getStatus().getNome().toLowerCase();

                // Ignorar tarefas resolvidas ou canceladas
                if (statusNome.contains("resolvid") || statusNome.contains("cancelad")) {
                    continue;
                }

                Map<String, Object> alerta = criarAlerta(tarefa);
                if (alerta != null) {
                    alertas.add(alerta);
                }
            }

            // Ordenar por urgência (prioridade menor = mais urgente)
            alertas.sort(Comparator.comparingInt(a -> (Integer) a.get("prioridade")));

            logger.info("Gerados {} alertas", alertas.size());
            return alertas;

        } catch (Exception e) {
            logger.error("Erro ao gerar alertas", e);
            throw new Exception("Erro ao gerar alertas: " + e.getMessage(), e);
        }
    }

    /**
     * Cria um alerta individual para uma tarefa, classificando por urgência.
     *
     * @param tarefa tarefa a ser analisada
     * @return Map<String, Object> alerta criado, ou null se tarefa não precisa de alerta
     */
    private Map<String, Object> criarAlerta(TarefaDTO tarefa) {
        if (tarefa.getDiasRestantes() == null) {
            return null;
        }

        long diasRestantes = tarefa.getDiasRestantes();
        String nivel;
        String cor;
        int prioridade;
        String mensagem;

        // Classificar por urgência
        if (diasRestantes < 0) {
            // CRÍTICO - Atrasado
            nivel = "CRÍTICO";
            cor = "#EF4444"; // Vermelho escuro
            prioridade = 1;
            long diasAtrasado = Math.abs(diasRestantes);
            mensagem = String.format("Atrasada há %d dia(s)!", diasAtrasado);

        } else if (diasRestantes <= 1) {
            // URGENTE - Vence hoje ou amanhã
            nivel = "URGENTE";
            cor = "#DC2626"; // Vermelho
            prioridade = 2;
            mensagem = diasRestantes == 0 ? "Vence HOJE!" : "Vence AMANHÃ!";

        } else if (diasRestantes <= 3) {
            // ATENÇÃO - Vence em 2-3 dias
            nivel = "ATENÇÃO";
            cor = "#F97316"; // Laranja
            prioridade = 3;
            mensagem = String.format("Vence em %d dias", diasRestantes);

        } else if (diasRestantes <= 5) {
            // AVISO - Vence em 4-5 dias
            nivel = "AVISO";
            cor = "#F59E0B"; // Amarelo/Laranja
            prioridade = 4;
            mensagem = String.format("Vence em %d dias", diasRestantes);

        } else {
            // Sem alerta para tarefas com mais de 5 dias
            return null;
        }

        // Criar objeto de alerta
        Map<String, Object> alerta = new HashMap<>();
        alerta.put("id", tarefa.getId());
        alerta.put("titulo", tarefa.getTitulo());
        alerta.put("etiqueta", tarefa.getEtiqueta().getNome());
        alerta.put("status", tarefa.getStatus().getNome());
        alerta.put("statusCor", tarefa.getStatus().getCorHex());
        alerta.put("prazoFinal", tarefa.getPrazoFinalFormatado());
        alerta.put("diasRestantes", diasRestantes);
        alerta.put("nivel", nivel);
        alerta.put("cor", cor);
        alerta.put("prioridade", prioridade);
        alerta.put("mensagem", mensagem);

        return alerta;
    }

    /**
     * Obtém estatísticas de alertas por nível de urgência.
     *
     * @return Map<String, Long> contadores de alertas por nível
     * @throws Exception se houver erro ao gerar estatísticas
     */
    public Map<String, Long> obterEstatisticasAlertas() throws Exception {
        logger.info("Gerando estatísticas de alertas");

        try {
            List<Map<String, Object>> alertas = gerarAlertas();
            Map<String, Long> estatisticas = new HashMap<>();

            estatisticas.put("critico", 0L);
            estatisticas.put("urgente", 0L);
            estatisticas.put("atencao", 0L);
            estatisticas.put("aviso", 0L);
            estatisticas.put("total", (long) alertas.size());

            for (Map<String, Object> alerta : alertas) {
                String nivel = (String) alerta.get("nivel");
                String key = nivel.toLowerCase();
                estatisticas.put(key, estatisticas.getOrDefault(key, 0L) + 1);
            }

            return estatisticas;

        } catch (Exception e) {
            logger.error("Erro ao gerar estatísticas de alertas", e);
            throw new Exception("Erro ao gerar estatísticas: " + e.getMessage(), e);
        }
    }
}
