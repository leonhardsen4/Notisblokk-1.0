package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.Pessoa;
import com.notisblokk.audiencias.model.ProcessoParticipante;
import com.notisblokk.audiencias.model.enums.TipoParticipacao;
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
 * Repositório para acesso a dados de Participantes de Processos.
 *
 * Gerencia a relação entre Pessoa e Processo (quem participa de qual processo).
 */
public class ProcessoParticipanteRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProcessoParticipanteRepository.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Busca todos os participantes de um processo com dados da Pessoa carregados.
     *
     * @param processoId ID do processo
     * @return List de participantes do processo
     * @throws SQLException se houver erro no banco de dados
     */
    public List<ProcessoParticipante> buscarPorProcesso(Long processoId) throws SQLException {
        String sql = """
            SELECT pp.*, p.*
            FROM processo_participante pp
            INNER JOIN pessoa p ON pp.pessoa_id = p.id
            WHERE pp.processo_id = ?
            ORDER BY pp.tipo_participacao, p.nome
        """;

        List<ProcessoParticipante> participantes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, processoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    participantes.add(mapResultSetToParticipante(rs, true));
                }
            }
        }

        logger.debug("Buscados {} participantes para processo ID {}", participantes.size(), processoId);
        return participantes;
    }

    /**
     * Busca um participante por ID.
     *
     * @param id ID do participante
     * @return Optional contendo o participante se encontrado
     * @throws SQLException se houver erro no banco de dados
     */
    public Optional<ProcessoParticipante> buscarPorId(Long id) throws SQLException {
        String sql = """
            SELECT pp.*, p.*
            FROM processo_participante pp
            INNER JOIN pessoa p ON pp.pessoa_id = p.id
            WHERE pp.id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToParticipante(rs, true));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Verifica se já existe uma participação específica.
     *
     * @param processoId ID do processo
     * @param pessoaId ID da pessoa
     * @param tipo Tipo de participação
     * @return true se a participação já existe
     * @throws SQLException se houver erro no banco de dados
     */
    public boolean existeParticipacao(Long processoId, Long pessoaId, TipoParticipacao tipo) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM processo_participante
            WHERE processo_id = ? AND pessoa_id = ? AND tipo_participacao = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, processoId);
            pstmt.setLong(2, pessoaId);
            pstmt.setString(3, tipo.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    /**
     * Salva um novo participante no processo.
     *
     * @param participante Participante a ser salvo
     * @return ID do participante criado
     * @throws SQLException se houver erro no banco de dados
     */
    public Long salvar(ProcessoParticipante participante) throws SQLException {
        String sql = """
            INSERT INTO processo_participante (processo_id, pessoa_id, tipo_participacao, observacoes, criado_em)
            VALUES (?, ?, ?, ?, ?)
        """;

        logger.debug("Salvando participante: Processo ID {}, Pessoa ID {}, Tipo {}",
            participante.getProcessoId(), participante.getPessoaId(), participante.getTipoParticipacao());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String timestamp = LocalDateTime.now().format(FORMATTER);

            pstmt.setLong(1, participante.getProcessoId());
            pstmt.setLong(2, participante.getPessoaId());
            pstmt.setString(3, participante.getTipoParticipacao().name());
            pstmt.setString(4, participante.getObservacoes());
            pstmt.setString(5, timestamp);

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    logger.info("Participante criado com ID: {}", id);
                    return id;
                }
                throw new SQLException("Falha ao obter ID do participante criado");
            }
        }
    }

    /**
     * Atualiza um participante existente.
     *
     * @param participante Participante com dados atualizados
     * @throws SQLException se houver erro no banco de dados
     */
    public void atualizar(ProcessoParticipante participante) throws SQLException {
        String sql = """
            UPDATE processo_participante
            SET tipo_participacao = ?, observacoes = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, participante.getTipoParticipacao().name());
            pstmt.setString(2, participante.getObservacoes());
            pstmt.setLong(3, participante.getId());

            pstmt.executeUpdate();
            logger.debug("Participante atualizado: ID {}", participante.getId());
        }
    }

    /**
     * Deleta um participante do processo.
     *
     * @param id ID do participante
     * @throws SQLException se houver erro no banco de dados
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM processo_participante WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.info("Participante deletado: ID {}", id);
        }
    }

    /**
     * Mapeia ResultSet para objeto ProcessoParticipante.
     *
     * @param rs ResultSet posicionado na linha
     * @param incluirPessoa Se true, carrega objeto Pessoa completo
     * @return ProcessoParticipante mapeado
     * @throws SQLException se houver erro ao ler ResultSet
     */
    private ProcessoParticipante mapResultSetToParticipante(ResultSet rs, boolean incluirPessoa) throws SQLException {
        ProcessoParticipante participante = new ProcessoParticipante();
        participante.setId(rs.getLong("id"));
        participante.setProcessoId(rs.getLong("processo_id"));
        participante.setPessoaId(rs.getLong("pessoa_id"));
        participante.setTipoParticipacao(TipoParticipacao.valueOf(rs.getString("tipo_participacao")));
        participante.setObservacoes(rs.getString("observacoes"));

        // Data de criação
        String criadoEm = rs.getString("criado_em");
        if (criadoEm != null) {
            participante.setCriadoEm(LocalDateTime.parse(criadoEm, FORMATTER));
        }

        // Carregar Pessoa se join foi feito
        if (incluirPessoa) {
            Pessoa pessoa = new Pessoa();
            pessoa.setId(rs.getLong("pessoa.id"));
            pessoa.setNome(rs.getString("pessoa.nome"));
            pessoa.setCpf(rs.getString("pessoa.cpf"));
            pessoa.setTelefone(rs.getString("pessoa.telefone"));
            pessoa.setEmail(rs.getString("pessoa.email"));
            pessoa.setObservacoes(rs.getString("pessoa.observacoes"));
            participante.setPessoa(pessoa);
        }

        return participante;
    }
}
