package com.notisblokk.scheduler;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gerenciador do Quartz Scheduler para jobs de tarefas agendadas.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Inicializar o Quartz Scheduler</li>
 *   <li>Agendar jobs (AlertaEmailJob, LimpezaJob, etc.)</li>
 *   <li>Gerenciar ciclo de vida do scheduler</li>
 * </ul>
 *
 * <p><b>Jobs configurados:</b></p>
 * <ul>
 *   <li><b>AlertaEmailJob:</b> Envia alertas por email a cada 1 hora</li>
 *   <li><b>LimpezaAlertasJob:</b> Limpa alertas antigos diariamente às 3h</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-27
 */
public class QuartzSchedulerManager {

    private static final Logger logger = LoggerFactory.getLogger(QuartzSchedulerManager.class);
    private Scheduler scheduler;

    /**
     * Inicializa e configura o Quartz Scheduler.
     *
     * @throws SchedulerException se houver erro ao inicializar
     */
    public void iniciar() throws SchedulerException {
        logger.info("Inicializando Quartz Scheduler...");

        // Criar scheduler
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        scheduler = schedulerFactory.getScheduler();

        // Configurar jobs
        configurarJobAlertaEmail();
        configurarJobLimpezaAlertas();

        // Iniciar scheduler
        scheduler.start();

        logger.info("✅ Quartz Scheduler iniciado com sucesso");
    }

    /**
     * Configura o job de envio de alertas por email.
     * Executa a cada 1 hora.
     */
    private void configurarJobAlertaEmail() throws SchedulerException {
        // Definir job
        JobDetail job = JobBuilder.newJob(AlertaEmailJob.class)
            .withIdentity("alertaEmailJob", "alertas")
            .withDescription("Envia alertas de prazos por email")
            .build();

        // Definir trigger: executa a cada 1 hora
        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity("alertaEmailTrigger", "alertas")
            .withDescription("Trigger para alertas por email a cada 1 hora")
            .startNow()
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInHours(1)
                    .repeatForever()
            )
            .build();

        // Agendar job
        scheduler.scheduleJob(job, trigger);

        logger.info("📧 Job de alertas por email agendado: executa a cada 1 hora");
    }

    /**
     * Configura o job de limpeza de alertas antigos.
     * Executa diariamente às 3h da manhã.
     */
    private void configurarJobLimpezaAlertas() throws SchedulerException {
        // Definir job
        JobDetail job = JobBuilder.newJob(LimpezaAlertasJob.class)
            .withIdentity("limpezaAlertasJob", "manutencao")
            .withDescription("Remove alertas enviados há mais de 30 dias")
            .build();

        // Definir trigger: executa diariamente às 3h
        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity("limpezaAlertasTrigger", "manutencao")
            .withDescription("Trigger para limpeza diária de alertas às 3h")
            .withSchedule(
                CronScheduleBuilder.dailyAtHourAndMinute(3, 0)
            )
            .build();

        // Agendar job
        scheduler.scheduleJob(job, trigger);

        logger.info("🧹 Job de limpeza de alertas agendado: executa diariamente às 3h");
    }

    /**
     * Para o scheduler de forma controlada.
     * Aguarda jobs em execução finalizarem.
     */
    public void parar() {
        if (scheduler != null) {
            try {
                logger.info("Parando Quartz Scheduler...");
                scheduler.shutdown(true); // true = aguardar jobs completarem
                logger.info("✅ Quartz Scheduler parado com sucesso");

            } catch (SchedulerException e) {
                logger.error("Erro ao parar Quartz Scheduler", e);
            }
        }
    }

    /**
     * Para o scheduler imediatamente, interrompendo jobs em execução.
     */
    public void pararImediatamente() {
        if (scheduler != null) {
            try {
                logger.info("Parando Quartz Scheduler imediatamente...");
                scheduler.shutdown(false); // false = não aguardar jobs
                logger.info("✅ Quartz Scheduler parado");

            } catch (SchedulerException e) {
                logger.error("Erro ao parar Quartz Scheduler", e);
            }
        }
    }

    /**
     * Verifica se o scheduler está em execução.
     *
     * @return true se está rodando, false caso contrário
     */
    public boolean estaRodando() {
        try {
            return scheduler != null && scheduler.isStarted() && !scheduler.isShutdown();
        } catch (SchedulerException e) {
            logger.error("Erro ao verificar status do scheduler", e);
            return false;
        }
    }

    /**
     * Executa manualmente o job de alertas por email (útil para testes).
     */
    public void executarAlertasManualmente() {
        try {
            logger.info("Executando job de alertas manualmente...");

            JobKey jobKey = new JobKey("alertaEmailJob", "alertas");
            scheduler.triggerJob(jobKey);

            logger.info("✅ Job de alertas disparado manualmente");

        } catch (SchedulerException e) {
            logger.error("Erro ao executar job manualmente", e);
        }
    }
}
