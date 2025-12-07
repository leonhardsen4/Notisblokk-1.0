package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.Processo;
import com.notisblokk.audiencias.model.Vara;
import com.notisblokk.audiencias.model.enums.Competencia;
import com.notisblokk.audiencias.model.enums.StatusProcesso;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Processos.
 *
 * Responsável por todas as operações de banco de dados relacionadas a processos judiciais.
 */
public class ProcessoRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProcessoRepository.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final VaraRepository varaRepository = new VaraRepository();

    /**
     * Busca todos os processos ordenados por data de criação (mais recente primeiro).
     *
     * @return List de processos com Vara carregada
     * @throws SQLException se houver erro no banco de dados
     */
    public List<Processo> buscarTodos() throws SQLException {
        String sql = """
            SELECT p.*, v.*
            FROM processo p
            INNER JOIN vara v ON p.vara_id = v.id
            ORDER BY p.criado_em DESC
        """;

        List<Processo> processos = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                processos.add(mapResultSetToProcesso(rs, true));
            }
        }

        logger.debug("Buscados {} processos", processos.size());
        return processos;
    }

    /**
     * Busca um processo por ID com Vara carregada.
     *
     * @param id ID do processo
     * @return Optional contendo o processo se encontrado
     * @throws SQLException se houver erro no banco de dados
     */
    public Optional<Processo> buscarPorId(Long id) throws SQLException {
        String sql = """
            SELECT p.*, v.*
            FROM processo p
            INNER JOIN vara v ON p.vara_id = v.id
            WHERE p.id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProcesso(rs, true));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Busca um processo pelo número do processo.
     *
     * @param numeroProcesso Número do processo
     * @return Optional contendo o processo se encontrado
     * @throws SQLException se houver erro no banco de dados
     */
    public Optional<Processo> buscarPorNumero(String numeroProcesso) throws SQLException {
        String sql = """
            SELECT p.*, v.*
            FROM processo p
            INNER JOIN vara v ON p.vara_id = v.id
            WHERE p.numero_processo = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, numeroProcesso);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProcesso(rs, true));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Busca processos por vara.
     *
     * @param varaId ID da vara
     * @return List de processos da vara
     * @throws SQLException se houver erro no banco de dados
     */
    public List<Processo> buscarPorVara(Long varaId) throws SQLException {
        String sql = """
            SELECT p.*, v.*
            FROM processo p
            INNER JOIN vara v ON p.vara_id = v.id
            WHERE p.vara_id = ?
            ORDER BY p.criado_em DESC
        """;

        List<Processo> processos = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, varaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    processos.add(mapResultSetToProcesso(rs, true));
                }
            }
        }

        return processos;
    }

    /**
     * Busca processos por status.
     *
     * @param status Status do processo
     * @return List de processos com o status especificado
     * @throws SQLException se houver erro no banco de dados
     */
    public List<Processo> buscarPorStatus(StatusProcesso status) throws SQLException {
        String sql = """
            SELECT p.*, v.*
            FROM processo p
            INNER JOIN vara v ON p.vara_id = v.id
            WHERE p.status = ?
            ORDER BY p.criado_em DESC
        """;

        List<Processo> processos = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    processos.add(mapResultSetToProcesso(rs, true));
                }
            }
        }

        return processos;
    }

    /**
     * Salva um novo processo no banco de dados.
     *
     * @param processo Processo a ser salvo
     * @return ID do processo criado
     * @throws SQLException se houver erro no banco de dados
     */
    public Long salvar(Processo processo) throws SQLException {
        String sql = """
            INSERT INTO processo (numero_processo, competencia, artigo, vara_id, status, observacoes, criado_em, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        logger.debug("Salvando processo: {}", processo.getNumeroProcesso());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String timestamp = LocalDateTime.now().format(FORMATTER);

            pstmt.setString(1, processo.getNumeroProcesso());
            pstmt.setString(2, processo.getCompetencia().name());
            pstmt.setString(3, processo.getArtigo());
            pstmt.setLong(4, processo.getVara().getId());
            pstmt.setString(5, processo.getStatus().name());
            pstmt.setString(6, processo.getObservacoes());
            pstmt.setString(7, timestamp);
            pstmt.setString(8, timestamp);

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    logger.info("Processo criado com ID: {}", id);
                    return id;
                }
                throw new SQLException("Falha ao obter ID do processo criado");
            }
        }
    }

    /**
     * Atualiza um processo existente.
     *
     * @param processo Processo com dados atualizados
     * @throws SQLException se houver erro no banco de dados
     */
    public void atualizar(Processo processo) throws SQLException {
        String sql = """
            UPDATE processo
            SET numero_processo = ?, competencia = ?, artigo = ?, vara_id = ?, status = ?, observacoes = ?, atualizado_em = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String timestamp = LocalDateTime.now().format(FORMATTER);

            pstmt.setString(1, processo.getNumeroProcesso());
            pstmt.setString(2, processo.getCompetencia().name());
            pstmt.setString(3, processo.getArtigo());
            pstmt.setLong(4, processo.getVara().getId());
            pstmt.setString(5, processo.getStatus().name());
            pstmt.setString(6, processo.getObservacoes());
            pstmt.setString(7, timestamp);
            pstmt.setLong(8, processo.getId());

            pstmt.executeUpdate();
            logger.debug("Processo atualizado: ID {}", processo.getId());
        }
    }

    /**
     * Deleta um processo.
     * Deleta em CASCADE todas as audiências e participantes vinculados.
     *
     * @param id ID do processo
     * @throws SQLException se houver erro no banco de dados
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM processo WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.info("Processo deletado: ID {}", id);
        }
    }

    /**
     * Mapeia ResultSet para objeto Processo.
     *
     * @param rs ResultSet posicionado na linha
     * @param incluirVara Se true, carrega objeto Vara completo
     * @return Processo mapeado
     * @throws SQLException se houver erro ao ler ResultSet
     */
    private Processo mapResultSetToProcesso(ResultSet rs, boolean incluirVara) throws SQLException {
        Processo processo = new Processo();
        processo.setId(rs.getLong("id"));
        processo.setNumeroProcesso(rs.getString("numero_processo"));
        processo.setCompetencia(Competencia.valueOf(rs.getString("competencia")));
        processo.setArtigo(rs.getString("artigo"));
        processo.setStatus(StatusProcesso.valueOf(rs.getString("status")));
        processo.setObservacoes(rs.getString("observacoes"));

        // Datas de auditoria
        String criadoEm = rs.getString("criado_em");
        String atualizadoEm = rs.getString("atualizado_em");
        if (criadoEm != null) {
            processo.setCriadoEm(LocalDateTime.parse(criadoEm, FORMATTER));
        }
        if (atualizadoEm != null) {
            processo.setAtualizadoEm(LocalDateTime.parse(atualizadoEm, FORMATTER));
        }

        // Carregar Vara se join foi feito
        if (incluirVara) {
            Vara vara = new Vara();
            vara.setId(rs.getLong("vara.id"));
            vara.setNome(rs.getString("vara.nome"));
            vara.setComarca(rs.getString("vara.comarca"));
            vara.setEndereco(rs.getString("vara.endereco"));
            vara.setTelefone(rs.getString("vara.telefone"));
            vara.setEmail(rs.getString("vara.email"));
            vara.setObservacoes(rs.getString("vara.observacoes"));
            processo.setVara(vara);
        }

        return processo;
    }
}
