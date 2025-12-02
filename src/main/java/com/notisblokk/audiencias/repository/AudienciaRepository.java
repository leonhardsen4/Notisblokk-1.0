package com.notisblokk.audiencias.repository;

import com.notisblokk.audiencias.model.*;
import com.notisblokk.audiencias.model.enums.*;
import com.notisblokk.audiencias.util.DateUtil;
import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositório para acesso a dados de Audiências Judiciais.
 *
 * <p><b>IMPORTANTE:</b> Usa formato brasileiro dd/MM/yyyy para datas.</p>
 */
public class AudienciaRepository {

    private static final Logger logger = LoggerFactory.getLogger(AudienciaRepository.class);

    // Repositórios auxiliares para carregar entidades relacionadas
    private final VaraRepository varaRepository = new VaraRepository();
    private final JuizRepository juizRepository = new JuizRepository();
    private final PromotorRepository promotorRepository = new PromotorRepository();

    /**
     * Busca todas as audiências ordenadas por data e horário.
     */
    public List<Audiencia> buscarTodas() throws SQLException {
        String sql = "SELECT * FROM audiencia ORDER BY data_audiencia, horario_inicio";
        List<Audiencia> audiencias = new ArrayList<>();

        System.out.println("DEBUG_AUDIENCIAS: AudienciaRepository.buscarTodas() - Iniciando busca");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                try {
                    Audiencia aud = mapResultSetToAudiencia(rs);
                    audiencias.add(aud);
                    System.out.println("DEBUG_AUDIENCIAS: Audiência mapeada com sucesso - ID: " + aud.getId());
                } catch (Exception e) {
                    System.err.println("DEBUG_AUDIENCIAS: ERRO ao mapear audiência - ID: " + rs.getLong("id"));
                    e.printStackTrace();
                    // Continuar processando as outras audiências
                }
            }

            logger.debug("Encontradas {} audiências", audiencias.size());
        }

        return audiencias;
    }

    /**
     * Busca audiência por ID.
     */
    public Optional<Audiencia> buscarPorId(Long id) throws SQLException {
        String sql = "SELECT * FROM audiencia WHERE id = ?";

        System.out.println("DEBUG_AUDIENCIAS: AudienciaRepository.buscarPorId() - ID: " + id);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAudiencia(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Busca audiências por data.
     */
    public List<Audiencia> buscarPorData(LocalDate data) throws SQLException {
        String sql = "SELECT * FROM audiencia WHERE data_audiencia = ? ORDER BY horario_inicio";
        List<Audiencia> audiencias = new ArrayList<>();

        String dataFormatada = DateUtil.formatDate(data);
        System.out.println("DEBUG_AUDIENCIAS: AudienciaRepository.buscarPorData() - Data: " + dataFormatada);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dataFormatada);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    audiencias.add(mapResultSetToAudiencia(rs));
                }
            }
        }

        return audiencias;
    }

    /**
     * Busca audiências por vara.
     */
    public List<Audiencia> buscarPorVara(Long varaId) throws SQLException {
        String sql = "SELECT * FROM audiencia WHERE vara_id = ? ORDER BY data_audiencia, horario_inicio";
        List<Audiencia> audiencias = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, varaId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    audiencias.add(mapResultSetToAudiencia(rs));
                }
            }
        }

        return audiencias;
    }

    /**
     * Busca todas as audiências dentro de um período de datas.
     * Usado para cálculo de horários livres.
     *
     * @param dataInicio Data inicial do período (inclusive)
     * @param dataFim Data final do período (inclusive)
     * @return Lista de audiências no período ordenadas por data e horário
     */
    public List<Audiencia> buscarTodasPorPeriodo(LocalDate dataInicio, LocalDate dataFim) throws SQLException {
        String sql = "SELECT * FROM audiencia WHERE data_audiencia >= ? AND data_audiencia <= ? " +
                     "ORDER BY data_audiencia, horario_inicio";
        List<Audiencia> audiencias = new ArrayList<>();

        String dataInicioFormatada = DateUtil.formatDate(dataInicio);
        String dataFimFormatada = DateUtil.formatDate(dataFim);

        logger.debug("DEBUG_AUDIENCIAS: AudienciaRepository.buscarTodasPorPeriodo() - Período: {} a {}",
                     dataInicioFormatada, dataFimFormatada);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dataInicioFormatada);
            pstmt.setString(2, dataFimFormatada);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    audiencias.add(mapResultSetToAudiencia(rs));
                }
            }

            logger.debug("DEBUG_AUDIENCIAS: Encontradas {} audiências no período", audiencias.size());
        }

        return audiencias;
    }

    /**
     * Verifica conflitos de horário para uma audiência.
     *
     * @return Lista de audiências conflitantes
     */
    public List<Map<String, Object>> verificarConflitosHorario(
            LocalDate data, LocalTime horarioInicio, Integer duracao,
            Long varaId, Long audienciaIdExcluir) throws SQLException {

        String dataFormatada = DateUtil.formatDate(data);
        LocalTime horarioFim = horarioInicio.plusMinutes(duracao);
        String horarioInicioStr = DateUtil.formatTime(horarioInicio);
        String horarioFimStr = DateUtil.formatTime(horarioFim);

        System.out.println("DEBUG_AUDIENCIAS: AudienciaRepository.verificarConflitosHorario() - " +
            "Data: " + dataFormatada +
            ", Horário: " + horarioInicioStr + " - " + horarioFimStr +
            ", Vara: " + varaId);

        String sql = """
            SELECT a.*, v.nome as vara_nome, j.nome as juiz_nome
            FROM audiencia a
            LEFT JOIN vara v ON a.vara_id = v.id
            LEFT JOIN juiz j ON a.juiz_id = j.id
            WHERE a.data_audiencia = ?
            AND a.vara_id = ?
            AND a.id != COALESCE(?, 0)
            AND a.horario_inicio < ?
            AND a.horario_fim > ?
            """;

        List<Map<String, Object>> conflitos = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dataFormatada);
            pstmt.setLong(2, varaId);
            pstmt.setObject(3, audienciaIdExcluir);
            pstmt.setString(4, horarioFimStr);
            pstmt.setString(5, horarioInicioStr);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> conflito = new HashMap<>();
                    conflito.put("id", rs.getLong("id"));
                    conflito.put("numeroProcesso", rs.getString("numero_processo"));
                    conflito.put("horarioInicio", rs.getString("horario_inicio"));
                    conflito.put("horarioFim", rs.getString("horario_fim"));
                    conflito.put("varaNome", rs.getString("vara_nome"));
                    conflito.put("juizNome", rs.getString("juiz_nome"));
                    conflitos.add(conflito);

                    System.err.println("DEBUG_AUDIENCIAS: CONFLITO DETECTADO - Processo: " +
                        rs.getString("numero_processo") + ", Horário: " +
                        rs.getString("horario_inicio") + " - " + rs.getString("horario_fim"));
                }
            }
        }

        if (conflitos.isEmpty()) {
            System.out.println("DEBUG_AUDIENCIAS: Nenhum conflito encontrado");
        } else {
            System.out.println("DEBUG_AUDIENCIAS: " + conflitos.size() + " conflito(s) encontrado(s)");
        }

        return conflitos;
    }

    /**
     * Salva uma nova audiência.
     */
    public Long salvar(Audiencia audiencia) throws SQLException {
        String sql = """
            INSERT INTO audiencia (
                numero_processo, vara_id, data_audiencia, horario_inicio, duracao,
                horario_fim, dia_semana, tipo_audiencia, formato, competencia, status,
                artigo, observacoes, reu_preso, agendamento_teams, reconhecimento,
                depoimento_especial, juiz_id, promotor_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        System.out.println("DEBUG_AUDIENCIAS: AudienciaRepository.salvar() - Processo: " +
            audiencia.getNumeroProcesso() + ", Data: " + DateUtil.formatDate(audiencia.getDataAudiencia()));

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setAudienciaParameters(pstmt, audiencia);

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    logger.info("Audiência criada - ID: {}, Processo: {}", id, audiencia.getNumeroProcesso());
                    System.out.println("DEBUG_AUDIENCIAS: Audiência salva com ID: " + id);
                    return id;
                }
                throw new SQLException("Falha ao obter ID");
            }
        }
    }

    /**
     * Atualiza uma audiência existente.
     */
    public void atualizar(Audiencia audiencia) throws SQLException {
        String sql = """
            UPDATE audiencia SET
                numero_processo = ?, vara_id = ?, data_audiencia = ?, horario_inicio = ?, duracao = ?,
                horario_fim = ?, dia_semana = ?, tipo_audiencia = ?, formato = ?, competencia = ?, status = ?,
                artigo = ?, observacoes = ?, reu_preso = ?, agendamento_teams = ?, reconhecimento = ?,
                depoimento_especial = ?, juiz_id = ?, promotor_id = ?
            WHERE id = ?
            """;

        System.out.println("DEBUG_AUDIENCIAS: AudienciaRepository.atualizar() - ID: " + audiencia.getId());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setAudienciaParameters(pstmt, audiencia);
            pstmt.setLong(20, audiencia.getId());

            pstmt.executeUpdate();
            logger.info("Audiência atualizada - ID: {}", audiencia.getId());
        }
    }

    /**
     * Deleta uma audiência.
     */
    public void deletar(Long id) throws SQLException {
        String sql = "DELETE FROM audiencia WHERE id = ?";

        System.out.println("DEBUG_AUDIENCIAS: AudienciaRepository.deletar() - ID: " + id);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.info("Audiência deletada - ID: {}", id);
        }
    }

    /**
     * Define os parâmetros do PreparedStatement para uma audiência.
     */
    private void setAudienciaParameters(PreparedStatement pstmt, Audiencia aud) throws SQLException {
        pstmt.setString(1, aud.getNumeroProcesso());
        pstmt.setLong(2, aud.getVara().getId());
        pstmt.setString(3, DateUtil.formatDate(aud.getDataAudiencia()));
        pstmt.setString(4, DateUtil.formatTime(aud.getHorarioInicio()));
        pstmt.setInt(5, aud.getDuracao());
        pstmt.setString(6, DateUtil.formatTime(aud.getHorarioFim()));
        pstmt.setString(7, aud.getDiaSemana());
        pstmt.setString(8, aud.getTipoAudiencia().name());
        pstmt.setString(9, aud.getFormato().name());
        pstmt.setString(10, aud.getCompetencia().name());
        pstmt.setString(11, aud.getStatus().name());
        pstmt.setString(12, aud.getArtigo());
        pstmt.setString(13, aud.getObservacoes());
        pstmt.setInt(14, aud.getReuPreso() ? 1 : 0);
        pstmt.setInt(15, aud.getAgendamentoTeams() ? 1 : 0);
        pstmt.setInt(16, aud.getReconhecimento() ? 1 : 0);
        pstmt.setInt(17, aud.getDepoimentoEspecial() ? 1 : 0);
        pstmt.setObject(18, aud.getJuiz() != null ? aud.getJuiz().getId() : null);
        pstmt.setObject(19, aud.getPromotor() != null ? aud.getPromotor().getId() : null);
    }

    /**
     * Mapeia ResultSet para Audiencia.
     */
    private Audiencia mapResultSetToAudiencia(ResultSet rs) throws SQLException {
        Audiencia aud = new Audiencia();
        aud.setId(rs.getLong("id"));
        aud.setNumeroProcesso(rs.getString("numero_processo"));

        // Carregar vara (obrigatória)
        Long varaId = rs.getLong("vara_id");
        aud.setVara(varaRepository.buscarPorId(varaId).orElse(null));

        // Converter datas do formato dd/MM/yyyy
        aud.setDataAudiencia(DateUtil.parseDate(rs.getString("data_audiencia")));
        aud.setHorarioInicio(DateUtil.parseTime(rs.getString("horario_inicio")));
        aud.setDuracao(rs.getInt("duracao"));

        String horarioFim = rs.getString("horario_fim");
        if (horarioFim != null) {
            aud.setHorarioFim(DateUtil.parseTime(horarioFim));
        }

        aud.setDiaSemana(rs.getString("dia_semana"));
        aud.setTipoAudiencia(TipoAudiencia.valueOf(rs.getString("tipo_audiencia")));
        aud.setFormato(FormatoAudiencia.valueOf(rs.getString("formato")));
        aud.setCompetencia(Competencia.valueOf(rs.getString("competencia")));
        aud.setStatus(StatusAudiencia.valueOf(rs.getString("status")));
        aud.setArtigo(rs.getString("artigo"));
        aud.setObservacoes(rs.getString("observacoes"));
        aud.setReuPreso(rs.getInt("reu_preso") == 1);
        aud.setAgendamentoTeams(rs.getInt("agendamento_teams") == 1);
        aud.setReconhecimento(rs.getInt("reconhecimento") == 1);
        aud.setDepoimentoEspecial(rs.getInt("depoimento_especial") == 1);

        // Carregar juiz (opcional)
        Object juizIdObj = rs.getObject("juiz_id");
        if (juizIdObj != null) {
            Long juizId = ((Number) juizIdObj).longValue();
            aud.setJuiz(juizRepository.buscarPorId(juizId).orElse(null));
        }

        // Carregar promotor (opcional)
        Object promotorIdObj = rs.getObject("promotor_id");
        if (promotorIdObj != null) {
            Long promotorId = ((Number) promotorIdObj).longValue();
            aud.setPromotor(promotorRepository.buscarPorId(promotorId).orElse(null));
        }

        return aud;
    }
}
