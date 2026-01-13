package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.Tarefa;
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
 * Repositório responsável pelo acesso a dados de tarefas.
 *
 * <p>Implementa operações CRUD (Create, Read, Update, Delete) para a entidade Tarefa,
 * utilizando PreparedStatements para prevenir SQL injection.</p>
 *
 * <p><b>IMPORTANTE:</b> Ajusta timestamps do SQLite (UTC) para UTC-3 (horário de Brasília).</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class TarefaRepository {

    private static final Logger logger = LoggerFactory.getLogger(TarefaRepository.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Busca todas as tarefas do sistema, ordenadas por prazo final (mais urgentes primeiro).
     *
     * @return List<Tarefa> lista de todas as tarefas (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Tarefa> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM tarefas ORDER BY prazo_final ASC, data_criacao DESC";
        List<Tarefa> tarefas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tarefas.add(mapResultSetToTarefa(rs));
            }

            logger.debug("Encontradas {} tarefas", tarefas.size());
        }

        return tarefas;
    }

    /**
     * Busca uma tarefa por ID.
     *
     * @param id ID da tarefa
     * @return Optional<Tarefa> tarefa encontrada ou Optional.empty()
     * @throws SQLException se houver erro ao acessar o banco
     */
    public Optional<Tarefa> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM tarefas WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Tarefa tarefa = mapResultSetToTarefa(rs);
                    logger.debug("Tarefa encontrada: {}", tarefa.getTitulo());
                    return Optional.of(tarefa);
                }
            }
        }

        logger.debug("Tarefa com ID {} não encontrada", id);
        return Optional.empty();
    }

    /**
     * Busca tarefas por etiqueta.
     *
     * @param etiquetaId ID da etiqueta
     * @return List<Tarefa> lista de tarefas com a etiqueta especificada
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Tarefa> buscarPorEtiqueta(Long etiquetaId) throws SQLException {
        String sql = "SELECT * FROM tarefas WHERE etiqueta_id = ? ORDER BY prazo_final ASC";
        List<Tarefa> tarefas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, etiquetaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tarefas.add(mapResultSetToTarefa(rs));
                }
            }

            logger.debug("Encontradas {} tarefas com etiqueta_id {}", tarefas.size(), etiquetaId);
        }

        return tarefas;
    }

    /**
     * Busca tarefas por status.
     *
     * @param statusId ID do status
     * @return List<Tarefa> lista de tarefas com o status especificado
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Tarefa> buscarPorStatus(Long statusId) throws SQLException {
        String sql = "SELECT * FROM tarefas WHERE status_id = ? ORDER BY prazo_final ASC";
        List<Tarefa> tarefas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, statusId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tarefas.add(mapResultSetToTarefa(rs));
                }
            }

            logger.debug("Encontradas {} tarefas com status_id {}", tarefas.size(), statusId);
        }

        return tarefas;
    }

    /**
     * Busca tarefas por texto no título ou conteúdo (case-insensitive).
     *
     * <p>Este método busca o termo fornecido tanto no título quanto no conteúdo das tarefas.
     * A busca é case-insensitive usando LOWER() e retorna tarefas com relacionamentos completos.</p>
     *
     * @param termo termo de busca (será convertido para lowercase)
     * @return List<TarefaDTO> lista de tarefas que contêm o termo (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<com.notisblokk.model.TarefaDTO> buscarPorTexto(String termo) throws SQLException {
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
            FROM tarefas n
            LEFT JOIN etiquetas e ON n.etiqueta_id = e.id
            LEFT JOIN status_tarefa s ON n.status_id = s.id
            WHERE LOWER(n.titulo) LIKE LOWER(?) OR LOWER(n.conteudo) LIKE LOWER(?)
            ORDER BY n.prazo_final ASC, n.data_criacao DESC
        """;

        List<com.notisblokk.model.TarefaDTO> tarefasDTO = new ArrayList<>();
        String termoBusca = "%" + termo.trim() + "%";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, termoBusca);
            pstmt.setString(2, termoBusca);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tarefasDTO.add(mapResultSetToTarefaDTOCompleto(rs));
                }
            }

            logger.debug("Encontradas {} tarefas para o termo de busca '{}'", tarefasDTO.size(), termo);
        }

        return tarefasDTO;
    }

    /**
     * Busca tarefas por intervalo de prazo final.
     *
     * <p>Retorna tarefas cujo prazo final está entre as datas especificadas (inclusive).
     * Retorna DTOs completos com relacionamentos (etiquetas e status).</p>
     *
     * @param dataInicio data inicial do intervalo (inclusive)
     * @param dataFim data final do intervalo (inclusive)
     * @return List<TarefaDTO> lista de tarefas no intervalo especificado
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<com.notisblokk.model.TarefaDTO> buscarPorIntervaloPrazo(LocalDate dataInicio, LocalDate dataFim)
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
            FROM tarefas n
            LEFT JOIN etiquetas e ON n.etiqueta_id = e.id
            LEFT JOIN status_tarefa s ON n.status_id = s.id
            WHERE n.prazo_final >= ? AND n.prazo_final <= ?
            ORDER BY n.prazo_final ASC, n.data_criacao DESC
        """;

        List<com.notisblokk.model.TarefaDTO> tarefasDTO = new ArrayList<>();
        String dataInicioStr = dataInicio.format(DATE_FORMATTER);
        String dataFimStr = dataFim.format(DATE_FORMATTER);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dataInicioStr);
            pstmt.setString(2, dataFimStr);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tarefasDTO.add(mapResultSetToTarefaDTOCompleto(rs));
                }
            }

            logger.debug("Encontradas {} tarefas entre {} e {}", tarefasDTO.size(), dataInicioStr, dataFimStr);
        }

        return tarefasDTO;
    }

    /**
     * Busca todas as tarefas com seus relacionamentos (etiquetas e status) em uma única query.
     *
     * <p>Este método otimiza a performance ao usar LEFT JOIN para evitar o problema de N+1 queries.
     * Retorna diretamente objetos TarefaDTO com etiquetas e status já populados.</p>
     *
     * @return List<TarefaDTO> lista de todas as tarefas com relacionamentos (vazia se não houver)
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<com.notisblokk.model.TarefaDTO> buscarTodasComRelacionamentos() throws SQLException {
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
            FROM tarefas n
            LEFT JOIN etiquetas e ON n.etiqueta_id = e.id
            LEFT JOIN status_tarefa s ON n.status_id = s.id
            ORDER BY n.prazo_final ASC, n.data_criacao DESC
        """;

        List<com.notisblokk.model.TarefaDTO> tarefasDTO = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tarefasDTO.add(mapResultSetToTarefaDTOCompleto(rs));
            }

            logger.debug("Encontradas {} tarefas com relacionamentos (query otimizada)", tarefasDTO.size());
        }

        return tarefasDTO;
    }

    /**
     * Busca tarefas por usuário (para sistema de alertas).
     *
     * @param usuarioId ID do usuário
     * @return List<TarefaDTO> lista de tarefas do usuário com informações completas
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<com.notisblokk.model.TarefaDTO> buscarPorUsuarioId(Long usuarioId) throws SQLException {
        String sql = """
            SELECT
                n.*,
                e.nome as etiqueta_nome,
                s.nome as status_nome,
                s.cor_hex as status_cor
            FROM tarefas n
            INNER JOIN etiquetas e ON n.etiqueta_id = e.id
            INNER JOIN status_tarefa s ON n.status_id = s.id
            WHERE n.usuario_id = ?
            ORDER BY n.prazo_final ASC
        """;
        List<com.notisblokk.model.TarefaDTO> tarefasDTO = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tarefasDTO.add(mapResultSetToTarefaDTO(rs));
                }
            }

            logger.debug("Encontradas {} tarefas para usuário ID {}", tarefasDTO.size(), usuarioId);
        }

        return tarefasDTO;
    }

    /**
     * Mapeia ResultSet para TarefaDTO com informações completas de etiqueta e status.
     * Usado pelo método buscarTodasComRelacionamentos() que traz todos os campos.
     *
     * @param rs ResultSet com dados completos da query com JOINs
     * @return TarefaDTO objeto DTO completo com etiqueta e status
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private com.notisblokk.model.TarefaDTO mapResultSetToTarefaDTOCompleto(ResultSet rs) throws SQLException {
        com.notisblokk.model.TarefaDTO dto = new com.notisblokk.model.TarefaDTO();

        // Dados da tarefa
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
            com.notisblokk.model.StatusTarefa status = new com.notisblokk.model.StatusTarefa();
            status.setId(statusId);
            status.setNome(rs.getString("status_nome"));
            status.setCorHex(rs.getString("status_cor"));
            dto.setStatus(status);
        }

        return dto;
    }

    /**
     * Mapeia ResultSet para TarefaDTO com informações completas de etiqueta e status.
     */
    private com.notisblokk.model.TarefaDTO mapResultSetToTarefaDTO(ResultSet rs) throws SQLException {
        com.notisblokk.model.TarefaDTO dto = new com.notisblokk.model.TarefaDTO();

        // Dados da tarefa
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
        com.notisblokk.model.StatusTarefa status = new com.notisblokk.model.StatusTarefa();
        status.setId(rs.getLong("status_id"));
        status.setNome(rs.getString("status_nome"));
        status.setCorHex(rs.getString("status_cor"));
        dto.setStatus(status);

        return dto;
    }

    /**
     * Salva uma nova tarefa no banco de dados.
     *
     * @param tarefa tarefa a ser salva (sem ID)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return Tarefa tarefa salva com ID gerado
     * @throws SQLException se houver erro ao salvar
     */
    public Tarefa salvar(Tarefa tarefa, Long sessaoId, Long usuarioId) throws SQLException {
        String sql = """
            INSERT INTO tarefas (etiqueta_id, status_id, titulo, conteudo, data_criacao, data_atualizacao, prazo_final, sessao_id, usuario_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);
            String prazoFinal = tarefa.getPrazoFinal().format(DATE_FORMATTER);

            pstmt.setLong(1, tarefa.getEtiquetaId());
            pstmt.setLong(2, tarefa.getStatusId());
            pstmt.setString(3, tarefa.getTitulo());
            pstmt.setString(4, tarefa.getConteudo());
            pstmt.setString(5, timestamp);
            pstmt.setString(6, timestamp);
            pstmt.setString(7, prazoFinal);
            pstmt.setLong(8, sessaoId);
            pstmt.setLong(9, usuarioId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Falha ao salvar tarefa, nenhuma linha afetada");
            }

            // Obter ID gerado
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {

                if (rs.next()) {
                    tarefa.setId(rs.getLong(1));
                    tarefa.setDataCriacao(now);
                    tarefa.setDataAtualizacao(now);
                    tarefa.setSessaoId(sessaoId);
                    tarefa.setUsuarioId(usuarioId);
                } else {
                    throw new SQLException("Falha ao obter ID da tarefa criada");
                }
            }

            logger.info("Tarefa salva com sucesso: {} (ID: {})", tarefa.getTitulo(), tarefa.getId());
            return tarefa;
        }
    }

    /**
     * Atualiza uma tarefa existente no banco de dados.
     *
     * @param tarefa tarefa a ser atualizada (com ID)
     * @throws SQLException se houver erro ao atualizar
     */
    public void atualizar(Tarefa tarefa) throws SQLException {
        String sql = """
            UPDATE tarefas
            SET etiqueta_id = ?, status_id = ?, titulo = ?, conteudo = ?, data_atualizacao = ?, prazo_final = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
            String timestamp = now.format(FORMATTER);
            String prazoFinal = tarefa.getPrazoFinal().format(DATE_FORMATTER);

            pstmt.setLong(1, tarefa.getEtiquetaId());
            pstmt.setLong(2, tarefa.getStatusId());
            pstmt.setString(3, tarefa.getTitulo());
            pstmt.setString(4, tarefa.getConteudo());
            pstmt.setString(5, timestamp);
            pstmt.setString(6, prazoFinal);
            pstmt.setLong(7, tarefa.getId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Tarefa com ID " + tarefa.getId() + " não encontrada");
            }

            tarefa.setDataAtualizacao(now);

            logger.info("Tarefa atualizada com sucesso: {} (ID: {})", tarefa.getTitulo(), tarefa.getId());
        }
    }

    /**
     * Remove uma tarefa do banco de dados.
     *
     * @param id ID da tarefa a ser removida
     * @throws SQLException se houver erro ao deletar
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM tarefas WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Tarefa com ID " + id + " não encontrada");
            }

            logger.info("Tarefa com ID {} removida com sucesso", id);
        }
    }

    /**
     * Conta o total de tarefas no sistema.
     *
     * @return long total de tarefas
     * @throws SQLException se houver erro ao contar
     */
    public long contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM tarefas";

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
     * Busca tarefas com paginação.
     *
     * @param pagina número da página (começa em 1)
     * @param tamanhoPagina quantidade de registros por página
     * @param ordenarPor campo para ordenação (prazo_final, data_criacao, titulo)
     * @param direcao direção da ordenação (ASC ou DESC)
     * @return List<Tarefa> lista de tarefas paginada
     * @throws SQLException se houver erro ao acessar o banco
     */
    public List<Tarefa> buscarComPaginacao(int pagina, int tamanhoPagina, String ordenarPor, String direcao)
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
            "SELECT * FROM tarefas ORDER BY %s %s LIMIT ? OFFSET ?",
            ordenacao, direcaoOrdem
        );

        List<Tarefa> tarefas = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tamanhoPagina);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tarefas.add(mapResultSetToTarefa(rs));
                }
            }

            logger.debug("Encontradas {} tarefas (página {}, tamanho {})",
                tarefas.size(), pagina, tamanhoPagina);
        }

        return tarefas;
    }

    /**
     * Mapeia um ResultSet para um objeto Tarefa.
     * IMPORTANTE: Ajusta timestamps do SQLite para o horário de Brasília.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return Tarefa objeto mapeado
     * @throws SQLException se houver erro ao ler o ResultSet
     */
    private Tarefa mapResultSetToTarefa(ResultSet rs) throws SQLException {
        Tarefa tarefa = new Tarefa();
        tarefa.setId(rs.getLong("id"));
        tarefa.setEtiquetaId(rs.getLong("etiqueta_id"));
        tarefa.setStatusId(rs.getLong("status_id"));
        tarefa.setTitulo(rs.getString("titulo"));
        tarefa.setConteudo(rs.getString("conteudo"));
        tarefa.setSessaoId(rs.getLong("sessao_id"));
        tarefa.setUsuarioId(rs.getLong("usuario_id"));

        // Parse data_criacao usando formato brasileiro
        String dataCriacaoStr = rs.getString("data_criacao");
        if (dataCriacaoStr != null && !dataCriacaoStr.isEmpty()) {
            try {
                tarefa.setDataCriacao(LocalDateTime.parse(dataCriacaoStr, FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", dataCriacaoStr);
                try {
                    tarefa.setDataCriacao(LocalDateTime.parse(dataCriacaoStr.replace(" ", "T")));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse da data_criacao: {}", dataCriacaoStr);
                }
            }
        }

        // Parse data_atualizacao usando formato brasileiro
        String dataAtualizacaoStr = rs.getString("data_atualizacao");
        if (dataAtualizacaoStr != null && !dataAtualizacaoStr.isEmpty()) {
            try {
                tarefa.setDataAtualizacao(LocalDateTime.parse(dataAtualizacaoStr, FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", dataAtualizacaoStr);
                try {
                    tarefa.setDataAtualizacao(LocalDateTime.parse(dataAtualizacaoStr.replace(" ", "T")));
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
                tarefa.setPrazoFinal(LocalDate.parse(prazoFinalStr, DATE_FORMATTER));
            } catch (Exception e) {
                logger.warn("Erro ao fazer parse da data '{}', tentando formato ISO", prazoFinalStr);
                try {
                    // Fallback: tentar formato ISO yyyy-MM-dd
                    tarefa.setPrazoFinal(LocalDate.parse(prazoFinalStr));
                } catch (Exception ex) {
                    logger.error("Não foi possível fazer parse do prazo_final: {}", prazoFinalStr);
                }
            }
        }

        return tarefa;
    }
}
