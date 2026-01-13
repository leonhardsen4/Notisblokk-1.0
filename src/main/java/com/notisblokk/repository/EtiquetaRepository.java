package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.Etiqueta;
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
 * Repositório responsável pelo acesso a dados de etiquetas.
 *
 * <p>Implementa operações CRUD (Create, Read, Update, Delete) para a entidade Etiqueta,
 * utilizando PreparedStatements para prevenir SQL injection.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class EtiquetaRepository {

    private static final Logger logger = LoggerFactory.getLogger(EtiquetaRepository.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Busca todas as etiquetas do sistema, ordenadas alfabeticamente.
     *
     * @return List<Etiqueta> lista de todas as etiquetas (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Etiqueta> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM etiquetas ORDER BY nome ASC";
        List<Etiqueta> etiquetas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                etiquetas.add(mapResultSetToEtiqueta(rs));
            }

            logger.debug("Encontradas {} etiquetas", etiquetas.size());
        }

        return etiquetas;
    }

    /**
     * Busca uma etiqueta por ID.
     *
     * @param id ID da etiqueta
     * @return Optional<Etiqueta> etiqueta encontrada ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<Etiqueta> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM etiquetas WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Etiqueta etiqueta = mapResultSetToEtiqueta(rs);
                    logger.debug("Etiqueta encontrada: {}", etiqueta.getNome());
                    return Optional.of(etiqueta);
                }
            }
        }

        logger.debug("Etiqueta com ID {} não encontrada", id);
        return Optional.empty();
    }

    /**
     * Busca uma etiqueta por nome.
     *
     * @param nome nome da etiqueta
     * @return Optional<Etiqueta> etiqueta encontrada ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<Etiqueta> buscarPorNome(String nome) throws SQLException {
        String sql = "SELECT * FROM etiquetas WHERE nome = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nome);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Etiqueta etiqueta = mapResultSetToEtiqueta(rs);
                    logger.debug("Etiqueta encontrada: {}", etiqueta.getNome());
                    return Optional.of(etiqueta);
                }
            }
        }

        logger.debug("Etiqueta com nome '{}' não encontrada", nome);
        return Optional.empty();
    }

    /**
     * Salva uma nova etiqueta no banco de dados.
     *
     * @param etiqueta etiqueta a ser salva (sem ID)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return Etiqueta etiqueta salva com ID gerado
     * @throws SQLException se houver erro ao salvar
     */
    public Etiqueta salvar(Etiqueta etiqueta, Long sessaoId, Long usuarioId) throws SQLException {
        String sql = """
            INSERT INTO etiquetas (nome, data_criacao, sessao_id, usuario_id)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);

            pstmt.setString(1, etiqueta.getNome());
            pstmt.setString(2, timestamp);
            pstmt.setLong(3, sessaoId);
            pstmt.setLong(4, usuarioId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar etiqueta, nenhuma linha afetada");
            }

            // Obter ID gerado
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {

                if (rs.next()) {
                    etiqueta.setId(rs.getLong(1));
                    etiqueta.setDataCriacao(now);
                    etiqueta.setSessaoId(sessaoId);
                    etiqueta.setUsuarioId(usuarioId);
                } else {
                    throw new SQLException("Falha ao obter ID da etiqueta criada");
                }
            }

            logger.info("Etiqueta salva com sucesso: {} (ID: {})", etiqueta.getNome(), etiqueta.getId());
            return etiqueta;
        }
    }

    /**
     * Atualiza uma etiqueta existente no banco de dados.
     *
     * @param etiqueta etiqueta a ser atualizada (com ID)
     * @throws SQLException se houver erro ao atualizar
     */
    public void atualizar(Etiqueta etiqueta) throws SQLException {
        String sql = "UPDATE etiquetas SET nome = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, etiqueta.getNome());
            pstmt.setLong(2, etiqueta.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Etiqueta com ID " + etiqueta.getId() + " não encontrada");
            }

            logger.info("Etiqueta atualizada com sucesso: {} (ID: {})", etiqueta.getNome(), etiqueta.getId());
        }
    }

    /**
     * Remove uma etiqueta do banco de dados.
     * ATENÇÃO: Cascata irá deletar todas as tarefas associadas!
     *
     * @param id ID da etiqueta a ser removida
     * @throws SQLException se houver erro ao deletar
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM etiquetas WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Etiqueta com ID " + id + " não encontrada");
            }

            logger.info("Etiqueta com ID {} removida com sucesso", id);
        }
    }

    /**
     * Conta quantas tarefas estão associadas a uma etiqueta.
     *
     * @param etiquetaId ID da etiqueta
     * @return long número de tarefas associadas
     * @throws SQLException se houver erro ao contar
     */
    public long contarTarefasPorEtiqueta(Long etiquetaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tarefas WHERE etiqueta_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, etiquetaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        }
    }

    /**
     * Mapeia um ResultSet para um objeto Etiqueta.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return Etiqueta objeto mapeado
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private Etiqueta mapResultSetToEtiqueta(ResultSet rs) throws SQLException {
        Etiqueta etiqueta = new Etiqueta();
        etiqueta.setId(rs.getLong("id"));
        etiqueta.setNome(rs.getString("nome"));
        etiqueta.setSessaoId(rs.getLong("sessao_id"));
        etiqueta.setUsuarioId(rs.getLong("usuario_id"));

        // Parse data_criacao usando formato brasileiro
        String dataCriacaoStr = rs.getString("data_criacao");
        if (dataCriacaoStr != null && !dataCriacaoStr.isEmpty()) {
            try {
                etiqueta.setDataCriacao(LocalDateTime.parse(dataCriacaoStr, FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", dataCriacaoStr);
                // Fallback para formato ISO caso exista dado antigo
                try {
                    etiqueta.setDataCriacao(LocalDateTime.parse(dataCriacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse da data: {}", dataCriacaoStr);
                }
            }
        }

        return etiqueta;
    }
}
