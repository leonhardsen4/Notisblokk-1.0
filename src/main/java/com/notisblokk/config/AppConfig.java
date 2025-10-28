package com.notisblokk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuração centralizada da aplicação.
 *
 * <p>Carrega e disponibiliza as propriedades definidas em application.properties,
 * fornecendo valores padrão caso alguma propriedade não esteja definida.</p>
 *
 * <p><b>Propriedades disponíveis:</b></p>
 * <ul>
 *   <li>server.port: Porta do servidor (padrão: 8080)</li>
 *   <li>server.host: Host do servidor (padrão: localhost)</li>
 *   <li>database.path: Caminho do banco de dados (padrão: ./notisblokk.db)</li>
 *   <li>session.timeout.minutes: Timeout de sessão em minutos (padrão: 30)</li>
 *   <li>app.name: Nome da aplicação (padrão: Notisblokk)</li>
 *   <li>app.version: Versão da aplicação (padrão: 1.0)</li>
 *   <li>app.timezone: Fuso horário (padrão: America/Sao_Paulo)</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String PROPERTIES_FILE = "/application.properties";
    private static Properties properties;

    // Carregar propriedades na inicialização da classe
    static {
        loadProperties();
    }

    /**
     * Carrega as propriedades do arquivo application.properties.
     */
    private static void loadProperties() {
        properties = new Properties();

        try (InputStream input = AppConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                logger.warn("Arquivo {} não encontrado, usando valores padrão", PROPERTIES_FILE);
                return;
            }

            properties.load(input);
            logger.info("Configurações carregadas de {}", PROPERTIES_FILE);

        } catch (IOException e) {
            logger.error("Erro ao carregar configurações de {}", PROPERTIES_FILE, e);
        }
    }

    /**
     * Obtém uma propriedade como String.
     *
     * @param key chave da propriedade
     * @param defaultValue valor padrão se a propriedade não existir
     * @return String valor da propriedade
     */
    private static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Obtém uma propriedade como int.
     *
     * @param key chave da propriedade
     * @param defaultValue valor padrão se a propriedade não existir
     * @return int valor da propriedade
     */
    private static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Valor inválido para {}: {}. Usando padrão: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    // ========== Servidor ==========

    /**
     * Retorna a porta do servidor.
     *
     * @return int porta (padrão: 8080)
     */
    public static int getServerPort() {
        return getIntProperty("server.port", 8080);
    }

    /**
     * Retorna o host do servidor.
     *
     * @return String host (padrão: localhost)
     */
    public static String getServerHost() {
        return getProperty("server.host", "localhost");
    }

    // ========== Banco de Dados ==========

    /**
     * Retorna o caminho do banco de dados.
     *
     * @return String caminho do arquivo do banco (padrão: ./notisblokk.db)
     */
    public static String getDatabasePath() {
        return getProperty("database.path", "./notisblokk.db");
    }

    /**
     * Retorna o tamanho do pool de conexões.
     *
     * @return int tamanho do pool (padrão: 10)
     */
    public static int getDatabasePoolSize() {
        return getIntProperty("database.pool.size", 10);
    }

    // ========== Sessão ==========

    /**
     * Retorna o timeout de sessão em minutos.
     *
     * @return int timeout em minutos (padrão: 30)
     */
    public static int getSessionTimeoutMinutes() {
        return getIntProperty("session.timeout.minutes", 30);
    }

    /**
     * Retorna o número de dias para lembrar sessão.
     *
     * @return int dias (padrão: 7)
     */
    public static int getSessionRememberDays() {
        return getIntProperty("session.remember.days", 7);
    }

    // ========== Aplicação ==========

    /**
     * Retorna o nome da aplicação.
     *
     * @return String nome (padrão: Notisblokk)
     */
    public static String getAppName() {
        return getProperty("app.name", "Notisblokk");
    }

    /**
     * Retorna a versão da aplicação.
     *
     * @return String versão (padrão: 1.0)
     */
    public static String getAppVersion() {
        return getProperty("app.version", "1.0");
    }

    /**
     * Retorna o nome completo da aplicação com versão.
     *
     * @return String nome + versão (ex: "Notisblokk 1.0")
     */
    public static String getAppFullName() {
        return getAppName() + " " + getAppVersion();
    }

    /**
     * Retorna o fuso horário da aplicação.
     *
     * @return String timezone (padrão: America/Sao_Paulo)
     */
    public static String getAppTimezone() {
        return getProperty("app.timezone", "America/Sao_Paulo");
    }

    // ========== Email ==========

    public static String getEmailSmtpHost() {
        return getProperty("email.smtp.host", "smtp.gmail.com");
    }

    public static int getEmailSmtpPort() {
        return getIntProperty("email.smtp.port", 587);
    }

    public static String getEmailUsername() {
        return getProperty("email.smtp.username", "");
    }

    public static String getEmailPassword() {
        return getProperty("email.smtp.password", "");
    }

    public static boolean getEmailSmtpAuth() {
        return Boolean.parseBoolean(getProperty("email.smtp.auth", "true"));
    }

    public static boolean getEmailSmtpStartTls() {
        return Boolean.parseBoolean(getProperty("email.smtp.starttls.enable", "true"));
    }

    public static String getEmailFrom() {
        return getProperty("email.from", getEmailUsername());
    }

    public static String getEmailFromName() {
        return getProperty("email.from.name", "Notisblokk Sistema");
    }

    // ========== Backup ==========

    public static String getBackupFolder() {
        return getProperty("backup.folder", "./backups");
    }

    public static boolean getBackupScheduleEnabled() {
        return Boolean.parseBoolean(getProperty("backup.schedule.enabled", "true"));
    }

    public static String getBackupScheduleCron() {
        return getProperty("backup.schedule.cron", "0 0 2 * * ?");
    }

    public static boolean getBackupAutoEnabled() {
        return Boolean.parseBoolean(getProperty("backup.auto.enabled", "false"));
    }

    public static int getBackupPeriodicidadeDias() {
        return getIntProperty("backup.periodicidade.dias", 7);
    }

    // ========== Uploads ==========

    public static String getUploadsFolder() {
        return getProperty("uploads.folder", "./uploads");
    }

    public static int getUploadsMaxSizeMb() {
        return getIntProperty("uploads.max.size.mb", 10);
    }

    public static String getUploadsAllowedExtensions() {
        return getProperty("uploads.allowed.extensions", "txt,pdf,doc,docx,xls,xlsx,csv,jpg,png");
    }

    // ========== Security ==========

    public static int getSecurityLoginMaxAttempts() {
        return getIntProperty("security.login.max.attempts", 3);
    }

    public static int getSecurityLoginLockoutMinutes() {
        return getIntProperty("security.login.lockout.minutes", 30);
    }

    public static int getSecurityPasswordExpirationMonths() {
        return getIntProperty("security.password.expiration.months", 3);
    }

    public static int getSecurityPasswordWarningDays() {
        return getIntProperty("security.password.warning.days", 10);
    }

    // ========== Alerts ==========

    public static boolean getAlertsEmailEnabled() {
        return Boolean.parseBoolean(getProperty("alerts.email.enabled", "true"));
    }

    public static boolean getAlertsToastifyEnabled() {
        return Boolean.parseBoolean(getProperty("alerts.toastify.enabled", "true"));
    }

    public static int getAlertsCheckIntervalMinutes() {
        return getIntProperty("alerts.check.interval.minutes", 60);
    }

    public static int getAlertsCriticoDias() {
        return getIntProperty("alerts.critico.dias", 0);
    }

    public static int getAlertsUrgenteDias() {
        return getIntProperty("alerts.urgente.dias", 3);
    }

    public static int getAlertsAtencaoDias() {
        return getIntProperty("alerts.atencao.dias", 5);
    }

    public static int getAlertsAvisoDias() {
        return getIntProperty("alerts.aviso.dias", 7);
    }

    /**
     * Imprime todas as configurações carregadas (para debug).
     */
    public static void printConfig() {
        logger.info("========== CONFIGURAÇÕES ==========");
        logger.info("App: {} v{}", getAppName(), getAppVersion());
        logger.info("Servidor: {}:{}", getServerHost(), getServerPort());
        logger.info("Banco de dados: {}", getDatabasePath());
        logger.info("Pool de conexões: {}", getDatabasePoolSize());
        logger.info("Timeout de sessão: {} min", getSessionTimeoutMinutes());
        logger.info("Timezone: {}", getAppTimezone());
        logger.info("Email: {} ({})", getEmailFrom(), getEmailFromName());
        logger.info("Backup: {} (Auto: {})", getBackupFolder(), getBackupAutoEnabled());
        logger.info("Uploads: {} (Max: {} MB)", getUploadsFolder(), getUploadsMaxSizeMb());
        logger.info("===================================");
    }
}
