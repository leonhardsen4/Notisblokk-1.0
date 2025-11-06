package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.*;
import com.notisblokk.audiencias.model.enums.TipoParticipacao;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Participação em Audiências.
 */
public class ParticipacaoAudienciaRepository {

    private static final Logger logger = LoggerFactory.getLogger(ParticipacaoAudienciaRepository.class);

    public List<ParticipacaoAudiencia> buscarPorAudiencia(Long audienciaId) throws SQLException {
        String sql = "SELECT * FROM participacao_audiencia WHERE audiencia_id = ?";
        List<ParticipacaoAudiencia> participacoes = new ArrayList<>();

        System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaRepository.buscarPorAudiencia() - ID: " + audienciaId);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, audienciaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    participacoes.add(mapResultSetToParticipacao(rs));
                }
            }
        }
        return participacoes;
    }

    public List<ParticipacaoAudiencia> buscarPorPessoa(Long pessoaId) throws SQLException {
        String sql = "SELECT * FROM participacao_audiencia WHERE pessoa_id = ?";
        List<ParticipacaoAudiencia> participacoes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, pessoaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    participacoes.add(mapResultSetToParticipacao(rs));
                }
            }
        }
        return participacoes;
    }

    public Optional<ParticipacaoAudiencia> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM participacao_audiencia WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToParticipacao(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Long salvar(ParticipacaoAudiencia participacao) throws SQLException {
        String sql = """
            INSERT INTO participacao_audiencia (audiencia_id, pessoa_id, tipo, intimado, observacoes)
            VALUES (?, ?, ?, ?, ?)
            """;

        System.out.println("DEBUG_AUDIENCIAS: ParticipacaoAudienciaRepository.salvar() - " +
            "Audiência: " + participacao.getAudiencia().getId() +
            ", Pessoa: " + participacao.getPessoa().getId() +
            ", Tipo: " + participacao.getTipo());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, participacao.getAudiencia().getId());
            pstmt.setLong(2, participacao.getPessoa().getId());
            pstmt.setString(3, participacao.getTipo().name());
            pstmt.setInt(4, participacao.getIntimado() ? 1 : 0);
            pstmt.setString(5, participacao.getObservacoes());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new SQLException("Falha ao obter ID");
            }
        }
    }

    public void atualizar(ParticipacaoAudiencia participacao) throws SQLException {
        String sql = """
            UPDATE participacao_audiencia
            SET audiencia_id = ?, pessoa_id = ?, tipo = ?, intimado = ?, observacoes = ?
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, participacao.getAudiencia().getId());
            pstmt.setLong(2, participacao.getPessoa().getId());
            pstmt.setString(3, participacao.getTipo().name());
            pstmt.setInt(4, participacao.getIntimado() ? 1 : 0);
            pstmt.setString(5, participacao.getObservacoes());
            pstmt.setLong(6, participacao.getId());

            pstmt.executeUpdate();
        }
    }

    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM participacao_audiencia WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    public void deletarPorAudiencia(Long audienciaId) throws SQLException {
        String sql = "DELETE FROM participacao_audiencia WHERE audiencia_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, audienciaId);
            pstmt.executeUpdate();
        }
    }

    private ParticipacaoAudiencia mapResultSetToParticipacao(ResultSet rs) throws SQLException {
        ParticipacaoAudiencia participacao = new ParticipacaoAudiencia();
        participacao.setId(rs.getLong("id"));

        // Criar objetos stub (apenas com ID) - serão preenchidos pelo Service se necessário
        Audiencia audiencia = new Audiencia();
        audiencia.setId(rs.getLong("audiencia_id"));
        participacao.setAudiencia(audiencia);

        Pessoa pessoa = new Pessoa();
        pessoa.setId(rs.getLong("pessoa_id"));
        participacao.setPessoa(pessoa);

        participacao.setTipo(TipoParticipacao.valueOf(rs.getString("tipo")));
        participacao.setIntimado(rs.getInt("intimado") == 1);
        participacao.setObservacoes(rs.getString("observacoes"));

        return participacao;
    }
}
