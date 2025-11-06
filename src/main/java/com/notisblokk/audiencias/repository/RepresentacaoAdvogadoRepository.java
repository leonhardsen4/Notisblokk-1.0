package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.*;
import com.notisblokk.audiencias.model.enums.TipoRepresentacao;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Representação de Advogados.
 */
public class RepresentacaoAdvogadoRepository {

    private static final Logger logger = LoggerFactory.getLogger(RepresentacaoAdvogadoRepository.class);

    public List<RepresentacaoAdvogado> buscarPorAudiencia(Long audienciaId) throws SQLException {
        String sql = "SELECT * FROM representacao_advogado WHERE audiencia_id = ?";
        List<RepresentacaoAdvogado> representacoes = new ArrayList<>();

        System.out.println("DEBUG_AUDIENCIAS: RepresentacaoAdvogadoRepository.buscarPorAudiencia() - ID: " + audienciaId);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, audienciaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    representacoes.add(mapResultSetToRepresentacao(rs));
                }
            }
        }
        return representacoes;
    }

    public List<RepresentacaoAdvogado> buscarPorAdvogado(Long advogadoId) throws SQLException {
        String sql = "SELECT * FROM representacao_advogado WHERE advogado_id = ?";
        List<RepresentacaoAdvogado> representacoes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, advogadoId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    representacoes.add(mapResultSetToRepresentacao(rs));
                }
            }
        }
        return representacoes;
    }

    public Optional<RepresentacaoAdvogado> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM representacao_advogado WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRepresentacao(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Long salvar(RepresentacaoAdvogado representacao) throws SQLException {
        String sql = """
            INSERT INTO representacao_advogado (audiencia_id, advogado_id, cliente_id, tipo)
            VALUES (?, ?, ?, ?)
            """;

        System.out.println("DEBUG_AUDIENCIAS: RepresentacaoAdvogadoRepository.salvar() - " +
            "Audiência: " + representacao.getAudiencia().getId() +
            ", Advogado: " + representacao.getAdvogado().getId() +
            ", Cliente: " + representacao.getCliente().getId());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, representacao.getAudiencia().getId());
            pstmt.setLong(2, representacao.getAdvogado().getId());
            pstmt.setLong(3, representacao.getCliente().getId());
            pstmt.setString(4, representacao.getTipo().name());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new SQLException("Falha ao obter ID");
            }
        }
    }

    public void atualizar(RepresentacaoAdvogado representacao) throws SQLException {
        String sql = """
            UPDATE representacao_advogado
            SET audiencia_id = ?, advogado_id = ?, cliente_id = ?, tipo = ?
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, representacao.getAudiencia().getId());
            pstmt.setLong(2, representacao.getAdvogado().getId());
            pstmt.setLong(3, representacao.getCliente().getId());
            pstmt.setString(4, representacao.getTipo().name());
            pstmt.setLong(5, representacao.getId());

            pstmt.executeUpdate();
        }
    }

    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM representacao_advogado WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    public void deletarPorAudiencia(Long audienciaId) throws SQLException {
        String sql = "DELETE FROM representacao_advogado WHERE audiencia_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, audienciaId);
            pstmt.executeUpdate();
        }
    }

    private RepresentacaoAdvogado mapResultSetToRepresentacao(ResultSet rs) throws SQLException {
        RepresentacaoAdvogado representacao = new RepresentacaoAdvogado();
        representacao.setId(rs.getLong("id"));

        // Criar objetos stub (apenas com ID) - serão preenchidos pelo Service se necessário
        Audiencia audiencia = new Audiencia();
        audiencia.setId(rs.getLong("audiencia_id"));
        representacao.setAudiencia(audiencia);

        Advogado advogado = new Advogado();
        advogado.setId(rs.getLong("advogado_id"));
        representacao.setAdvogado(advogado);

        Pessoa cliente = new Pessoa();
        cliente.setId(rs.getLong("cliente_id"));
        representacao.setCliente(cliente);

        representacao.setTipo(TipoRepresentacao.valueOf(rs.getString("tipo")));

        return representacao;
    }
}
