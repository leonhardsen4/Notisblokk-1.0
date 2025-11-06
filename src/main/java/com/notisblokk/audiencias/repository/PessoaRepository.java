package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.Pessoa;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Pessoas (partes processuais).
 */
public class PessoaRepository {

    private static final Logger logger = LoggerFactory.getLogger(PessoaRepository.class);

    public List<Pessoa> buscarTodas() throws SQLException {
        String sql = "SELECT * FROM pessoa ORDER BY nome ASC";
        List<Pessoa> pessoas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                pessoas.add(mapResultSetToPessoa(rs));
            }
        }
        return pessoas;
    }

    public Optional<Pessoa> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM pessoa WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPessoa(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Pessoa> buscarPorNome(String nome) throws SQLException {
        String sql = "SELECT * FROM pessoa WHERE LOWER(nome) LIKE LOWER(?) ORDER BY nome ASC";
        List<Pessoa> pessoas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + nome + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pessoas.add(mapResultSetToPessoa(rs));
                }
            }
        }
        return pessoas;
    }

    public List<Pessoa> buscarPorCPF(String cpf) throws SQLException {
        String sql = "SELECT * FROM pessoa WHERE cpf = ?";
        List<Pessoa> pessoas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cpf);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pessoas.add(mapResultSetToPessoa(rs));
                }
            }
        }
        return pessoas;
    }

    public Long salvar(Pessoa pessoa) throws SQLException {
        String sql = "INSERT INTO pessoa (nome, cpf, telefone, email, observacoes) VALUES (?, ?, ?, ?, ?)";

        System.out.println("DEBUG_AUDIENCIAS: PessoaRepository.salvar() - " + pessoa.getNome());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, pessoa.getNome());
            pstmt.setString(2, pessoa.getCpf());
            pstmt.setString(3, pessoa.getTelefone());
            pstmt.setString(4, pessoa.getEmail());
            pstmt.setString(5, pessoa.getObservacoes());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new SQLException("Falha ao obter ID");
            }
        }
    }

    public void atualizar(Pessoa pessoa) throws SQLException {
        String sql = "UPDATE pessoa SET nome = ?, cpf = ?, telefone = ?, email = ?, observacoes = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, pessoa.getNome());
            pstmt.setString(2, pessoa.getCpf());
            pstmt.setString(3, pessoa.getTelefone());
            pstmt.setString(4, pessoa.getEmail());
            pstmt.setString(5, pessoa.getObservacoes());
            pstmt.setLong(6, pessoa.getId());

            pstmt.executeUpdate();
        }
    }

    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM pessoa WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    private Pessoa mapResultSetToPessoa(ResultSet rs) throws SQLException {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(rs.getLong("id"));
        pessoa.setNome(rs.getString("nome"));
        pessoa.setCpf(rs.getString("cpf"));
        pessoa.setTelefone(rs.getString("telefone"));
        pessoa.setEmail(rs.getString("email"));
        pessoa.setObservacoes(rs.getString("observacoes"));
        return pessoa;
    }
}
