package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.Juiz;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Juízes.
 *
 * <p>Implementa operações CRUD para a entidade Juiz.</p>
 */
public class JuizRepository {

    private static final Logger logger = LoggerFactory.getLogger(JuizRepository.class);

    public List<Juiz> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM juiz ORDER BY nome ASC";
        List<Juiz> juizes = new ArrayList<>();

        System.out.println("DEBUG_AUDIENCIAS: JuizRepository.buscarTodos() - Iniciando busca");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                juizes.add(mapResultSetToJuiz(rs));
            }

            logger.debug("Encontrados {} juízes", juizes.size());
        }

        return juizes;
    }

    public Optional<Juiz> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM juiz WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToJuiz(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<Juiz> buscarPorNome(String nome) throws SQLException {
        String sql = "SELECT * FROM juiz WHERE LOWER(nome) LIKE LOWER(?) ORDER BY nome ASC";
        List<Juiz> juizes = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + nome + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    juizes.add(mapResultSetToJuiz(rs));
                }
            }
        }

        return juizes;
    }

    public Long salvar(Juiz juiz) throws SQLException {
        String sql = "INSERT INTO juiz (nome, telefone, email, observacoes) VALUES (?, ?, ?, ?)";

        System.out.println("DEBUG_AUDIENCIAS: JuizRepository.salvar() - Juiz: " + juiz.getNome());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, juiz.getNome());
            pstmt.setString(2, juiz.getTelefone());
            pstmt.setString(3, juiz.getEmail());
            pstmt.setString(4, juiz.getObservacoes());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    logger.info("Juiz criado com sucesso - ID: {}", id);
                    return id;
                } else {
                    throw new SQLException("Falha ao obter ID do juiz criado");
                }
            }
        }
    }

    public void atualizar(Juiz juiz) throws SQLException {
        String sql = "UPDATE juiz SET nome = ?, telefone = ?, email = ?, observacoes = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, juiz.getNome());
            pstmt.setString(2, juiz.getTelefone());
            pstmt.setString(3, juiz.getEmail());
            pstmt.setString(4, juiz.getObservacoes());
            pstmt.setLong(5, juiz.getId());

            pstmt.executeUpdate();
            logger.info("Juiz atualizado - ID: {}", juiz.getId());
        }
    }

    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM juiz WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.info("Juiz deletado - ID: {}", id);
        }
    }

    private Juiz mapResultSetToJuiz(ResultSet rs) throws SQLException {
        Juiz juiz = new Juiz();
        juiz.setId(rs.getLong("id"));
        juiz.setNome(rs.getString("nome"));
        juiz.setTelefone(rs.getString("telefone"));
        juiz.setEmail(rs.getString("email"));
        juiz.setObservacoes(rs.getString("observacoes"));
        return juiz;
    }
}
