package com.notisblokk.repository;

import com.notisblokk.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Repositório para controlar alertas de email já enviados.
 *
 * <p>Previne envio duplicado de alertas para a mesma nota/usuário no mesmo dia.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-27
 */
public class AlertaEnviadoRepository {

    private static final Logger logger = LoggerFactory.getLogger(AlertaEnviadoRepository.class);

    /**
     * Verifica se um alerta já foi enviado hoje para uma nota específica de um usuário.
     *
     * @param userId ID do usuário
     * @param notaId ID da nota
     * @param nivel Nível do alerta (CRÍTICO, URGENTE, etc.)
     * @return true se já foi enviado hoje, false caso contrário
     */
    public boolean alertaJaEnviado(Long userId, Long notaId, String nivel) {
        String sql = """
            SELECT COUNT(*) FROM alertas_enviados
            WHERE usuario_id = ? AND nota_id = ? AND nivel = ? AND DATE(data_envio) = DATE('now', 'localtime')
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, notaId);
            pstmt.setString(3, nivel);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            logger.error("Erro ao verificar se alerta foi enviado - userId: {}, notaId: {}, nivel: {}",
                        userId, notaId, nivel, e);
        }

        return false;
    }

    /**
     * Registra que um alerta foi enviado.
     *
     * @param userId ID do usuário
     * @param notaId ID da nota
     * @param nivel Nível do alerta
     * @param diasRestantes Quantos dias faltam para o prazo
     */
    public void registrarEnvio(Long userId, Long notaId, String nivel, int diasRestantes) {
        String sql = """
            INSERT INTO alertas_enviados (usuario_id, nota_id, nivel, dias_restantes, data_envio)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, notaId);
            pstmt.setString(3, nivel);
            pstmt.setInt(4, diasRestantes);

            pstmt.executeUpdate();

            logger.debug("Alerta registrado - userId: {}, notaId: {}, nivel: {}", userId, notaId, nivel);

        } catch (SQLException e) {
            logger.error("Erro ao registrar envio de alerta - userId: {}, notaId: {}, nivel: {}",
                        userId, notaId, nivel, e);
        }
    }

    /**
     * Remove registros antigos de alertas enviados (mais de 30 dias).
     * Executado periodicamente para evitar crescimento excessivo da tabela.
     */
    public void limparAlertasAntigos() {
        String sql = "DELETE FROM alertas_enviados WHERE DATE(data_envio) < DATE('now', '-30 days')";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int deletados = pstmt.executeUpdate();

            if (deletados > 0) {
                logger.info("Limpeza de alertas antigos: {} registros removidos", deletados);
            }

        } catch (SQLException e) {
            logger.error("Erro ao limpar alertas antigos", e);
        }
    }

    /**
     * Conta quantos alertas foram enviados hoje.
     *
     * @return número de alertas enviados hoje
     */
    public int contarAlertasEnviadosHoje() {
        String sql = "SELECT COUNT(*) FROM alertas_enviados WHERE DATE(data_envio) = DATE('now', 'localtime')";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            logger.error("Erro ao contar alertas enviados hoje", e);
        }

        return 0;
    }
}
