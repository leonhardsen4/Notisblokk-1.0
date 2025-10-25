package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.User;
import com.notisblokk.model.UserRole;
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
 * Repositório responsável pelo acesso a dados de usuários.
 *
 * <p>Implementa operações CRUD (Create, Read, Update, Delete) para a entidade User,
 * utilizando PreparedStatements para prevenir SQL injection.</p>
 *
 * <p><b>Métodos principais:</b></p>
 * <ul>
 *   <li>buscarTodos() - Lista todos os usuários</li>
 *   <li>buscarPorId() - Busca usuário por ID</li>
 *   <li>buscarPorUsername() - Busca usuário por username</li>
 *   <li>buscarPorEmail() - Busca usuário por email</li>
 *   <li>salvar() - Cria novo usuário</li>
 *   <li>atualizar() - Atualiza usuário existente</li>
 *   <li>deletar() - Remove usuário (soft ou hard delete)</li>
 *   <li>alterarStatus() - Ativa/desativa usuário</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Busca todos os usuários do sistema.
     *
     * @return List<User> lista de todos os usuários (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<User> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

            logger.debug("Encontrados {} usuários", users.size());
        }

        return users;
    }

    /**
     * Busca um usuário por ID.
     *
     * @param id ID do usuário
     * @return Optional<User> usuário encontrado ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<User> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    logger.debug("Usuário encontrado: {}", user.getUsername());
                    return Optional.of(user);
                }
            }
        }

        logger.debug("Usuário com ID {} não encontrado", id);
        return Optional.empty();
    }

    /**
     * Busca um usuário por username.
     *
     * @param username nome de usuário
     * @return Optional<User> usuário encontrado ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<User> buscarPorUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    logger.debug("Usuário encontrado: {}", user.getUsername());
                    return Optional.of(user);
                }
            }
        }

        logger.debug("Usuário com username '{}' não encontrado", username);
        return Optional.empty();
    }

    /**
     * Busca um usuário por email.
     *
     * @param email email do usuário
     * @return Optional<User> usuário encontrado ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<User> buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    logger.debug("Usuário encontrado: {}", user.getEmail());
                    return Optional.of(user);
                }
            }
        }

        logger.debug("Usuário com email '{}' não encontrado", email);
        return Optional.empty();
    }

    /**
     * Busca um usuário por username ou email.
     * Útil para login onde o usuário pode usar qualquer um dos dois.
     *
     * @param usernameOrEmail username ou email do usuário
     * @return Optional<User> usuário encontrado ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<User> buscarPorUsernameOuEmail(String usernameOrEmail) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usernameOrEmail);
            pstmt.setString(2, usernameOrEmail);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    logger.debug("Usuário encontrado: {}", user.getUsername());
                    return Optional.of(user);
                }
            }
        }

        logger.debug("Usuário com username/email '{}' não encontrado", usernameOrEmail);
        return Optional.empty();
    }

    /**
     * Salva um novo usuário no banco de dados.
     *
     * @param user usuário a ser salvo (sem ID)
     * @return User usuário salvo com ID gerado
     * @throws SQLException se houver erro ao salvar
     */
    public User salvar(User user) throws SQLException {
        String sql = """
            INSERT INTO users (username, email, password_hash, full_name, role, active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getRole().name());
            pstmt.setBoolean(6, user.isActive());
            pstmt.setString(7, timestamp);
            pstmt.setString(8, timestamp);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar usuário, nenhuma linha afetada");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                    user.setCreatedAt(now);
                    user.setUpdatedAt(now);
                } else {
                    throw new SQLException("Falha ao salvar usuário, ID não gerado");
                }
            }

            logger.info("Usuário salvo com sucesso: {} (ID: {})", user.getUsername(), user.getId());
            return user;
        }
    }

    /**
     * Atualiza um usuário existente no banco de dados.
     *
     * @param user usuário a ser atualizado (com ID)
     * @throws SQLException se houver erro ao atualizar
     */
    public void atualizar(User user) throws SQLException {
        String sql = """
            UPDATE users
            SET username = ?, email = ?, password_hash = ?, full_name = ?, role = ?, active = ?, updated_at = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String timestamp = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getRole().name());
            pstmt.setBoolean(6, user.isActive());
            pstmt.setString(7, timestamp);
            pstmt.setLong(8, user.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Usuário com ID " + user.getId() + " não encontrado");
            }

            logger.info("Usuário atualizado com sucesso: {} (ID: {})", user.getUsername(), user.getId());
        }
    }

    /**
     * Remove um usuário do banco de dados (hard delete).
     *
     * @param id ID do usuário a ser removido
     * @throws SQLException se houver erro ao deletar
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Usuário com ID " + id + " não encontrado");
            }

            logger.info("Usuário com ID {} removido com sucesso", id);
        }
    }

    /**
     * Altera o status de ativação de um usuário (soft delete).
     *
     * @param id ID do usuário
     * @param active novo status (true = ativo, false = inativo)
     * @throws SQLException se houver erro ao atualizar
     */
    public void alterarStatus(Long id, boolean active) throws SQLException {
        String sql = "UPDATE users SET active = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String timestamp = LocalDateTime.now(BRAZIL_ZONE).format(FORMATTER);

            pstmt.setBoolean(1, active);
            pstmt.setString(2, timestamp);
            pstmt.setLong(3, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Usuário com ID " + id + " não encontrado");
            }

            logger.info("Status do usuário ID {} alterado para: {}", id, active ? "ATIVO" : "INATIVO");
        }
    }

    /**
     * Conta o total de usuários no sistema.
     *
     * @return long total de usuários
     * @throws SQLException se houver erro ao contar
     */
    public long contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";

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
     * Conta usuários ativos no sistema.
     *
     * @return long total de usuários ativos
     * @throws SQLException se houver erro ao contar
     */
    public long contarAtivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE active = 1";

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
     * Mapeia um ResultSet para um objeto User.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return User objeto mapeado
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(UserRole.fromString(rs.getString("role")));
        user.setActive(rs.getBoolean("active"));

        // Parse timestamps
        String createdAtStr = rs.getString("created_at");
        String updatedAtStr = rs.getString("updated_at");

        if (createdAtStr != null) {
            user.setCreatedAt(LocalDateTime.parse(createdAtStr, FORMATTER));
        }
        if (updatedAtStr != null) {
            user.setUpdatedAt(LocalDateTime.parse(updatedAtStr, FORMATTER));
        }

        return user;
    }
}
