package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.HistoricoCalculadora;
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
 * Repositório responsável pelo acesso a dados do histórico da calculadora.
 *
 * <p>Implementa operações CRUD para a entidade HistoricoCalculadora,
 * utilizando PreparedStatements para prevenir SQL injection.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class HistoricoCalculadoraRepository {

    private static final Logger logger = LoggerFactory.getLogger(HistoricoCalculadoraRepository.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Busca todo o histórico de cálculos de um usuário, ordenado por data (mais recentes primeiro).
     *
     * @param usuarioId ID do usuário
     * @return List<HistoricoCalculadora> lista do histórico (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<HistoricoCalculadora> buscarPorUsuario(Long usuarioId) throws SQLException {
        String sql = "SELECT * FROM historico_calculadora WHERE usuario_id = ? ORDER BY data_criacao DESC";
        List<HistoricoCalculadora> historico = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    historico.add(mapResultSetToHistorico(rs));
                }
            }

            logger.debug("Encontrados {} registros de histórico para usuário ID {}", historico.size(), usuarioId);
        }

        return historico;
    }

    /**
     * Busca os últimos N cálculos de um usuário.
     *
     * @param limite quantidade máxima de registros a retornar
     * @param usuarioId ID do usuário
     * @return List<HistoricoCalculadora> lista dos últimos cálculos (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<HistoricoCalculadora> buscarUltimos(int limite, Long usuarioId) throws SQLException {
        String sql = """
            SELECT * FROM historico_calculadora
            WHERE usuario_id = ?
            ORDER BY data_criacao DESC
            LIMIT ?
        """;
        List<HistoricoCalculadora> historico = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);
            pstmt.setInt(2, limite);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    historico.add(mapResultSetToHistorico(rs));
                }
            }

            logger.debug("Encontrados {} registros dos últimos {} cálculos para usuário ID {}",
                    historico.size(), limite, usuarioId);
        }

        return historico;
    }

    /**
     * Busca um registro do histórico por ID.
     *
     * @param id ID do registro
     * @return Optional<HistoricoCalculadora> registro encontrado ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<HistoricoCalculadora> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM historico_calculadora WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    HistoricoCalculadora historico = mapResultSetToHistorico(rs);
                    logger.debug("Registro de histórico encontrado: ID {}", id);
                    return Optional.of(historico);
                }
            }
        }

        logger.debug("Registro de histórico com ID {} não encontrado", id);
        return Optional.empty();
    }

    /**
     * Salva um novo cálculo no histórico.
     *
     * @param historico registro do cálculo a ser salvo (sem ID)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return HistoricoCalculadora registro salvo com ID gerado
     * @throws SQLException se houver erro ao salvar
     */
    public HistoricoCalculadora salvar(HistoricoCalculadora historico, Long sessaoId, Long usuarioId)
            throws SQLException {

        String sql = """
            INSERT INTO historico_calculadora (usuario_id, expressao, resultado, tipo_operacao, data_criacao, sessao_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);

            pstmt.setLong(1, usuarioId);
            pstmt.setString(2, historico.getExpressao());
            pstmt.setDouble(3, historico.getResultado());
            pstmt.setString(4, historico.getTipoOperacao());
            pstmt.setString(5, timestamp);
            pstmt.setLong(6, sessaoId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar histórico, nenhuma linha afetada");
            }

            // Obter ID gerado
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {

                if (rs.next()) {
                    historico.setId(rs.getLong(1));
                    historico.setDataCriacao(now);
                    historico.setSessaoId(sessaoId);
                    historico.setUsuarioId(usuarioId);
                } else {
                    throw new SQLException("Falha ao obter ID do histórico criado");
                }
            }

            logger.info("Cálculo salvo no histórico: {} = {} (ID: {})",
                    historico.getExpressao(), historico.getResultado(), historico.getId());
            return historico;
        }
    }

    /**
     * Remove todo o histórico de cálculos de um usuário.
     *
     * @param usuarioId ID do usuário
     * @return int número de registros deletados
     * @throws SQLException se houver erro ao deletar
     */
    public int limparHistorico(Long usuarioId) throws SQLException {
        String sql = "DELETE FROM historico_calculadora WHERE usuario_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);
            int affectedRows = pstmt.executeUpdate();

            logger.info("Histórico limpo para usuário ID {}: {} registros deletados", usuarioId, affectedRows);
            return affectedRows;
        }
    }

    /**
     * Remove um registro específico do histórico.
     *
     * @param id ID do registro a ser removido
     * @throws SQLException se houver erro ao deletar
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM historico_calculadora WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Registro de histórico com ID " + id + " não encontrado");
            }

            logger.info("Registro de histórico com ID {} removido com sucesso", id);
        }
    }

    /**
     * Conta o total de registros no histórico de um usuário.
     *
     * @param usuarioId ID do usuário
     * @return long total de registros
     * @throws SQLException se houver erro ao contar
     */
    public long contarPorUsuario(Long usuarioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM historico_calculadora WHERE usuario_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        }
    }

    /**
     * Busca registros do histórico por tipo de operação.
     *
     * @param tipoOperacao tipo de operação (SOMA, SUBTRACAO, etc.)
     * @param usuarioId ID do usuário
     * @return List<HistoricoCalculadora> lista de registros filtrados
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<HistoricoCalculadora> buscarPorTipoOperacao(String tipoOperacao, Long usuarioId)
            throws SQLException {

        String sql = """
            SELECT * FROM historico_calculadora
            WHERE usuario_id = ? AND tipo_operacao = ?
            ORDER BY data_criacao DESC
        """;
        List<HistoricoCalculadora> historico = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);
            pstmt.setString(2, tipoOperacao);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    historico.add(mapResultSetToHistorico(rs));
                }
            }

            logger.debug("Encontrados {} registros de tipo {} para usuário ID {}",
                    historico.size(), tipoOperacao, usuarioId);
        }

        return historico;
    }

    /**
     * Mapeia um ResultSet para um objeto HistoricoCalculadora.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return HistoricoCalculadora objeto mapeado
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private HistoricoCalculadora mapResultSetToHistorico(ResultSet rs) throws SQLException {
        HistoricoCalculadora historico = new HistoricoCalculadora();
        historico.setId(rs.getLong("id"));
        historico.setUsuarioId(rs.getLong("usuario_id"));
        historico.setExpressao(rs.getString("expressao"));
        historico.setResultado(rs.getDouble("resultado"));
        historico.setTipoOperacao(rs.getString("tipo_operacao"));
        historico.setSessaoId(rs.getLong("sessao_id"));

        // Parse data_criacao usando formato brasileiro
        String dataCriacaoStr = rs.getString("data_criacao");
        if (dataCriacaoStr != null && !dataCriacaoStr.isEmpty()) {
            try {
                historico.setDataCriacao(LocalDateTime.parse(dataCriacaoStr, FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", dataCriacaoStr);
                try {
                    historico.setDataCriacao(LocalDateTime.parse(dataCriacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse da data_criacao: {}", dataCriacaoStr);
                }
            }
        }

        return historico;
    }
}
