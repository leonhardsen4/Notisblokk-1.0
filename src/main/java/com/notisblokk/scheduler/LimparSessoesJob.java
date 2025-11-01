package com.notisblokk.scheduler;

import com.notisblokk.service.SessionService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job do Quartz Scheduler para limpeza automática de sessões expiradas.
 *
 * <p>Executa periodicamente (padrão: a cada 1 hora) e:</p>
 * <ul>
 *   <li>Identifica sessões ativas que excederam o timeout de inatividade</li>
 *   <li>Marca essas sessões como EXPIRED</li>
 *   <li>Registra logout_time automático</li>
 *   <li>Libera recursos do sistema</li>
 * </ul>
 *
 * <p><b>Benefícios:</b></p>
 * <ul>
 *   <li>Segurança: Previne sessões órfãs/esquecidas</li>
 *   <li>Performance: Evita acúmulo excessivo de sessões ativas</li>
 *   <li>Auditoria: Mantém histórico limpo e organizado</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-27
 */
public class LimparSessoesJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(LimparSessoesJob.class);

    private final SessionService sessionService;

    /**
     * Construtor padrão (necessário para o Quartz).
     */
    public LimparSessoesJob() {
        this.sessionService = new SessionService();
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
        logger.info("Iniciando job de limpeza de sessões expiradas");
        logger.info("========================================");

        try {
            // Obter timeout do SessionService
            int timeoutMinutes = sessionService.getDefaultSessionTimeout();
            logger.info("Timeout de sessão configurado: {} minutos", timeoutMinutes);

            // Contar sessões ativas antes da limpeza
            long sessoesAtivasAntes = sessionService.contarSessoesAtivas();
            logger.info("Sessões ativas antes da limpeza: {}", sessoesAtivasAntes);

            // Executar limpeza
            int sessoesExpiradas = sessionService.expirarSessoesAntigas(timeoutMinutes);

            // Contar sessões ativas após a limpeza
            long sessoesAtivasDepois = sessionService.contarSessoesAtivas();
            logger.info("Sessões ativas após a limpeza: {}", sessoesAtivasDepois);

            logger.info("========================================");
            if (sessoesExpiradas > 0) {
                logger.info("Job finalizado com sucesso: {} sessões expiradas", sessoesExpiradas);
            } else {
                logger.info("Job finalizado: nenhuma sessão precisou ser expirada");
            }
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("Erro ao executar job de limpeza de sessões", e);
            throw new JobExecutionException("Erro ao limpar sessões expiradas: " + e.getMessage(), e);
        }
    }
}
