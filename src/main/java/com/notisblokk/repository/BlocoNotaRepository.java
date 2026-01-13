package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.BlocoNota;
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
import java.util.Optional;

/**
 * Repositório responsável pelo acesso a dados do bloco de notas.
 *
 * <p>Implementa operações CRUD para a entidade BlocoNota.
 * Cada usuário possui um único documento de bloco de notas.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class BlocoNotaRepository {

    private static final Logger logger = LoggerFactory.getLogger(BlocoNotaRepository.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Busca o bloco de notas de um usuário.
     *
     * @param usuarioId ID do usuário
     * @return Optional<BlocoNota> documento encontrado ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<BlocoNota> buscarPorUsuario(Long usuarioId) throws SQLException {
        String sql = "SELECT * FROM bloco_notas WHERE usuario_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BlocoNota nota = mapResultSetToBlocoNota(rs);
                    logger.debug("Bloco de notas encontrado para usuário ID {}", usuarioId);
                    return Optional.of(nota);
                }
            }
        }

        logger.debug("Bloco de notas não encontrado para usuário ID {}", usuarioId);
        return Optional.empty();
    }

    /**
     * Cria um novo bloco de notas para um usuário.
     *
     * @param nota bloco de notas a ser criado (sem ID)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return BlocoNota bloco criado com ID gerado
     * @throws SQLException se houver erro ao criar (incluindo violação de UNIQUE constraint)
     */
    public BlocoNota criar(BlocoNota nota, Long sessaoId, Long usuarioId) throws SQLException {
        String sql = """
            INSERT INTO bloco_notas (usuario_id, conteudo_markdown, conteudo_html, data_criacao, data_atualizacao, sessao_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);

            pstmt.setLong(1, usuarioId);
            pstmt.setString(2, nota.getConteudoMarkdown() != null ? nota.getConteudoMarkdown() : "");
            pstmt.setString(3, nota.getConteudoHtml() != null ? nota.getConteudoHtml() : "");
            pstmt.setString(4, timestamp);
            pstmt.setString(5, timestamp);
            pstmt.setLong(6, sessaoId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar bloco de notas, nenhuma linha afetada");
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
                    throw new SQLException("Falha ao obter ID do bloco de notas criado");
                }
            }

            logger.info("Bloco de notas criado para usuário ID {} (ID: {})", usuarioId, nota.getId());
            return nota;
        }
    }

    /**
     * Atualiza um bloco de notas existente.
     *
     * @param nota bloco de notas a ser atualizado (com ID)
     * @throws SQLException se houver erro ao atualizar
     */
    public void atualizar(BlocoNota nota) throws SQLException {
        String sql = """
            UPDATE bloco_notas
            SET conteudo_markdown = ?, conteudo_html = ?, data_atualizacao = ?, sessao_id = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);

            pstmt.setString(1, nota.getConteudoMarkdown() != null ? nota.getConteudoMarkdown() : "");
            pstmt.setString(2, nota.getConteudoHtml() != null ? nota.getConteudoHtml() : "");
            pstmt.setString(3, timestamp);
            pstmt.setLong(4, nota.getSessaoId());
            pstmt.setLong(5, nota.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Bloco de notas com ID " + nota.getId() + " não encontrado");
            }

            nota.setDataAtualizacao(now);

            logger.info("Bloco de notas ID {} atualizado", nota.getId());
        }
    }

    /**
     * Obtém o bloco de notas do usuário ou cria um novo se não existir.
     *
     * <p>Este método é uma conveniência que combina busca e criação,
     * garantindo que sempre haverá um documento para o usuário.</p>
     *
     * @param usuarioId ID do usuário
     * @param sessaoId ID da sessão atual
     * @return BlocoNota documento existente ou recém-criado
     * @throws SQLException se houver erro ao acessar o banco
     */
    public BlocoNota obterOuCriar(Long usuarioId, Long sessaoId) throws SQLException {
        Optional<BlocoNota> notaOpt = buscarPorUsuario(usuarioId);

        if (notaOpt.isPresent()) {
            logger.debug("Retornando bloco de notas existente para usuário ID {}", usuarioId);
            return notaOpt.get();
        }

        // Criar novo bloco de notas vazio
        BlocoNota novaNota = new BlocoNota();
        novaNota.setConteudoMarkdown("");
        novaNota.setConteudoHtml("");

        logger.info("Criando novo bloco de notas para usuário ID {}", usuarioId);
        return criar(novaNota, sessaoId, usuarioId);
    }

    /**
     * Deleta o bloco de notas de um usuário.
     *
     * @param usuarioId ID do usuário
     * @throws SQLException se houver erro ao deletar
     */
    public void deletar(Long usuarioId) throws SQLException {
        String sql = "DELETE FROM bloco_notas WHERE usuario_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Bloco de notas do usuário ID " + usuarioId + " não encontrado");
            }

            logger.info("Bloco de notas do usuário ID {} deletado", usuarioId);
        }
    }

    /**
     * Mapeia um ResultSet para um objeto BlocoNota.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return BlocoNota objeto mapeado
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private BlocoNota mapResultSetToBlocoNota(ResultSet rs) throws SQLException {
        BlocoNota nota = new BlocoNota();
        nota.setId(rs.getLong("id"));
        nota.setUsuarioId(rs.getLong("usuario_id"));
        nota.setConteudoMarkdown(rs.getString("conteudo_markdown"));
        nota.setConteudoHtml(rs.getString("conteudo_html"));
        nota.setSessaoId(rs.getLong("sessao_id"));

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

        return nota;
    }
}
