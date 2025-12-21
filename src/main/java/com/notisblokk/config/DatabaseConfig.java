package com.notisblokk.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Configuração do banco de dados SQLite com pool de conexões HikariCP.
 *
 * <p>Esta classe é responsável por:</p>
 * <ul>
 *   <li>Configurar e inicializar o pool de conexões HikariCP</li>
 *   <li>Executar o schema SQL para criar tabelas e índices</li>
 *   <li>Criar usuários padrão (admin e operador) na primeira execução</li>
 *   <li>Fornecer conexões para acesso ao banco de dados</li>
 * </ul>
 *
 * <p><b>Configurações do Pool HikariCP:</b></p>
 * <ul>
 *   <li>Pool size: 10 conexões</li>
 *   <li>Connection timeout: 30 segundos</li>
 *   <li>Idle timeout: 600 segundos (10 minutos)</li>
 *   <li>Max lifetime: 1800 segundos (30 minutos)</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String DATABASE_PATH = "./notisblokk.db";
    private static final String SCHEMA_FILE = "/database/schema.sql";
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static HikariDataSource dataSource;

    /**
     * Inicializa o banco de dados.
     *
     * <p>Configura o pool de conexões HikariCP, executa o schema SQL e cria
     * os usuários padrão caso ainda não existam.</p>
     *
     * @throws RuntimeException se houver erro na inicialização do banco
     */
    public static void initialize() {
        logger.info("Iniciando configuração do banco de dados...");

        try {
            configurarHikariCP();
            executarSchema();
            criarUsuariosPadrao();
            criarStatusPadrao();

            logger.info("Banco de dados inicializado com sucesso!");
        } catch (Exception e) {
            logger.error("Erro ao inicializar banco de dados", e);
            throw new RuntimeException("Falha na inicialização do banco de dados", e);
        }
    }

    /**
     * Configura o pool de conexões HikariCP.
     *
     * <p>Define as configurações de pool, timeouts e propriedades específicas
     * do SQLite para otimização de performance.</p>
     */
    private static void configurarHikariCP() {
        logger.info("Configurando pool de conexões HikariCP...");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + DATABASE_PATH);
        config.setDriverClassName("org.sqlite.JDBC");

        // Configurações do pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30 segundos
        config.setIdleTimeout(600000);      // 10 minutos
        config.setMaxLifetime(1800000);     // 30 minutos

        // Propriedades específicas do SQLite
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Nome do pool para identificação em logs
        config.setPoolName("NotisblokkPool");

        dataSource = new HikariDataSource(config);

        logger.info("Pool de conexões HikariCP configurado com sucesso");
    }

    /**
     * Executa o arquivo schema.sql para criar tabelas, índices e triggers.
     *
     * @throws IOException se houver erro ao ler o arquivo schema.sql
     * @throws SQLException se houver erro ao executar o SQL
     */
    private static void executarSchema() throws IOException, SQLException {
        logger.info("Executando schema SQL...");

        // Ler o arquivo schema.sql do classpath
        InputStream is = DatabaseConfig.class.getResourceAsStream(SCHEMA_FILE);
        if (is == null) {
            throw new IOException("Arquivo schema.sql não encontrado em resources/database");
        }

        String schema;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            schema = reader.lines()
                    .filter(line -> !line.trim().startsWith("--"))  // Remove comentários
                    .filter(line -> !line.trim().isEmpty())         // Remove linhas vazias
                    .collect(Collectors.joining("\n"));
        }

        // Executar o schema
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Dividir statements considerando blocos BEGIN/END
            String[] statements = splitSqlStatements(schema);
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    logger.debug("Executando SQL: {}", trimmed.substring(0, Math.min(50, trimmed.length())) + "...");
                    stmt.execute(trimmed);
                }
            }

            logger.info("Schema SQL executado com sucesso");
        }
    }

    /**
     * Divide o schema SQL em statements individuais, respeitando blocos BEGIN/END.
     *
     * @param schema conteúdo completo do schema SQL
     * @return array de statements SQL
     */
    private static String[] splitSqlStatements(String schema) {
        java.util.List<String> statements = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBlock = false;

        String[] lines = schema.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();

            // Detectar início de bloco (TRIGGER com BEGIN)
            if (trimmed.matches("(?i).*\\bBEGIN\\b.*")) {
                inBlock = true;
            }

            current.append(line).append("\n");

            // Detectar fim de statement
            if (trimmed.endsWith(";")) {
                if (inBlock && trimmed.matches("(?i).*\\bEND\\s*;.*")) {
                    // Fim de bloco TRIGGER
                    inBlock = false;
                    statements.add(current.toString());
                    current = new StringBuilder();
                } else if (!inBlock) {
                    // Statement normal
                    statements.add(current.toString());
                    current = new StringBuilder();
                }
            }
        }

        // Adicionar último statement se houver
        if (!current.isEmpty()) {
            statements.add(current.toString());
        }

        return statements.toArray(new String[0]);
    }

    /**
     * Cria os usuários padrão do sistema se ainda não existirem.
     *
     * <p><b>Usuários criados:</b></p>
     * <ul>
     *   <li><b>Administrador:</b> username=admin, email=admin@notisblokk.com, senha=Admin@123, role=ADMIN</li>
     *   <li><b>Operador:</b> username=operador, email=operador@notisblokk.com, senha=Operador@123, role=OPERATOR</li>
     * </ul>
     *
     * <p>As senhas são criptografadas usando BCrypt com cost factor 12.</p>
     *
     * @throws SQLException se houver erro ao criar os usuários
     */
    private static void criarUsuariosPadrao() throws SQLException {
        logger.info("Verificando usuários padrão...");

        try (Connection conn = getConnection()) {

            // Verificar se já existem usuários
            String checkSql = "SELECT COUNT(*) FROM users";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {

                if (rs.next() && rs.getInt(1) > 0) {
                    logger.info("Usuários já existem no banco de dados");
                    return;
                }
            }

            logger.info("Criando usuários padrão...");

            // SQL para inserir usuário
            String insertSql = """
                INSERT INTO users (username, email, password_hash, full_name, role, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 1, ?, ?)
            """;

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);

            // Criar administrador
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, "admin");
                pstmt.setString(2, "admin@notisblokk.com");
                pstmt.setString(3, BCrypt.hashpw("Admin@123", BCrypt.gensalt(12)));
                pstmt.setString(4, "Administrador do Sistema");
                pstmt.setString(5, "ADMIN");
                pstmt.setString(6, timestamp);
                pstmt.setString(7, timestamp);
                pstmt.executeUpdate();

                logger.info("✓ Usuário ADMIN criado com sucesso");
            }

            // Criar operador
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, "operador");
                pstmt.setString(2, "operador@notisblokk.com");
                pstmt.setString(3, BCrypt.hashpw("Operador@123", BCrypt.gensalt(12)));
                pstmt.setString(4, "Operador do Sistema");
                pstmt.setString(5, "OPERATOR");
                pstmt.setString(6, timestamp);
                pstmt.setString(7, timestamp);
                pstmt.executeUpdate();

                logger.info("✓ Usuário OPERATOR criado com sucesso");
            }

            logger.info("Usuários padrão criados com sucesso!");
            logger.info("  → Admin: admin@notisblokk.com / Admin@123");
            logger.info("  → Operador: operador@notisblokk.com / Operador@123");
        }
    }

    /**
     * Cria os status padrão do sistema de notas se ainda não existirem.
     *
     * <p><b>Status criados:</b></p>
     * <ul>
     *   <li><b>Pendente:</b> cor laranja (#FFA500)</li>
     *   <li><b>Em Andamento:</b> cor azul (#4A90E2)</li>
     *   <li><b>Resolvido:</b> cor verde (#10B981)</li>
     *   <li><b>Suspenso:</b> cor cinza (#9CA3AF)</li>
     *   <li><b>Cancelado:</b> cor vermelho (#EF4444)</li>
     * </ul>
     *
     * @throws SQLException se houver erro ao criar os status
     */
    private static void criarStatusPadrao() throws SQLException {
        logger.info("Verificando status padrão...");

        try (Connection conn = getConnection()) {

            // Array com os status padrão [nome, cor]
            String[][] statusPadrao = {
                {"Pendente", "#FFA500"},
                {"Em Andamento", "#4A90E2"},
                {"Resolvido", "#10B981"},
                {"Suspenso", "#9CA3AF"},
                {"Cancelado", "#EF4444"}
            };

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);

            // Verificar e criar cada status se não existir
            String checkSql = "SELECT COUNT(*) FROM status_nota WHERE nome = ?";
            String insertSql = """
                INSERT INTO status_nota (nome, cor_hex, data_criacao, sessao_id, usuario_id)
                VALUES (?, ?, ?, NULL, NULL)
            """;

            int statusCriados = 0;
            for (String[] status : statusPadrao) {
                // Verificar se status já existe
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, status[0]);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            logger.debug("Status '{}' já existe", status[0]);
                            continue;
                        }
                    }
                }

                // Criar status se não existe
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setString(1, status[0]);
                    pstmt.setString(2, status[1]);
                    pstmt.setString(3, timestamp);
                    pstmt.executeUpdate();
                    statusCriados++;

                    logger.info("✓ Status '{}' criado com cor {}", status[0], status[1]);
                }
            }

            if (statusCriados > 0) {
                logger.info("{} status padrão criados com sucesso!", statusCriados);
            } else {
                logger.info("Todos os status padrão já existem no banco de dados");
            }
        }
    }

    /**
     * Obtém uma conexão do pool HikariCP.
     *
     * @return Connection conexão ativa do pool
     * @throws SQLException se não conseguir obter conexão do pool
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource não inicializado. Chame initialize() primeiro.");
        }
        return dataSource.getConnection();
    }

    /**
     * Fecha o pool de conexões HikariCP.
     *
     * <p>Deve ser chamado ao encerrar a aplicação para liberar recursos.</p>
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Fechando pool de conexões...");
            dataSource.close();
            logger.info("Pool de conexões fechado");
        }
    }

    /**
     * Limpa sessões expiradas do banco de dados.
     *
     * <p>Atualiza o status de sessões ativas que ultrapassaram o tempo
     * de expiração configurado (30 minutos por padrão).</p>
     *
     * @param timeoutMinutes tempo em minutos para considerar sessão expirada
     * @return int número de sessões expiradas
     * @throws SQLException se houver erro ao limpar sessões
     */
    public static int limparSessoesExpiradas(int timeoutMinutes) throws SQLException {
        String sql = """
            UPDATE sessions
            SET status = 'EXPIRED'
            WHERE status = 'ACTIVE'
            AND datetime(login_time, '+' || ? || ' minutes') < datetime('now')
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, timeoutMinutes);
            int updated = pstmt.executeUpdate();

            if (updated > 0) {
                logger.info("Limpeza de sessões: {} sessões expiradas", Optional.of(updated));
            }

            return updated;
        }
    }

    /**
     * Retorna informações sobre o pool de conexões.
     *
     * @return String informações do pool (total, ativas, ociosas)
     */
    public static String getPoolStats() {
        if (dataSource == null) {
            return "Pool não inicializado";
        }

        try {
            com.zaxxer.hikari.HikariPoolMXBean poolMXBean = dataSource.getHikariPoolMXBean();
            return String.format(
                "Pool: %d conexões (Ativas: %d, Ociosas: %d, Aguardando: %d)",
                poolMXBean.getTotalConnections(),
                poolMXBean.getActiveConnections(),
                poolMXBean.getIdleConnections(),
                poolMXBean.getThreadsAwaitingConnection()
            );
        } catch (Exception e) {
            logger.warn("Não foi possível obter estatísticas do pool", e);
            return "Pool inicializado (estatísticas indisponíveis)";
        }
    }
}