package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.AudienciaParticipante;
import com.notisblokk.audiencias.model.Pessoa;
import com.notisblokk.audiencias.model.ProcessoParticipante;
import com.notisblokk.audiencias.model.enums.StatusIntimacao;
import com.notisblokk.audiencias.model.enums.StatusOitiva;
import com.notisblokk.audiencias.model.enums.TipoParticipacao;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Participantes de Audiências.
 *
 * Gerencia participantes do processo que foram selecionados para audiências específicas,
 * incluindo controle de intimação e oitiva.
 */
public class AudienciaParticipanteRepository {

    private static final Logger logger = LoggerFactory.getLogger(AudienciaParticipanteRepository.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Busca todos os participantes de uma audiência com dados completos.
     *
     * @param audienciaId ID da audiência
     * @return List de participantes da audiência
     * @throws SQLException se houver erro no banco de dados
     */
    public List<AudienciaParticipante> buscarPorAudiencia(Long audienciaId) throws SQLException {
        String sql = """
            SELECT ap.*, pp.*, p.*
            FROM audiencia_participante ap
            INNER JOIN processo_participante pp ON ap.processo_participante_id = pp.id
            INNER JOIN pessoa p ON pp.pessoa_id = p.id
            WHERE ap.audiencia_id = ?
            ORDER BY pp.tipo_participacao, p.nome
        """;

        List<AudienciaParticipante> participantes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, audienciaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    participantes.add(mapResultSetToAudienciaParticipante(rs));
                }
            }
        }

        logger.debug("Buscados {} participantes para audiência ID {}", participantes.size(), audienciaId);
        return participantes;
    }

    /**
     * Busca participantes não intimados de uma audiência.
     *
     * @param audienciaId ID da audiência
     * @return List de participantes não intimados
     * @throws SQLException se houver erro no banco de dados
     */
    public List<AudienciaParticipante> buscarNaoIntimados(Long audienciaId) throws SQLException {
        String sql = """
            SELECT ap.*, pp.*, p.*
            FROM audiencia_participante ap
            INNER JOIN processo_participante pp ON ap.processo_participante_id = pp.id
            INNER JOIN pessoa p ON pp.pessoa_id = p.id
            WHERE ap.audiencia_id = ? AND ap.status_intimacao = 'NAO_INTIMADA'
            ORDER BY p.nome
        """;

        List<AudienciaParticipante> participantes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, audienciaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    participantes.add(mapResultSetToAudienciaParticipante(rs));
                }
            }
        }

        return participantes;
    }

    /**
     * Busca participantes aguardando intimação de uma audiência.
     *
     * @param audienciaId ID da audiência
     * @return List de participantes aguardando intimação
     * @throws SQLException se houver erro no banco de dados
     */
    public List<AudienciaParticipante> buscarAguardandoIntimacao(Long audienciaId) throws SQLException {
        String sql = """
            SELECT ap.*, pp.*, p.*
            FROM audiencia_participante ap
            INNER JOIN processo_participante pp ON ap.processo_participante_id = pp.id
            INNER JOIN pessoa p ON pp.pessoa_id = p.id
            WHERE ap.audiencia_id = ? AND ap.status_intimacao = 'AGUARDANDO_INTIMACAO'
            ORDER BY p.nome
        """;

        List<AudienciaParticipante> participantes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, audienciaId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    participantes.add(mapResultSetToAudienciaParticipante(rs));
                }
            }
        }

        return participantes;
    }

    /**
     * Busca um participante de audiência por ID.
     *
     * @param id ID do participante de audiência
     * @return Optional contendo o participante se encontrado
     * @throws SQLException se houver erro no banco de dados
     */
    public Optional<AudienciaParticipante> buscarPorId(Long id) throws SQLException {
        String sql = """
            SELECT ap.*, pp.*, p.*
            FROM audiencia_participante ap
            INNER JOIN processo_participante pp ON ap.processo_participante_id = pp.id
            INNER JOIN pessoa p ON pp.pessoa_id = p.id
            WHERE ap.id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAudienciaParticipante(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Salva um novo participante na audiência.
     *
     * @param participante Participante a ser salvo
     * @return ID do participante criado
     * @throws SQLException se houver erro no banco de dados
     */
    public Long salvar(AudienciaParticipante participante) throws SQLException {
        String sql = """
            INSERT INTO audiencia_participante
            (audiencia_id, processo_participante_id, status_intimacao, status_oitiva,
             observacoes_desistencia, data_oitiva_anterior, observacoes_oitiva, presente, observacoes,
             criado_em, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        logger.debug("Salvando participante: Audiência ID {}, Processo Participante ID {}",
            participante.getAudienciaId(), participante.getProcessoParticipanteId());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String timestamp = LocalDateTime.now().format(DATETIME_FORMATTER);

            pstmt.setLong(1, participante.getAudienciaId());
            pstmt.setLong(2, participante.getProcessoParticipanteId());
            pstmt.setString(3, participante.getStatusIntimacao().name());
            pstmt.setString(4, participante.getStatusOitiva().name());
            pstmt.setString(5, participante.getObservacoesDesistencia());

            // Data oitiva anterior
            if (participante.getDataOitivaAnterior() != null) {
                pstmt.setString(6, participante.getDataOitivaAnterior().format(DATE_FORMATTER));
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }

            pstmt.setString(7, participante.getObservacoesOitiva());
            pstmt.setInt(8, participante.getPresente() ? 1 : 0);
            pstmt.setString(9, participante.getObservacoes());
            pstmt.setString(10, timestamp);
            pstmt.setString(11, timestamp);

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    logger.info("Participante de audiência criado com ID: {}", id);
                    return id;
                }
                throw new SQLException("Falha ao obter ID do participante criado");
            }
        }
    }

    /**
     * Atualiza um participante de audiência existente.
     *
     * @param participante Participante com dados atualizados
     * @throws SQLException se houver erro no banco de dados
     */
    public void atualizar(AudienciaParticipante participante) throws SQLException {
        String sql = """
            UPDATE audiencia_participante
            SET status_intimacao = ?, status_oitiva = ?, observacoes_desistencia = ?,
                data_oitiva_anterior = ?, observacoes_oitiva = ?, presente = ?, observacoes = ?,
                atualizado_em = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String timestamp = LocalDateTime.now().format(DATETIME_FORMATTER);

            pstmt.setString(1, participante.getStatusIntimacao().name());
            pstmt.setString(2, participante.getStatusOitiva().name());
            pstmt.setString(3, participante.getObservacoesDesistencia());

            // Data oitiva anterior
            if (participante.getDataOitivaAnterior() != null) {
                pstmt.setString(4, participante.getDataOitivaAnterior().format(DATE_FORMATTER));
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }

            pstmt.setString(5, participante.getObservacoesOitiva());
            pstmt.setInt(6, participante.getPresente() ? 1 : 0);
            pstmt.setString(7, participante.getObservacoes());
            pstmt.setString(8, timestamp);
            pstmt.setLong(9, participante.getId());

            pstmt.executeUpdate();
            logger.debug("Participante de audiência atualizado: ID {}", participante.getId());
        }
    }

    /**
     * Deleta um participante da audiência.
     *
     * @param id ID do participante
     * @throws SQLException se houver erro no banco de dados
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM audiencia_participante WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.info("Participante de audiência deletado: ID {}", id);
        }
    }

    /**
     * Mapeia ResultSet para objeto AudienciaParticipante com ProcessoParticipante e Pessoa.
     *
     * @param rs ResultSet posicionado na linha
     * @return AudienciaParticipante mapeado
     * @throws SQLException se houver erro ao ler ResultSet
     */
    private AudienciaParticipante mapResultSetToAudienciaParticipante(ResultSet rs) throws SQLException {
        AudienciaParticipante ap = new AudienciaParticipante();
        ap.setId(rs.getLong("id"));
        ap.setAudienciaId(rs.getLong("audiencia_id"));
        ap.setProcessoParticipanteId(rs.getLong("processo_participante_id"));
        ap.setStatusIntimacao(StatusIntimacao.valueOf(rs.getString("status_intimacao")));
        ap.setStatusOitiva(StatusOitiva.valueOf(rs.getString("status_oitiva")));
        ap.setObservacoesDesistencia(rs.getString("observacoes_desistencia"));

        // Data oitiva anterior
        String dataOitiva = rs.getString("data_oitiva_anterior");
        if (dataOitiva != null && !dataOitiva.isEmpty()) {
            ap.setDataOitivaAnterior(LocalDate.parse(dataOitiva, DATE_FORMATTER));
        }

        ap.setObservacoesOitiva(rs.getString("observacoes_oitiva"));
        ap.setPresente(rs.getInt("presente") == 1);
        ap.setObservacoes(rs.getString("observacoes"));

        // Datas de auditoria
        String criadoEm = rs.getString("criado_em");
        String atualizadoEm = rs.getString("atualizado_em");
        if (criadoEm != null) {
            ap.setCriadoEm(LocalDateTime.parse(criadoEm, DATETIME_FORMATTER));
        }
        if (atualizadoEm != null) {
            ap.setAtualizadoEm(LocalDateTime.parse(atualizadoEm, DATETIME_FORMATTER));
        }

        // Mapear ProcessoParticipante
        ProcessoParticipante pp = new ProcessoParticipante();
        pp.setId(rs.getLong("processo_participante.id"));
        pp.setProcessoId(rs.getLong("processo_participante.processo_id"));
        pp.setPessoaId(rs.getLong("processo_participante.pessoa_id"));
        pp.setTipoParticipacao(TipoParticipacao.valueOf(rs.getString("processo_participante.tipo_participacao")));
        pp.setObservacoes(rs.getString("processo_participante.observacoes"));

        // Mapear Pessoa dentro do ProcessoParticipante
        Pessoa pessoa = new Pessoa();
        pessoa.setId(rs.getLong("pessoa.id"));
        pessoa.setNome(rs.getString("pessoa.nome"));
        pessoa.setCpf(rs.getString("pessoa.cpf"));
        pessoa.setTelefone(rs.getString("pessoa.telefone"));
        pessoa.setEmail(rs.getString("pessoa.email"));
        pessoa.setObservacoes(rs.getString("pessoa.observacoes"));

        pp.setPessoa(pessoa);
        ap.setProcessoParticipante(pp);

        return ap;
    }
}
