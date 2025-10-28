package com.notisblokk.scheduler;

import com.notisblokk.repository.AlertaEnviadoRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job do Quartz para limpeza de registros antigos de alertas enviados.
 *
 * <p>Executa diariamente às 3h da manhã e remove registros de alertas
 * enviados há mais de 30 dias, mantendo a base de dados otimizada.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-27
 */
public class LimpezaAlertasJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(LimpezaAlertasJob.class);

    private final AlertaEnviadoRepository alertaEnviadoRepository;

    /**
     * Construtor padrão (necessário para o Quartz).
     */
    public LimpezaAlertasJob() {
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
        logger.info("Iniciando job de limpeza de alertas antigos");
        logger.info("========================================");

        try {
            alertaEnviadoRepository.limparAlertasAntigos();

            logger.info("========================================");
            logger.info("Job de limpeza finalizado com sucesso");
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("Erro ao executar job de limpeza de alertas", e);
            throw new JobExecutionException("Erro na limpeza de alertas: " + e.getMessage(), e);
        }
    }
}
