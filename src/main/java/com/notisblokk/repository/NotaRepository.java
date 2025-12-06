package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.Nota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório responsável pelo acesso a dados de notas.
 *
 * <p>Implementa operações CRUD (Create, Read, Update, Delete) para a entidade Nota,
 * utilizando PreparedStatements para prevenir SQL injection.</p>
 *
 * <p><b>IMPORTANTE:</b> Ajusta timestamps do SQLite (UTC) para UTC-3 (horário de Brasília).</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class NotaRepository {

    private static final Logger logger = LoggerFactory.getLogger(NotaRepository.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Busca todas as notas do sistema, ordenadas por prazo final (mais urgentes primeiro).
     *
     * @return List<Nota> lista de todas as notas (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Nota> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM notas ORDER BY prazo_final ASC, data_criacao DESC";
        List<Nota> notas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                notas.add(mapResultSetToNota(rs));
            }

            logger.debug("Encontradas {} notas", notas.size());
        }

        return notas;
    }

    /**
     * Busca uma nota por ID.
     *
     * @param id ID da nota
     * @return Optional<Nota> nota encontrada ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<Nota> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM notas WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Nota nota = mapResultSetToNota(rs);
                    logger.debug("Nota encontrada: {}", nota.getTitulo());
                    return Optional.of(nota);
                }
            }
        }

        logger.debug("Nota com ID {} não encontrada", id);
        return Optional.empty();
    }

    /**
     * Busca notas por etiqueta.
     *
     * @param etiquetaId ID da etiqueta
     * @return List<Nota> lista de notas com a etiqueta especificada
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Nota> buscarPorEtiqueta(Long etiquetaId) throws SQLException {
        String sql = "SELECT * FROM notas WHERE etiqueta_id = ? ORDER BY prazo_final ASC";
        List<Nota> notas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, etiquetaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notas.add(mapResultSetToNota(rs));
                }
            }

            logger.debug("Encontradas {} notas com etiqueta_id {}", notas.size(), etiquetaId);
        }

        return notas;
    }

    /**
     * Busca notas por status.
     *
     * @param statusId ID do status
     * @return List<Nota> lista de notas com o status especificado
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Nota> buscarPorStatus(Long statusId) throws SQLException {
        String sql = "SELECT * FROM notas WHERE status_id = ? ORDER BY prazo_final ASC";
        List<Nota> notas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, statusId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notas.add(mapResultSetToNota(rs));
                }
            }

            logger.debug("Encontradas {} notas com status_id {}", notas.size(), statusId);
        }

        return notas;
    }

    /**
     * Busca notas por texto no título ou conteúdo (case-insensitive).
     *
     * <p>Este método busca o termo fornecido tanto no título quanto no conteúdo das notas.
     * A busca é case-insensitive usando LOWER() e retorna notas com relacionamentos completos.</p>
     *
     * @param termo termo de busca (será convertido para lowercase)
     * @return List<NotaDTO> lista de notas que contêm o termo (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<com.notisblokk.model.NotaDTO> buscarPorTexto(String termo) throws SQLException {
        if (termo == null || termo.trim().isEmpty()) {
            logger.debug("Termo de busca vazio, retornando lista vazia");
            return new ArrayList<>();
        }

        String sql = """
            SELECT
                n.id,
                n.titulo,
                n.conteudo,
                n.prazo_final,
                n.data_criacao,
                n.data_atualizacao,
                n.sessao_id,
                n.usuario_id,
                e.id as etiqueta_id,
                e.nome as etiqueta_nome,
                s.id as status_id,
                s.nome as status_nome,
                s.cor_hex as status_cor
            FROM notas n
            LEFT JOIN etiquetas e ON n.etiqueta_id = e.id
            LEFT JOIN status_nota s ON n.status_id = s.id
            WHERE LOWER(n.titulo) LIKE LOWER(?) OR LOWER(n.conteudo) LIKE LOWER(?)
            ORDER BY n.prazo_final ASC, n.data_criacao DESC
        """;

        List<com.notisblokk.model.NotaDTO> notasDTO = new ArrayList<>();
        String termoBusca = "%" + termo.trim() + "%";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, termoBusca);
            pstmt.setString(2, termoBusca);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notasDTO.add(mapResultSetToNotaDTOCompleto(rs));
                }
            }

            logger.debug("Encontradas {} notas para o termo de busca '{}'", notasDTO.size(), termo);
        }

        return notasDTO;
    }

    /**
     * Busca notas por intervalo de prazo final.
     *
     * <p>Retorna notas cujo prazo final está entre as datas especificadas (inclusive).
     * Retorna DTOs completos com relacionamentos (etiquetas e status).</p>
     *
     * @param dataInicio data inicial do intervalo (inclusive)
     * @param dataFim data final do intervalo (inclusive)
     * @return List<NotaDTO> lista de notas no intervalo especificado
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<com.notisblokk.model.NotaDTO> buscarPorIntervaloPrazo(LocalDate dataInicio, LocalDate dataFim)
            throws SQLException {

        if (dataInicio == null || dataFim == null) {
            logger.debug("Data início ou fim é nula, retornando lista vazia");
            return new ArrayList<>();
        }

        String sql = """
            SELECT
                n.id,
                n.titulo,
                n.conteudo,
                n.prazo_final,
                n.data_criacao,
                n.data_atualizacao,
                n.sessao_id,
                n.usuario_id,
                e.id as etiqueta_id,
                e.nome as etiqueta_nome,
                s.id as status_id,
                s.nome as status_nome,
                s.cor_hex as status_cor
            FROM notas n
            LEFT JOIN etiquetas e ON n.etiqueta_id = e.id
            LEFT JOIN status_nota s ON n.status_id = s.id
            WHERE n.prazo_final >= ? AND n.prazo_final <= ?
            ORDER BY n.prazo_final ASC, n.data_criacao DESC
        """;

        List<com.notisblokk.model.NotaDTO> notasDTO = new ArrayList<>();
        String dataInicioStr = dataInicio.format(DATE_FORMATTER);
        String dataFimStr = dataFim.format(DATE_FORMATTER);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dataInicioStr);
            pstmt.setString(2, dataFimStr);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notasDTO.add(mapResultSetToNotaDTOCompleto(rs));
                }
            }

            logger.debug("Encontradas {} notas entre {} e {}", notasDTO.size(), dataInicioStr, dataFimStr);
        }

        return notasDTO;
    }

    /**
     * Busca todas as notas com seus relacionamentos (etiquetas e status) em uma única query.
     *
     * <p>Este método otimiza a performance ao usar LEFT JOIN para evitar o problema de N+1 queries.
     * Retorna diretamente objetos NotaDTO com etiquetas e status já populados.</p>
     *
     * @return List<NotaDTO> lista de todas as notas com relacionamentos (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<com.notisblokk.model.NotaDTO> buscarTodasComRelacionamentos() throws SQLException {
        String sql = """
            SELECT
                n.id,
                n.titulo,
                n.conteudo,
                n.prazo_final,
                n.data_criacao,
                n.data_atualizacao,
                n.sessao_id,
                n.usuario_id,
                e.id as etiqueta_id,
                e.nome as etiqueta_nome,
                s.id as status_id,
                s.nome as status_nome,
                s.cor_hex as status_cor
            FROM notas n
            LEFT JOIN etiquetas e ON n.etiqueta_id = e.id
            LEFT JOIN status_nota s ON n.status_id = s.id
            ORDER BY n.prazo_final ASC, n.data_criacao DESC
        """;

        List<com.notisblokk.model.NotaDTO> notasDTO = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                notasDTO.add(mapResultSetToNotaDTOCompleto(rs));
            }

            logger.debug("Encontradas {} notas com relacionamentos (query otimizada)", notasDTO.size());
        }

        return notasDTO;
    }

    /**
     * Busca notas por usuário (para sistema de alertas).
     *
     * @param usuarioId ID do usuário
     * @return List<NotaDTO> lista de notas do usuário com informações completas
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<com.notisblokk.model.NotaDTO> buscarPorUsuarioId(Long usuarioId) throws SQLException {
        String sql = """
            SELECT
                n.*,
                e.nome as etiqueta_nome,
                s.nome as status_nome,
                s.cor_hex as status_cor
            FROM notas n
            INNER JOIN etiquetas e ON n.etiqueta_id = e.id
            INNER JOIN status_nota s ON n.status_id = s.id
            WHERE n.usuario_id = ?
            ORDER BY n.prazo_final ASC
        """;
        List<com.notisblokk.model.NotaDTO> notasDTO = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notasDTO.add(mapResultSetToNotaDTO(rs));
                }
            }

            logger.debug("Encontradas {} notas para usuário ID {}", notasDTO.size(), usuarioId);
        }

        return notasDTO;
    }

    /**
     * Mapeia ResultSet para NotaDTO com informações completas de etiqueta e status.
     * Usado pelo método buscarTodasComRelacionamentos() que traz todos os campos.
     *
     * @param rs ResultSet com dados completos da query com JOINs
     * @return NotaDTO objeto DTO completo com etiqueta e status
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private com.notisblokk.model.NotaDTO mapResultSetToNotaDTOCompleto(ResultSet rs) throws SQLException {
        com.notisblokk.model.NotaDTO dto = new com.notisblokk.model.NotaDTO();

        // Dados da nota
        dto.setId(rs.getLong("id"));
        dto.setTitulo(rs.getString("titulo"));
        dto.setConteudo(rs.getString("conteudo"));
        dto.setSessaoId(rs.getLong("sessao_id"));
        dto.setUsuarioId(rs.getLong("usuario_id"));

        // Parse prazo_final (tentar formato brasileiro primeiro, depois ISO)
        String prazoFinalStr = rs.getString("prazo_final");
        if (prazoFinalStr != null && !prazoFinalStr.isEmpty()) {
            try {
                dto.setPrazoFinal(LocalDate.parse(prazoFinalStr, DATE_FORMATTER));
            } catch (Exception e) {
                try {
                    dto.setPrazoFinal(LocalDate.parse(prazoFinalStr));
                } catch (Exception ex) {
                    logger.error("Erro ao parsear prazo_final: {}", prazoFinalStr);
                }
            }
        }

        // Parse datas de criação e atualização
        String dataCriacaoStr = rs.getString("data_criacao");
        if (dataCriacaoStr != null && !dataCriacaoStr.isEmpty()) {
            try {
                dto.setDataCriacao(LocalDateTime.parse(dataCriacaoStr, FORMATTER));
            } catch (Exception e) {
                try {
                    dto.setDataCriacao(LocalDateTime.parse(dataCriacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Erro ao parsear data_criacao: {}", dataCriacaoStr);
                }
            }
        }

        String dataAtualizacaoStr = rs.getString("data_atualizacao");
        if (dataAtualizacaoStr != null && !dataAtualizacaoStr.isEmpty()) {
            try {
                dto.setDataAtualizacao(LocalDateTime.parse(dataAtualizacaoStr, FORMATTER));
            } catch (Exception e) {
                try {
                    dto.setDataAtualizacao(LocalDateTime.parse(dataAtualizacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Erro ao parsear data_atualizacao: {}", dataAtualizacaoStr);
                }
            }
        }

        // Etiqueta (pode ser NULL se LEFT JOIN não encontrou)
        Long etiquetaId = rs.getLong("etiqueta_id");
        if (!rs.wasNull()) {
            com.notisblokk.model.Etiqueta etiqueta = new com.notisblokk.model.Etiqueta();
            etiqueta.setId(etiquetaId);
            etiqueta.setNome(rs.getString("etiqueta_nome"));
            dto.setEtiqueta(etiqueta);
        }

        // Status (pode ser NULL se LEFT JOIN não encontrou)
        Long statusId = rs.getLong("status_id");
        if (!rs.wasNull()) {
            com.notisblokk.model.StatusNota status = new com.notisblokk.model.StatusNota();
            status.setId(statusId);
            status.setNome(rs.getString("status_nome"));
            status.setCorHex(rs.getString("status_cor"));
            dto.setStatus(status);
        }

        return dto;
    }

    /**
     * Mapeia ResultSet para NotaDTO com informações completas de etiqueta e status.
     */
    private com.notisblokk.model.NotaDTO mapResultSetToNotaDTO(ResultSet rs) throws SQLException {
        com.notisblokk.model.NotaDTO dto = new com.notisblokk.model.NotaDTO();

        // Dados da nota
        dto.setId(rs.getLong("id"));
        dto.setTitulo(rs.getString("titulo"));
        dto.setConteudo(rs.getString("conteudo"));

        // Parse prazo_final
        String prazoFinalStr = rs.getString("prazo_final");
        if (prazoFinalStr != null && !prazoFinalStr.isEmpty()) {
            try {
                dto.setPrazoFinal(LocalDate.parse(prazoFinalStr, DATE_FORMATTER));
            } catch (Exception e) {
                try {
                    dto.setPrazoFinal(LocalDate.parse(prazoFinalStr));
                } catch (Exception ex) {
                    logger.error("Erro ao parsear prazo_final: {}", prazoFinalStr);
                }
            }
        }

        // Parse datas de criação e atualização
        String dataCriacaoStr = rs.getString("data_criacao");
        if (dataCriacaoStr != null && !dataCriacaoStr.isEmpty()) {
            try {
                dto.setDataCriacao(LocalDateTime.parse(dataCriacaoStr, FORMATTER));
            } catch (Exception e) {
                try {
                    dto.setDataCriacao(LocalDateTime.parse(dataCriacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Erro ao parsear data_criacao: {}", dataCriacaoStr);
                }
            }
        }

        // Etiqueta
        com.notisblokk.model.Etiqueta etiqueta = new com.notisblokk.model.Etiqueta();
        etiqueta.setId(rs.getLong("etiqueta_id"));
        etiqueta.setNome(rs.getString("etiqueta_nome"));
        dto.setEtiqueta(etiqueta);

        // Status
        com.notisblokk.model.StatusNota status = new com.notisblokk.model.StatusNota();
        status.setId(rs.getLong("status_id"));
        status.setNome(rs.getString("status_nome"));
        status.setCorHex(rs.getString("status_cor"));
        dto.setStatus(status);

        return dto;
    }

    /**
     * Salva uma nova nota no banco de dados.
     *
     * @param nota nota a ser salva (sem ID)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return Nota nota salva com ID gerado
     * @throws SQLException se houver erro ao salvar
     */
    public Nota salvar(Nota nota, Long sessaoId, Long usuarioId) throws SQLException {
        String sql = """
            INSERT INTO notas (etiqueta_id, status_id, titulo, conteudo, data_criacao, data_atualizacao, prazo_final, sessao_id, usuario_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);
            String prazoFinal = nota.getPrazoFinal().format(DATE_FORMATTER);

            pstmt.setLong(1, nota.getEtiquetaId());
            pstmt.setLong(2, nota.getStatusId());
            pstmt.setString(3, nota.getTitulo());
            pstmt.setString(4, nota.getConteudo());
            pstmt.setString(5, timestamp);
            pstmt.setString(6, timestamp);
            pstmt.setString(7, prazoFinal);
            pstmt.setLong(8, sessaoId);
            pstmt.setLong(9, usuarioId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar nota, nenhuma linha afetada");
            }

            // Obter ID gerado
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {

                if (rs.next()) {
                    nota.setId(rs.getLong(1));
                    nota.setDataCriacao(now);
                    nota.setDataAtualizacao(now);
                    nota.setSessaoId(sessaoId);
                    nota.setUsuarioId(usuarioId);
                } else {
                    throw new SQLException("Falha ao obter ID da nota criada");
                }
            }

            logger.info("Nota salva com sucesso: {} (ID: {})", nota.getTitulo(), nota.getId());
            return nota;
        }
    }

    /**
     * Atualiza uma nota existente no banco de dados.
     *
     * @param nota nota a ser atualizada (com ID)
     * @throws SQLException se houver erro ao atualizar
     */
    public void atualizar(Nota nota) throws SQLException {
        String sql = """
            UPDATE notas
            SET etiqueta_id = ?, status_id = ?, titulo = ?, conteudo = ?, data_atualizacao = ?, prazo_final = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);
            String prazoFinal = nota.getPrazoFinal().format(DATE_FORMATTER);

            pstmt.setLong(1, nota.getEtiquetaId());
            pstmt.setLong(2, nota.getStatusId());
            pstmt.setString(3, nota.getTitulo());
            pstmt.setString(4, nota.getConteudo());
            pstmt.setString(5, timestamp);
            pstmt.setString(6, prazoFinal);
            pstmt.setLong(7, nota.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Nota com ID " + nota.getId() + " não encontrada");
            }

            nota.setDataAtualizacao(now);

            logger.info("Nota atualizada com sucesso: {} (ID: {})", nota.getTitulo(), nota.getId());
        }
    }

    /**
     * Remove uma nota do banco de dados.
     *
     * @param id ID da nota a ser removida
     * @throws SQLException se houver erro ao deletar
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM notas WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Nota com ID " + id + " não encontrada");
            }

            logger.info("Nota com ID {} removida com sucesso", id);
        }
    }

    /**
     * Conta o total de notas no sistema.
     *
     * @return long total de notas
     * @throws SQLException se houver erro ao contar
     */
    public long contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM notas";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    /**
     * Busca notas com paginação.
     *
     * @param pagina número da página (começa em 1)
     * @param tamanhoPagina quantidade de registros por página
     * @param ordenarPor campo para ordenação (prazo_final, data_criacao, titulo)
     * @param direcao direção da ordenação (ASC ou DESC)
     * @return List<Nota> lista de notas paginada
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Nota> buscarComPaginacao(int pagina, int tamanhoPagina, String ordenarPor, String direcao)
            throws SQLException {

        // Validar e sanitizar ordenação
        String ordenacao = switch (ordenarPor) {
            case "titulo" -> "titulo";
            case "data_criacao" -> "data_criacao";
            case "data_atualizacao" -> "data_atualizacao";
            default -> "prazo_final";
        };

        String direcaoOrdem = "DESC".equalsIgnoreCase(direcao) ? "DESC" : "ASC";

        // Calcular offset
        int offset = (pagina - 1) * tamanhoPagina;

        String sql = String.format(
            "SELECT * FROM notas ORDER BY %s %s LIMIT ? OFFSET ?",
            ordenacao, direcaoOrdem
        );

        List<Nota> notas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tamanhoPagina);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notas.add(mapResultSetToNota(rs));
                }
            }

            logger.debug("Encontradas {} notas (página {}, tamanho {})",
                notas.size(), pagina, tamanhoPagina);
        }

        return notas;
    }

    /**
     * Mapeia um ResultSet para um objeto Nota.
     * IMPORTANTE: Ajusta timestamps do SQLite para o horário de Brasília.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return Nota objeto mapeado
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private Nota mapResultSetToNota(ResultSet rs) throws SQLException {
        Nota nota = new Nota();
        nota.setId(rs.getLong("id"));
        nota.setEtiquetaId(rs.getLong("etiqueta_id"));
        nota.setStatusId(rs.getLong("status_id"));
        nota.setTitulo(rs.getString("titulo"));
        nota.setConteudo(rs.getString("conteudo"));
        nota.setSessaoId(rs.getLong("sessao_id"));
        nota.setUsuarioId(rs.getLong("usuario_id"));

        // Parse data_criacao usando formato brasileiro
        String dataCriacaoStr = rs.getString("data_criacao");
        if (dataCriacaoStr != null && !dataCriacaoStr.isEmpty()) {
            try {
                nota.setDataCriacao(LocalDateTime.parse(dataCriacaoStr, FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", dataCriacaoStr);
                try {
                    nota.setDataCriacao(LocalDateTime.parse(dataCriacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse da data_criacao: {}", dataCriacaoStr);
                }
            }
        }

        // Parse data_atualizacao usando formato brasileiro
        String dataAtualizacaoStr = rs.getString("data_atualizacao");
        if (dataAtualizacaoStr != null && !dataAtualizacaoStr.isEmpty()) {
            try {
                nota.setDataAtualizacao(LocalDateTime.parse(dataAtualizacaoStr, FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", dataAtualizacaoStr);
                try {
                    nota.setDataAtualizacao(LocalDateTime.parse(dataAtualizacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse da data_atualizacao: {}", dataAtualizacaoStr);
                }
            }
        }

        // Parse prazo_final (tentar formato brasileiro primeiro, depois ISO)
        String prazoFinalStr = rs.getString("prazo_final");
        if (prazoFinalStr != null && !prazoFinalStr.isEmpty()) {
            try {
                // Tentar formato brasileiro dd/MM/yyyy primeiro
                nota.setPrazoFinal(LocalDate.parse(prazoFinalStr, DATE_FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", prazoFinalStr);
                try {
                    // Fallback: tentar formato ISO yyyy-MM-dd
                    nota.setPrazoFinal(LocalDate.parse(prazoFinalStr));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse do prazo_final: {}", prazoFinalStr);
                }
            }
        }

        return nota;
    }
}
