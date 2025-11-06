package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.Advogado;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Advogados.
 */
public class AdvogadoRepository {

    private static final Logger logger = LoggerFactory.getLogger(AdvogadoRepository.class);

    public List<Advogado> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM advogado ORDER BY nome ASC";
        List<Advogado> advogados = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                advogados.add(mapResultSetToAdvogado(rs));
            }
        }
        return advogados;
    }

    public Optional<Advogado> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM advogado WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAdvogado(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Advogado> buscarPorNome(String nome) throws SQLException {
        String sql = "SELECT * FROM advogado WHERE LOWER(nome) LIKE LOWER(?) ORDER BY nome ASC";
        List<Advogado> advogados = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + nome + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    advogados.add(mapResultSetToAdvogado(rs));
                }
            }
        }
        return advogados;
    }

    public List<Advogado> buscarPorOAB(String oab) throws SQLException {
        String sql = "SELECT * FROM advogado WHERE LOWER(oab) LIKE LOWER(?) ORDER BY nome ASC";
        List<Advogado> advogados = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + oab + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    advogados.add(mapResultSetToAdvogado(rs));
                }
            }
        }
        return advogados;
    }

    public Long salvar(Advogado advogado) throws SQLException {
        String sql = "INSERT INTO advogado (nome, oab, telefone, email, observacoes) VALUES (?, ?, ?, ?, ?)";

        System.out.println("DEBUG_AUDIENCIAS: AdvogadoRepository.salvar() - " + advogado.getNome() + " (OAB: " + advogado.getOab() + ")");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, advogado.getNome());
            pstmt.setString(2, advogado.getOab());
            pstmt.setString(3, advogado.getTelefone());
            pstmt.setString(4, advogado.getEmail());
            pstmt.setString(5, advogado.getObservacoes());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new SQLException("Falha ao obter ID");
            }
        }
    }

    public void atualizar(Advogado advogado) throws SQLException {
        String sql = "UPDATE advogado SET nome = ?, oab = ?, telefone = ?, email = ?, observacoes = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, advogado.getNome());
            pstmt.setString(2, advogado.getOab());
            pstmt.setString(3, advogado.getTelefone());
            pstmt.setString(4, advogado.getEmail());
            pstmt.setString(5, advogado.getObservacoes());
            pstmt.setLong(6, advogado.getId());

            pstmt.executeUpdate();
        }
    }

    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM advogado WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    private Advogado mapResultSetToAdvogado(ResultSet rs) throws SQLException {
        Advogado advogado = new Advogado();
        advogado.setId(rs.getLong("id"));
        advogado.setNome(rs.getString("nome"));
        advogado.setOab(rs.getString("oab"));
        advogado.setTelefone(rs.getString("telefone"));
        advogado.setEmail(rs.getString("email"));
        advogado.setObservacoes(rs.getString("observacoes"));
        return advogado;
    }
}
