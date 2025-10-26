package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.Nota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório responsável pelo acesso a dados de notas.
 *
 * <p>Implementa operações CRUD (Create, Read, Update, Delete) para a entidade Nota,
 * utilizando PreparedStatements para prevenir SQL injection.</p>
 *
 * <p><b>IMPORTANTE:</b> Ajusta timestamps do SQLite (UTC) para UTC-3 (horário de Brasília).</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class NotaRepository {

    private static final Logger logger = LoggerFactory.getLogger(NotaRepository.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Busca todas as notas do sistema, ordenadas por prazo final (mais urgentes primeiro).
     *
     * @return List<Nota> lista de todas as notas (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Nota> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM notas ORDER BY prazo_final ASC, data_criacao DESC";
        List<Nota> notas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                notas.add(mapResultSetToNota(rs));
            }

            logger.debug("Encontradas {} notas", notas.size());
        }

        return notas;
    }

    /**
     * Busca uma nota por ID.
     *
     * @param id ID da nota
     * @return Optional<Nota> nota encontrada ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<Nota> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM notas WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Nota nota = mapResultSetToNota(rs);
                    logger.debug("Nota encontrada: {}", nota.getTitulo());
                    return Optional.of(nota);
                }
            }
        }

        logger.debug("Nota com ID {} não encontrada", id);
        return Optional.empty();
    }

    /**
     * Busca notas por etiqueta.
     *
     * @param etiquetaId ID da etiqueta
     * @return List<Nota> lista de notas com a etiqueta especificada
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Nota> buscarPorEtiqueta(Long etiquetaId) throws SQLException {
        String sql = "SELECT * FROM notas WHERE etiqueta_id = ? ORDER BY prazo_final ASC";
        List<Nota> notas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, etiquetaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notas.add(mapResultSetToNota(rs));
                }
            }

            logger.debug("Encontradas {} notas com etiqueta_id {}", notas.size(), etiquetaId);
        }

        return notas;
    }

    /**
     * Busca notas por status.
     *
     * @param statusId ID do status
     * @return List<Nota> lista de notas com o status especificado
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Nota> buscarPorStatus(Long statusId) throws SQLException {
        String sql = "SELECT * FROM notas WHERE status_id = ? ORDER BY prazo_final ASC";
        List<Nota> notas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, statusId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notas.add(mapResultSetToNota(rs));
                }
            }

            logger.debug("Encontradas {} notas com status_id {}", notas.size(), statusId);
        }

        return notas;
    }

    /**
     * Salva uma nova nota no banco de dados.
     *
     * @param nota nota a ser salva (sem ID)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return Nota nota salva com ID gerado
     * @throws SQLException se houver erro ao salvar
     */
    public Nota salvar(Nota nota, Long sessaoId, Long usuarioId) throws SQLException {
        String sql = """
            INSERT INTO notas (etiqueta_id, status_id, titulo, conteudo, data_criacao, data_atualizacao, prazo_final, sessao_id, usuario_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);
            String prazoFinal = nota.getPrazoFinal().format(DATE_FORMATTER);

            pstmt.setLong(1, nota.getEtiquetaId());
            pstmt.setLong(2, nota.getStatusId());
            pstmt.setString(3, nota.getTitulo());
            pstmt.setString(4, nota.getConteudo());
            pstmt.setString(5, timestamp);
            pstmt.setString(6, timestamp);
            pstmt.setString(7, prazoFinal);
            pstmt.setLong(8, sessaoId);
            pstmt.setLong(9, usuarioId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar nota, nenhuma linha afetada");
            }

            // Obter ID gerado
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {

                if (rs.next()) {
                    nota.setId(rs.getLong(1));
                    nota.setDataCriacao(now);
                    nota.setDataAtualizacao(now);
                    nota.setSessaoId(sessaoId);
                    nota.setUsuarioId(usuarioId);
                } else {
                    throw new SQLException("Falha ao obter ID da nota criada");
                }
            }

            logger.info("Nota salva com sucesso: {} (ID: {})", nota.getTitulo(), nota.getId());
            return nota;
        }
    }

    /**
     * Atualiza uma nota existente no banco de dados.
     *
     * @param nota nota a ser atualizada (com ID)
     * @throws SQLException se houver erro ao atualizar
     */
    public void atualizar(Nota nota) throws SQLException {
        String sql = """
            UPDATE notas
            SET etiqueta_id = ?, status_id = ?, titulo = ?, conteudo = ?, data_atualizacao = ?, prazo_final = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);
            String prazoFinal = nota.getPrazoFinal().format(DATE_FORMATTER);

            pstmt.setLong(1, nota.getEtiquetaId());
            pstmt.setLong(2, nota.getStatusId());
            pstmt.setString(3, nota.getTitulo());
            pstmt.setString(4, nota.getConteudo());
            pstmt.setString(5, timestamp);
            pstmt.setString(6, prazoFinal);
            pstmt.setLong(7, nota.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Nota com ID " + nota.getId() + " não encontrada");
            }

            nota.setDataAtualizacao(now);

            logger.info("Nota atualizada com sucesso: {} (ID: {})", nota.getTitulo(), nota.getId());
        }
    }

    /**
     * Remove uma nota do banco de dados.
     *
     * @param id ID da nota a ser removida
     * @throws SQLException se houver erro ao deletar
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM notas WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Nota com ID " + id + " não encontrada");
            }

            logger.info("Nota com ID {} removida com sucesso", id);
        }
    }

    /**
     * Conta o total de notas no sistema.
     *
     * @return long total de notas
     * @throws SQLException se houver erro ao contar
     */
    public long contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM notas";

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
     * Mapeia um ResultSet para um objeto Nota.
     * IMPORTANTE: Ajusta timestamps do SQLite para o horário de Brasília.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return Nota objeto mapeado
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private Nota mapResultSetToNota(ResultSet rs) throws SQLException {
        Nota nota = new Nota();
        nota.setId(rs.getLong("id"));
        nota.setEtiquetaId(rs.getLong("etiqueta_id"));
        nota.setStatusId(rs.getLong("status_id"));
        nota.setTitulo(rs.getString("titulo"));
        nota.setConteudo(rs.getString("conteudo"));
        nota.setSessaoId(rs.getLong("sessao_id"));
        nota.setUsuarioId(rs.getLong("usuario_id"));

        // Parse data_criacao usando formato brasileiro
        String dataCriacaoStr = rs.getString("data_criacao");
        if (dataCriacaoStr != null && !dataCriacaoStr.isEmpty()) {
            try {
                nota.setDataCriacao(LocalDateTime.parse(dataCriacaoStr, FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", dataCriacaoStr);
                try {
                    nota.setDataCriacao(LocalDateTime.parse(dataCriacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse da data_criacao: {}", dataCriacaoStr);
                }
            }
        }

        // Parse data_atualizacao usando formato brasileiro
        String dataAtualizacaoStr = rs.getString("data_atualizacao");
        if (dataAtualizacaoStr != null && !dataAtualizacaoStr.isEmpty()) {
            try {
                nota.setDataAtualizacao(LocalDateTime.parse(dataAtualizacaoStr, FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", dataAtualizacaoStr);
                try {
                    nota.setDataAtualizacao(LocalDateTime.parse(dataAtualizacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse da data_atualizacao: {}", dataAtualizacaoStr);
                }
            }
        }

        // Parse prazo_final (tentar formato brasileiro primeiro, depois ISO)
        String prazoFinalStr = rs.getString("prazo_final");
        if (prazoFinalStr != null && !prazoFinalStr.isEmpty()) {
            try {
                // Tentar formato brasileiro dd/MM/yyyy primeiro
                nota.setPrazoFinal(LocalDate.parse(prazoFinalStr, DATE_FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", prazoFinalStr);
                try {
                    // Fallback: tentar formato ISO yyyy-MM-dd
                    nota.setPrazoFinal(LocalDate.parse(prazoFinalStr));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse do prazo_final: {}", prazoFinalStr);
                }
            }
        }

        return nota;
    }
}
