package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.StatusNota;
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
 * Repositório responsável pelo acesso a dados de status de notas.
 *
 * <p>Implementa operações CRUD (Create, Read, Update, Delete) para a entidade StatusNota,
 * utilizando PreparedStatements para prevenir SQL injection.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class StatusNotaRepository {

    private static final Logger logger = LoggerFactory.getLogger(StatusNotaRepository.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Busca todos os status de notas do sistema, ordenados alfabeticamente.
     *
     * @return List<StatusNota> lista de todos os status (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<StatusNota> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM status_nota ORDER BY nome ASC";
        List<StatusNota> statusList = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                statusList.add(mapResultSetToStatusNota(rs));
            }

            logger.debug("Encontrados {} status", statusList.size());
        }

        return statusList;
    }

    /**
     * Busca um status por ID.
     *
     * @param id ID do status
     * @return Optional<StatusNota> status encontrado ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<StatusNota> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM status_nota WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    StatusNota status = mapResultSetToStatusNota(rs);
                    logger.debug("Status encontrado: {}", status.getNome());
                    return Optional.of(status);
                }
            }
        }

        logger.debug("Status com ID {} não encontrado", id);
        return Optional.empty();
    }

    /**
     * Busca um status por nome.
     *
     * @param nome nome do status
     * @return Optional<StatusNota> status encontrado ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<StatusNota> buscarPorNome(String nome) throws SQLException {
        String sql = "SELECT * FROM status_nota WHERE nome = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nome);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    StatusNota status = mapResultSetToStatusNota(rs);
                    logger.debug("Status encontrado: {}", status.getNome());
                    return Optional.of(status);
                }
            }
        }

        logger.debug("Status com nome '{}' não encontrado", nome);
        return Optional.empty();
    }

    /**
     * Salva um novo status no banco de dados.
     *
     * @param status status a ser salvo (sem ID)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return StatusNota status salvo com ID gerado
     * @throws SQLException se houver erro ao salvar
     */
    public StatusNota salvar(StatusNota status, Long sessaoId, Long usuarioId) throws SQLException {
        String sql = """
            INSERT INTO status_nota (nome, cor_hex, data_criacao, sessao_id, usuario_id)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);

            pstmt.setString(1, status.getNome());
            pstmt.setString(2, status.getCorHex());
            pstmt.setString(3, timestamp);
            pstmt.setLong(4, sessaoId);
            pstmt.setLong(5, usuarioId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar status, nenhuma linha afetada");
            }

            // Obter ID gerado
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {

                if (rs.next()) {
                    status.setId(rs.getLong(1));
                    status.setDataCriacao(now);
                    status.setSessaoId(sessaoId);
                    status.setUsuarioId(usuarioId);
                } else {
                    throw new SQLException("Falha ao obter ID do status criado");
                }
            }

            logger.info("Status salvo com sucesso: {} (ID: {})", status.getNome(), status.getId());
            return status;
        }
    }

    /**
     * Atualiza um status existente no banco de dados.
     *
     * @param status status a ser atualizado (com ID)
     * @throws SQLException se houver erro ao atualizar
     */
    public void atualizar(StatusNota status) throws SQLException {
        String sql = "UPDATE status_nota SET nome = ?, cor_hex = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.getNome());
            pstmt.setString(2, status.getCorHex());
            pstmt.setLong(3, status.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Status com ID " + status.getId() + " não encontrado");
            }

            logger.info("Status atualizado com sucesso: {} (ID: {})", status.getNome(), status.getId());
        }
    }

    /**
     * Remove um status do banco de dados.
     * ATENÇÃO: Não será possível deletar se houver notas com esse status (RESTRICT).
     *
     * @param id ID do status a ser removido
     * @throws SQLException se houver erro ao deletar ou se houver notas com esse status
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM status_nota WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Status com ID " + id + " não encontrado");
            }

            logger.info("Status com ID {} removido com sucesso", id);
        }
    }

    /**
     * Conta quantas notas estão associadas a um status.
     *
     * @param statusId ID do status
     * @return long número de notas associadas
     * @throws SQLException se houver erro ao contar
     */
    public long contarNotasPorStatus(Long statusId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notas WHERE status_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, statusId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        }
    }

    /**
     * Mapeia um ResultSet para um objeto StatusNota.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return StatusNota objeto mapeado
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private StatusNota mapResultSetToStatusNota(ResultSet rs) throws SQLException {
        StatusNota status = new StatusNota();
        status.setId(rs.getLong("id"));
        status.setNome(rs.getString("nome"));
        status.setCorHex(rs.getString("cor_hex"));
        status.setSessaoId(rs.getLong("sessao_id"));
        status.setUsuarioId(rs.getLong("usuario_id"));

        // Parse data_criacao usando formato brasileiro
        String dataCriacaoStr = rs.getString("data_criacao");
        if (dataCriacaoStr != null && !dataCriacaoStr.isEmpty()) {
            try {
                status.setDataCriacao(LocalDateTime.parse(dataCriacaoStr, FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", dataCriacaoStr);
                // Fallback para formato ISO caso exista dado antigo
                try {
                    status.setDataCriacao(LocalDateTime.parse(dataCriacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse da data: {}", dataCriacaoStr);
                }
            }
        }

        return status;
    }
}
