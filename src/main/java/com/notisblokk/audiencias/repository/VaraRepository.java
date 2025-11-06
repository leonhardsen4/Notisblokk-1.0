package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.Vara;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Varas Judiciais.
 *
 * <p>Implementa operações CRUD (Create, Read, Update, Delete) para a entidade Vara.</p>
 */
public class VaraRepository {

    private static final Logger logger = LoggerFactory.getLogger(VaraRepository.class);

    /**
     * Busca todas as varas ordenadas por nome.
     *
     * @return List<Vara> lista de varas (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Vara> buscarTodas() throws SQLException {
        String sql = "SELECT * FROM vara ORDER BY nome ASC";
        List<Vara> varas = new ArrayList<>();

        // DEBUG_AUDIENCIAS: Log de busca
        System.out.println("DEBUG_AUDIENCIAS: VaraRepository.buscarTodas() - Iniciando busca");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                varas.add(mapResultSetToVara(rs));
            }

            logger.debug("Encontradas {} varas", varas.size());
            System.out.println("DEBUG_AUDIENCIAS: VaraRepository.buscarTodas() - " + varas.size() + " varas encontradas");
        }

        return varas;
    }

    /**
     * Busca uma vara por ID.
     *
     * @param id ID da vara
     * @return Optional<Vara> vara encontrada ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<Vara> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM vara WHERE id = ?";

        System.out.println("DEBUG_AUDIENCIAS: VaraRepository.buscarPorId() - ID: " + id);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Vara vara = mapResultSetToVara(rs);
                    logger.debug("Vara encontrada: {}", vara.getNome());
                    return Optional.of(vara);
                }
            }
        }

        logger.debug("Vara com ID {} não encontrada", id);
        return Optional.empty();
    }

    /**
     * Busca varas por nome (busca parcial, case-insensitive).
     *
     * @param nome nome ou parte do nome
     * @return List<Vara> lista de varas encontradas
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Vara> buscarPorNome(String nome) throws SQLException {
        String sql = "SELECT * FROM vara WHERE LOWER(nome) LIKE LOWER(?) ORDER BY nome ASC";
        List<Vara> varas = new ArrayList<>();

        System.out.println("DEBUG_AUDIENCIAS: VaraRepository.buscarPorNome() - Nome: " + nome);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + nome + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    varas.add(mapResultSetToVara(rs));
                }
            }

            logger.debug("Encontradas {} varas com nome contendo '{}'", varas.size(), nome);
        }

        return varas;
    }

    /**
     * Salva uma nova vara no banco de dados.
     *
     * @param vara vara a ser salva
     * @return Long ID da vara criada
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Long salvar(Vara vara) throws SQLException {
        String sql = """
            INSERT INTO vara (nome, comarca, endereco, telefone, email, observacoes)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        System.out.println("DEBUG_AUDIENCIAS: VaraRepository.salvar() - Vara: " + vara.getNome());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, vara.getNome());
            pstmt.setString(2, vara.getComarca());
            pstmt.setString(3, vara.getEndereco());
            pstmt.setString(4, vara.getTelefone());
            pstmt.setString(5, vara.getEmail());
            pstmt.setString(6, vara.getObservacoes());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar vara, nenhuma linha afetada");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    logger.info("Vara criada com sucesso - ID: {}, Nome: {}", id, vara.getNome());
                    System.out.println("DEBUG_AUDIENCIAS: VaraRepository.salvar() - Vara criada com ID: " + id);
                    return id;
                } else {
                    throw new SQLException("Falha ao obter ID da vara criada");
                }
            }
        }
    }

    /**
     * Atualiza uma vara existente.
     *
     * @param vara vara com dados atualizados
     * @throws SQLException se houver erro ao acessar o banco
     */
    public void atualizar(Vara vara) throws SQLException {
        String sql = """
            UPDATE vara
            SET nome = ?, comarca = ?, endereco = ?, telefone = ?, email = ?, observacoes = ?
            WHERE id = ?
            """;

        System.out.println("DEBUG_AUDIENCIAS: VaraRepository.atualizar() - ID: " + vara.getId());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vara.getNome());
            pstmt.setString(2, vara.getComarca());
            pstmt.setString(3, vara.getEndereco());
            pstmt.setString(4, vara.getTelefone());
            pstmt.setString(5, vara.getEmail());
            pstmt.setString(6, vara.getObservacoes());
            pstmt.setLong(7, vara.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                logger.warn("Nenhuma vara foi atualizada com ID: {}", vara.getId());
            } else {
                logger.info("Vara atualizada com sucesso - ID: {}", vara.getId());
            }
        }
    }

    /**
     * Deleta uma vara por ID.
     *
     * @param id ID da vara a deletar
     * @throws SQLException se houver erro ao acessar o banco
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM vara WHERE id = ?";

        System.out.println("DEBUG_AUDIENCIAS: VaraRepository.deletar() - ID: " + id);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                logger.warn("Nenhuma vara foi deletada com ID: {}", id);
            } else {
                logger.info("Vara deletada com sucesso - ID: {}", id);
                System.out.println("DEBUG_AUDIENCIAS: VaraRepository.deletar() - Vara deletada");
            }
        }
    }

    /**
     * Mapeia ResultSet para objeto Vara.
     *
     * @param rs ResultSet posicionado na linha da vara
     * @return Vara objeto mapeado
     * @throws SQLException se houver erro ao ler dados
     */
    private Vara mapResultSetToVara(ResultSet rs) throws SQLException {
        Vara vara = new Vara();
        vara.setId(rs.getLong("id"));
        vara.setNome(rs.getString("nome"));
        vara.setComarca(rs.getString("comarca"));
        vara.setEndereco(rs.getString("endereco"));
        vara.setTelefone(rs.getString("telefone"));
        vara.setEmail(rs.getString("email"));
        vara.setObservacoes(rs.getString("observacoes"));
        return vara;
    }
}
