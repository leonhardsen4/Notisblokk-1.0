package com.notisblokk.scheduler;

import com.notisblokk.model.TarefaDTO;
import com.notisblokk.model.User;
import com.notisblokk.repository.AlertaEnviadoRepository;
import com.notisblokk.repository.TarefaRepository;
import com.notisblokk.repository.UserRepository;
import com.notisblokk.service.ConfiguracaoService;
import com.notisblokk.service.EmailService;
import com.notisblokk.service.TarefaService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Job do Quartz Scheduler para envio automático de alertas por email.
 *
 * <p>Executa periodicamente (padrão: a cada 1 hora) e:</p>
 * <ul>
 *   <li>Busca todos os usuários ativos</li>
 *   <li>Verifica configuração de notificação por email de cada usuário</li>
 *   <li>Identifica tarefas com prazos próximos</li>
 *   <li>Envia emails de alerta respeitando níveis configurados</li>
 *   <li>Evita duplicatas usando controle de envios</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-27
 */
public class AlertaEmailJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(AlertaEmailJob.class);

    private final UserRepository userRepository;
    private final TarefaService tarefaService;
    private final TarefaRepository tarefaRepository;
    private final EmailService emailService;
    private final ConfiguracaoService configuracaoService;
    private final AlertaEnviadoRepository alertaEnviadoRepository;

    /**
     * Construtor padrão (necessário para o Quartz).
     */
    public AlertaEmailJob() {
        this.userRepository = new UserRepository();
        this.tarefaService = new TarefaService();
        this.tarefaRepository = new TarefaRepository();
        this.emailService = new EmailService();
        this.configuracaoService = new ConfiguracaoService();
        this.alertaEnviadoRepository = new AlertaEnviadoRepository();
    }

    /**
     * Método executado pelo Quartz Scheduler.
     *
     * @param context contexto de execução do job
     * @throws JobExecutionException se houver erro na execução
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("========================================");
        logger.info("Iniciando job de envio de alertas por email");
        logger.info("========================================");

        try {
            int totalEnviados = processarAlertasParaTodosUsuarios();

            logger.info("========================================");
            logger.info("Job finalizado com sucesso: {} emails enviados", totalEnviados);
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("Erro ao executar job de alertas por email", e);
            throw new JobExecutionException("Erro ao processar alertas: " + e.getMessage(), e);
        }
    }

    /**
     * Processa alertas para todos os usuários ativos.
     *
     * @return número total de emails enviados
     */
    private int processarAlertasParaTodosUsuarios() throws Exception {
        int totalEnviados = 0;

        // Buscar todos os usuários ativos
        List<User> usuarios = userRepository.buscarTodos();
        logger.info("Processando alertas para {} usuários", usuarios.size());

        for (User usuario : usuarios) {
            // Ignorar usuários inativos
            if (!usuario.isActive()) {
                logger.debug("Usuário {} está inativo, pulando...", usuario.getUsername());
                continue;
            }

            // Verificar se usuário tem notificações por email habilitadas
            if (!usuarioTemNotificacaoEmailHabilitada(usuario.getId())) {
                logger.debug("Usuário {} tem notificações por email desabilitadas", usuario.getUsername());
                continue;
            }

            try {
                int enviados = processarAlertasParaUsuario(usuario);
                totalEnviados += enviados;

            } catch (Exception e) {
                logger.error("Erro ao processar alertas para usuário {}", usuario.getUsername(), e);
                // Continua processando outros usuários mesmo se um falhar
            }
        }

        return totalEnviados;
    }

    /**
     * Processa alertas para um usuário específico.
     *
     * @param usuario usuário a processar
     * @return número de emails enviados para este usuário
     */
    private int processarAlertasParaUsuario(User usuario) throws Exception {
        int enviados = 0;

        // Buscar todas as tarefas do usuário
        List<TarefaDTO> tarefas = tarefaRepository.buscarPorUsuarioId(usuario.getId());

        if (tarefas.isEmpty()) {
            logger.debug("Usuário {} não possui tarefas", usuario.getUsername());
            return 0;
        }

        logger.debug("Verificando {} tarefas do usuário {}", tarefas.size(), usuario.getUsername());

        // Buscar configurações de dias para alertas
        Map<String, String> config = configuracaoService.buscarConfiguracoes(usuario.getId());
        int diasCritico = Integer.parseInt(config.getOrDefault("notif_dias_critico", "0"));
        int diasUrgente = Integer.parseInt(config.getOrDefault("notif_dias_urgente", "3"));
        int diasAtencao = Integer.parseInt(config.getOrDefault("notif_dias_atencao", "5"));

        for (TarefaDTO tarefa : tarefas) {
            // Ignorar tarefas resolvidas ou canceladas
            String statusNome = tarefa.getStatus().getNome().toLowerCase();
            if (statusNome.contains("resolvid") || statusNome.contains("cancelad")) {
                continue;
            }

            // Verificar se tarefa precisa de alerta
            if (tarefa.getDiasRestantes() == null) {
                continue;
            }

            long diasRestantes = tarefa.getDiasRestantes();
            String nivel = determinarNivelAlerta(diasRestantes, diasCritico, diasUrgente, diasAtencao);

            if (nivel == null) {
                // Tarefa não precisa de alerta ainda
                continue;
            }

            // Verificar se já foi enviado alerta hoje
            if (alertaEnviadoRepository.alertaJaEnviado(usuario.getId(), tarefa.getId(), nivel)) {
                logger.debug("Alerta para tarefa {} (nível {}) já foi enviado hoje", tarefa.getTitulo(), nivel);
                continue;
            }

            // Enviar email de alerta
            try {
                enviarEmailAlerta(usuario, tarefa, diasRestantes);

                // Registrar envio
                alertaEnviadoRepository.registrarEnvio(usuario.getId(), tarefa.getId(), nivel, (int) diasRestantes);

                enviados++;

                logger.info("Email enviado: {} - Tarefa: {} ({} dias)",
                           usuario.getEmail(), tarefa.getTitulo(), diasRestantes);

            } catch (Exception e) {
                logger.error("Erro ao enviar email para tarefa {}: {}", tarefa.getTitulo(), e.getMessage());
            }
        }

        return enviados;
    }

    /**
     * Determina o nível de alerta baseado nos dias restantes e configurações do usuário.
     *
     * @param diasRestantes dias até o prazo
     * @param diasCritico limite para alerta crítico
     * @param diasUrgente limite para alerta urgente
     * @param diasAtencao limite para alerta de atenção
     * @return nível do alerta ou null se não precisa alertar
     */
    private String determinarNivelAlerta(long diasRestantes, int diasCritico, int diasUrgente, int diasAtencao) {
        if (diasRestantes < 0 || diasRestantes <= diasCritico) {
            return "CRITICO";
        } else if (diasRestantes <= diasUrgente) {
            return "URGENTE";
        } else if (diasRestantes <= diasAtencao) {
            return "ATENCAO";
        } else {
            return null; // Não precisa alertar ainda
        }
    }

    /**
     * Envia email de alerta para o usuário sobre uma tarefa específica.
     *
     * @param usuario usuário destinatário
     * @param tarefa tarefa sobre a qual alertar
     * @param diasRestantes dias restantes até o prazo
     */
    private void enviarEmailAlerta(User usuario, TarefaDTO tarefa, long diasRestantes) throws Exception {
        String nomeUsuario = usuario.getFullName() != null ? usuario.getFullName() : usuario.getUsername();

        emailService.enviarEmailAlertaNota(
            usuario.getEmail(),
            nomeUsuario,
            tarefa.getTitulo(),
            tarefa.getPrazoFinalFormatado(),
            (int) diasRestantes
        );
    }

    /**
     * Verifica se o usuário tem notificações por email habilitadas.
     *
     * @param userId ID do usuário
     * @return true se habilitado, false caso contrário
     */
    private boolean usuarioTemNotificacaoEmailHabilitada(Long userId) {
        try {
            String notifEmail = configuracaoService.buscarConfiguracao(userId, "notif_email");
            return "true".equalsIgnoreCase(notifEmail);
        } catch (Exception e) {
            logger.error("Erro ao verificar configuração de notificação para userId: {}", userId, e);
            return false; // Default: desabilitado em caso de erro
        }
    }
}
