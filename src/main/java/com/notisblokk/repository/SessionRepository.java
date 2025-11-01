package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.Session;
import com.notisblokk.model.SessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório responsável pelo acesso a dados de sessões.
 *
 * <p>Implementa operações CRUD para a entidade Session, gerenciando
 * o log de acessos e sessões ativas dos usuários.</p>
 *
 * <p><b>Métodos principais:</b></p>
 * <ul>
 *   <li>buscarTodas() - Lista todas as sessões</li>
 *   <li>buscarPorId() - Busca sessão por ID</li>
 *   <li>buscarPorUsuario() - Lista sessões de um usuário</li>
 *   <li>buscarSessoesAtivas() - Lista sessões ativas</li>
 *   <li>salvar() - Cria nova sessão (login)</li>
 *   <li>encerrar() - Encerra sessão (logout)</li>
 *   <li>expirarSessoes() - Marca sessões como expiradas</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class SessionRepository {

    private static final Logger logger = LoggerFactory.getLogger(SessionRepository.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Busca todas as sessões do sistema.
     *
     * @return List<Session> lista de todas as sessões (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Session> buscarTodas() throws SQLException {
        String sql = "SELECT * FROM sessions ORDER BY login_time DESC";
        List<Session> sessions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                sessions.add(mapResultSetToSession(rs));
            }

            logger.debug("Encontradas {} sessões", sessions.size());
        }

        return sessions;
    }

    /**
     * Busca uma sessão por ID.
     *
     * @param id ID da sessão
     * @return Optional<Session> sessão encontrada ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<Session> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Session session = mapResultSetToSession(rs);
                    logger.debug("Sessão encontrada: ID {}", session.getId());
                    return Optional.of(session);
                }
            }
        }

        logger.debug("Sessão com ID {} não encontrada", id);
        return Optional.empty();
    }

    /**
     * Busca todas as sessões de um usuário específico.
     *
     * @param userId ID do usuário
     * @return List<Session> lista de sessões do usuário (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Session> buscarPorUsuario(Long userId) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE user_id = ? ORDER BY login_time DESC";
        List<Session> sessions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }

            logger.debug("Encontradas {} sessões para o usuário ID {}", sessions.size(), userId);
        }

        return sessions;
    }

    /**
     * Busca todas as sessões ativas do sistema.
     *
     * @return List<Session> lista de sessões ativas (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Session> buscarSessoesAtivas() throws SQLException {
        String sql = "SELECT * FROM sessions WHERE status = 'ACTIVE' ORDER BY login_time DESC";
        List<Session> sessions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                sessions.add(mapResultSetToSession(rs));
            }

            logger.debug("Encontradas {} sessões ativas", sessions.size());
        }

        return sessions;
    }

    /**
     * Busca sessões ativas de um usuário específico.
     *
     * @param userId ID do usuário
     * @return List<Session> lista de sessões ativas do usuário
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Session> buscarSessoesAtivasPorUsuario(Long userId) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE user_id = ? AND status = 'ACTIVE' ORDER BY login_time DESC";
        List<Session> sessions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }

            logger.debug("Encontradas {} sessões ativas para o usuário ID {}", sessions.size(), userId);
        }

        return sessions;
    }

    /**
     * Busca as últimas N sessões do sistema.
     *
     * @param limit número máximo de sessões a retornar
     * @return List<Session> lista das últimas sessões
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Session> buscarUltimas(int limit) throws SQLException {
        String sql = "SELECT * FROM sessions ORDER BY login_time DESC LIMIT ?";
        List<Session> sessions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }

            logger.debug("Encontradas {} últimas sessões", sessions.size());
        }

        return sessions;
    }

    /**
     * Salva uma nova sessão no banco de dados.
     *
     * @param session sessão a ser salva (sem ID)
     * @return Session sessão salva com ID gerado
     * @throws SQLException se houver erro ao salvar
     */
    public Session salvar(Session session) throws SQLException {
        String sql = """
            INSERT INTO sessions (user_id, login_time, logout_time, ip_address, user_agent, status)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String loginTime = now.format(FORMATTER);

            pstmt.setLong(1, session.getUserId());
            pstmt.setString(2, loginTime);
            pstmt.setString(3, session.getLogoutTime() != null ? session.getLogoutTime().format(FORMATTER) : null);
            pstmt.setString(4, session.getIpAddress());
            pstmt.setString(5, session.getUserAgent());
            pstmt.setString(6, session.getStatus().name());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar sessão, nenhuma linha afetada");
            }

            // SQLite: Obter ID usando last_insert_rowid()
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {

                if (rs.next()) {
                    session.setId(rs.getLong(1));
                    session.setLoginTime(now);
                } else {
                    throw new SQLException("Falha ao obter ID da sessão criada");
                }
            }

            logger.info("Sessão criada com sucesso para usuário ID {} (Sessão ID: {})",
                       session.getUserId(), session.getId());
            return session;
        }
    }

    /**
     * Atualiza uma sessão existente.
     *
     * @param session sessão a ser atualizada
     * @throws SQLException se houver erro ao atualizar
     */
    public void atualizar(Session session) throws SQLException {
        String sql = """
            UPDATE sessions
            SET logout_time = ?, status = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, session.getLogoutTime() != null ? session.getLogoutTime().format(FORMATTER) : null);
            pstmt.setString(2, session.getStatus().name());
            pstmt.setLong(3, session.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Sessão com ID " + session.getId() + " não encontrada");
            }

            logger.info("Sessão ID {} atualizada com sucesso (Status: {})",
                       session.getId(), session.getStatus());
        }
    }

    /**
     * Encerra uma sessão ativa (logout).
     *
     * @param sessionId ID da sessão a ser encerrada
     * @throws SQLException se houver erro ao encerrar
     */
    public void encerrar(Long sessionId) throws SQLException {
        String sql = """
            UPDATE sessions
            SET logout_time = ?, status = 'LOGGED_OUT'
            WHERE id = ? AND status = 'ACTIVE'
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String logoutTime = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);

            pstmt.setString(1, logoutTime);
            pstmt.setLong(2, sessionId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                logger.warn("Sessão ID {} não encontrada ou já encerrada", sessionId);
            } else {
                logger.info("Sessão ID {} encerrada com sucesso", sessionId);
            }
        }
    }

    /**
     * Encerra todas as sessões ativas de um usuário.
     *
     * @param userId ID do usuário
     * @return int número de sessões encerradas
     * @throws SQLException se houver erro ao encerrar
     */
    public int encerrarTodasDoUsuario(Long userId) throws SQLException {
        String sql = """
            UPDATE sessions
            SET logout_time = ?, status = 'LOGGED_OUT'
            WHERE user_id = ? AND status = 'ACTIVE'
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String logoutTime = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);

            pstmt.setString(1, logoutTime);
            pstmt.setLong(2, userId);

            int affectedRows = pstmt.executeUpdate();

            logger.info("Encerradas {} sessões ativas do usuário ID {}", affectedRows, userId);
            return affectedRows;
        }
    }

    /**
     * Marca sessões como expiradas baseado no timeout.
     *
     * @param timeoutMinutes tempo em minutos para considerar expirada
     * @return int número de sessões expiradas
     * @throws SQLException se houver erro ao expirar
     */
    public int expirarSessoes(int timeoutMinutes) throws SQLException {
        String sql = """
            UPDATE sessions
            SET status = 'EXPIRED', logout_time = ?
            WHERE status = 'ACTIVE'
            AND datetime(login_time, 'localtime') < datetime('now', 'localtime', '-' || ? || ' minutes')
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String now = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);

            pstmt.setString(1, now);
            pstmt.setInt(2, timeoutMinutes);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Expiradas {} sessões antigas (timeout: {} min)", affectedRows, timeoutMinutes);
            }

            return affectedRows;
        }
    }

    /**
     * Conta o total de sessões no sistema.
     *
     * @return long total de sessões
     * @throws SQLException se houver erro ao contar
     */
    public long contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sessions";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    /**
     * Conta sessões ativas no sistema.
     *
     * @return long total de sessões ativas
     * @throws SQLException se houver erro ao contar
     */
    public long contarAtivas() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sessions WHERE status = 'ACTIVE'";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    /**
     * Conta sessões ativas de um usuário específico.
     *
     * @param userId ID do usuário
     * @return long total de sessões ativas do usuário
     * @throws SQLException se houver erro ao contar
     */
    public long contarAtivasPorUsuario(Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sessions WHERE user_id = ? AND status = 'ACTIVE'";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        }
    }

    /**
     * Encerra a sessão mais antiga de um usuário.
     * Útil quando o limite de sessões é atingido.
     *
     * @param userId ID do usuário
     * @return boolean true se uma sessão foi encerrada, false caso contrário
     * @throws SQLException se houver erro ao encerrar
     */
    public boolean encerrarSessaoMaisAntigaDoUsuario(Long userId) throws SQLException {
        // Primeiro buscar a sessão mais antiga
        String sqlSelect = """
            SELECT id FROM sessions
            WHERE user_id = ? AND status = 'ACTIVE'
            ORDER BY login_time ASC
            LIMIT 1
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Long sessionId = rs.getLong("id");
                    encerrar(sessionId);
                    logger.info("Sessão mais antiga (ID {}) do usuário {} encerrada por limite de sessões", sessionId, userId);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Mapeia um ResultSet para um objeto Session.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return Session objeto mapeado
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private Session mapResultSetToSession(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setId(rs.getLong("id"));
        session.setUserId(rs.getLong("user_id"));
        session.setIpAddress(rs.getString("ip_address"));
        session.setUserAgent(rs.getString("user_agent"));
        session.setStatus(SessionStatus.fromString(rs.getString("status")));

        // Parse timestamps - SQLite retorna no formato ISO
        String loginTimeStr = rs.getString("login_time");
        String logoutTimeStr = rs.getString("logout_time");

        if (loginTimeStr != null && !loginTimeStr.isEmpty()) {
            try {
                // Tentar formato brasileiro primeiro
                session.setLoginTime(LocalDateTime.parse(loginTimeStr, FORMATTER));
            } catch (Exception e) {
                // Se falhar, tentar formato ISO (padrão do SQLite)
                session.setLoginTime(LocalDateTime.parse(loginTimeStr.replace(" ", "T")));
            }
        }
        if (logoutTimeStr != null && !logoutTimeStr.isEmpty()) {
            try {
                // Tentar formato brasileiro primeiro
                session.setLogoutTime(LocalDateTime.parse(logoutTimeStr, FORMATTER));
            } catch (Exception e) {
                // Se falhar, tentar formato ISO (padrão do SQLite)
                session.setLogoutTime(LocalDateTime.parse(logoutTimeStr.replace(" ", "T")));
            }
        }

        return session;
    }
}
