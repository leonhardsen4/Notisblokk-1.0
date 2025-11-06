package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.Promotor;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Promotores.
 */
public class PromotorRepository {

    private static final Logger logger = LoggerFactory.getLogger(PromotorRepository.class);

    public List<Promotor> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM promotor ORDER BY nome ASC";
        List<Promotor> promotores = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                promotores.add(mapResultSetToPromotor(rs));
            }
        }
        return promotores;
    }

    public Optional<Promotor> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM promotor WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPromotor(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Promotor> buscarPorNome(String nome) throws SQLException {
        String sql = "SELECT * FROM promotor WHERE LOWER(nome) LIKE LOWER(?) ORDER BY nome ASC";
        List<Promotor> promotores = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + nome + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    promotores.add(mapResultSetToPromotor(rs));
                }
            }
        }
        return promotores;
    }

    public Long salvar(Promotor promotor) throws SQLException {
        String sql = "INSERT INTO promotor (nome, telefone, email, observacoes) VALUES (?, ?, ?, ?)";

        System.out.println("DEBUG_AUDIENCIAS: PromotorRepository.salvar() - " + promotor.getNome());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, promotor.getNome());
            pstmt.setString(2, promotor.getTelefone());
            pstmt.setString(3, promotor.getEmail());
            pstmt.setString(4, promotor.getObservacoes());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new SQLException("Falha ao obter ID");
            }
        }
    }

    public void atualizar(Promotor promotor) throws SQLException {
        String sql = "UPDATE promotor SET nome = ?, telefone = ?, email = ?, observacoes = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, promotor.getNome());
            pstmt.setString(2, promotor.getTelefone());
            pstmt.setString(3, promotor.getEmail());
            pstmt.setString(4, promotor.getObservacoes());
            pstmt.setLong(5, promotor.getId());

            pstmt.executeUpdate();
        }
    }

    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM promotor WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    private Promotor mapResultSetToPromotor(ResultSet rs) throws SQLException {
        Promotor promotor = new Promotor();
        promotor.setId(rs.getLong("id"));
        promotor.setNome(rs.getString("nome"));
        promotor.setTelefone(rs.getString("telefone"));
        promotor.setEmail(rs.getString("email"));
        promotor.setObservacoes(rs.getString("observacoes"));
        return promotor;
    }
}
