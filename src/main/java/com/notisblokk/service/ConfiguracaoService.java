package com.notisblokk.service;

import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço de gerenciamento de configurações do usuário.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Buscar configurações do usuário</li>
 *   <li>Salvar configurações personalizadas</li>
 *   <li>Resetar para configurações padrão</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class ConfiguracaoService {

    private static final Logger logger = LoggerFactory.getLogger(ConfiguracaoService.class);

    // Configurações padrão
    private static final Map<String, String> DEFAULTS = Map.ofEntries(
        Map.entry("notif_email", "true"),
        Map.entry("notif_toast", "true"),
        Map.entry("notif_dias_critico", "0"),
        Map.entry("notif_dias_urgente", "3"),
        Map.entry("notif_dias_atencao", "5"),
        Map.entry("senha_expira_meses", "3"),
        Map.entry("senha_aviso_antecedencia", "10"),
        Map.entry("backup_auto", "false"),
        Map.entry("backup_periodicidade", "7")
    );

    /**
     * Busca todas as configurações de um usuário.
     * Se não houver configurações, retorna valores padrão.
     *
     * @param userId ID do usuário
     * @return Map com chave-valor das configurações
     */
    public Map<String, String> buscarConfiguracoes(Long userId) {
        Map<String, String> configuracoes = new HashMap<>(DEFAULTS);

        String sql = "SELECT chave, valor FROM configuracoes WHERE usuario_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String chave = rs.getString("chave");
                    String valor = rs.getString("valor");
                    configuracoes.put(chave, valor);
                }
            }

            logger.debug("Configurações carregadas para userId: {}", userId);

        } catch (SQLException e) {
            logger.error("Erro ao buscar configurações para userId: {}", userId, e);
        }

        return configuracoes;
    }

    /**
     * Busca uma configuração específica.
     *
     * @param userId ID do usuário
     * @param chave Chave da configuração
     * @return Valor da configuração ou valor padrão se não encontrado
     */
    public String buscarConfiguracao(Long userId, String chave) {
        String sql = "SELECT valor FROM configuracoes WHERE usuario_id = ? AND chave = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, chave);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("valor");
                }
            }

        } catch (SQLException e) {
            logger.error("Erro ao buscar configuração {} para userId: {}", chave, userId, e);
        }

        // Retornar padrão se não encontrado
        return DEFAULTS.getOrDefault(chave, null);
    }

    /**
     * Salva ou atualiza múltiplas configurações.
     *
     * @param userId ID do usuário
     * @param configuracoes Map de configurações chave-valor
     */
    public void salvarConfiguracoes(Long userId, Map<String, String> configuracoes) {
        String upsertSql = """
            INSERT INTO configuracoes (usuario_id, chave, valor, data_criacao, data_atualizacao)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(usuario_id, chave) DO UPDATE SET
                valor = excluded.valor,
                data_atualizacao = CURRENT_TIMESTAMP
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {

            for (Map.Entry<String, String> entry : configuracoes.entrySet()) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, entry.getKey());
                pstmt.setString(3, entry.getValue());
                pstmt.addBatch();
            }

            pstmt.executeBatch();

            logger.info("Configurações salvas com sucesso para userId: {}", userId);

        } catch (SQLException e) {
            logger.error("Erro ao salvar configurações para userId: {}", userId, e);
            throw new RuntimeException("Erro ao salvar configurações", e);
        }
    }

    /**
     * Salva ou atualiza uma configuração específica.
     *
     * @param userId ID do usuário
     * @param chave Chave da configuração
     * @param valor Valor da configuração
     */
    public void salvarConfiguracao(Long userId, String chave, String valor) {
        Map<String, String> config = new HashMap<>();
        config.put(chave, valor);
        salvarConfiguracoes(userId, config);
    }

    /**
     * Reseta todas as configurações do usuário para os valores padrão.
     *
     * @param userId ID do usuário
     */
    public void resetarConfiguracoes(Long userId) {
        String deleteSql = "DELETE FROM configuracoes WHERE usuario_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {

            pstmt.setLong(1, userId);
            pstmt.executeUpdate();

            logger.info("Configurações resetadas para userId: {}", userId);

        } catch (SQLException e) {
            logger.error("Erro ao resetar configurações para userId: {}", userId, e);
            throw new RuntimeException("Erro ao resetar configurações", e);
        }
    }

    /**
     * Retorna os valores padrão das configurações.
     *
     * @return Map com configurações padrão
     */
    public Map<String, String> obterPadroes() {
        return new HashMap<>(DEFAULTS);
    }

    /**
     * Remove configurações obsoletas do banco de dados.
     *
     * @param userId ID do usuário
     */
    public void limparConfiguracoesObsoletas(Long userId) {
        // Configurações obsoletas que devem ser removidas
        String[] chavesObsoletas = {"tema", "paginacao_padrao", "idioma_interface"};

        String deleteSql = "DELETE FROM configuracoes WHERE usuario_id = ? AND chave = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {

            for (String chave : chavesObsoletas) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, chave);
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            int removidos = 0;
            for (int result : results) {
                if (result > 0) removidos++;
            }

            if (removidos > 0) {
                logger.info("Removidas {} configurações obsoletas para userId: {}", removidos, userId);
            }

        } catch (SQLException e) {
            logger.error("Erro ao limpar configurações obsoletas para userId: {}", userId, e);
        }
    }
}
