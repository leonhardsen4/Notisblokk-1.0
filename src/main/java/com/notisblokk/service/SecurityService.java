package com.notisblokk.service;

import com.notisblokk.config.AppConfig;
import com.notisblokk.config.DatabaseConfig;
import com.notisblokk.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Serviço de segurança para gerenciar autenticação e controle de acesso.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Gerenciar tentativas de login</li>
 *   <li>Bloquear usuários após múltiplas tentativas</li>
 *   <li>Gerar e validar tokens de verificação</li>
 *   <li>Gerenciar expiração de senhas</li>
 *   <li>Enviar emails de confirmação e recuperação</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EmailService emailService;

    public SecurityService() {
        this.emailService = new EmailService();
    }

    /**
     * Registra uma tentativa de login falhada.
     *
     * @param userId ID do usuário
     * @throws SQLException se houver erro ao atualizar
     */
    public void registrarTentativaFalha(Long userId) throws SQLException {
        String sql = """
            UPDATE users
            SET tentativas_login = tentativas_login + 1
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.executeUpdate();

            logger.info("Tentativa de login falhada registrada para usuário ID: {}", userId);

            // Verificar se deve bloquear
            verificarEBloquearUsuario(userId);
        }
    }

    /**
     * Reseta as tentativas de login após sucesso.
     *
     * @param userId ID do usuário
     * @throws SQLException se houver erro ao atualizar
     */
    public void resetarTentativas(Long userId) throws SQLException {
        String sql = """
            UPDATE users
            SET tentativas_login = 0
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.executeUpdate();

            logger.info("Tentativas de login resetadas para usuário ID: {}", userId);
        }
    }

    /**
     * Verifica quantas tentativas o usuário tem e bloqueia se necessário.
     *
     * @param userId ID do usuário
     * @throws SQLException se houver erro ao acessar banco
     */
    private void verificarEBloquearUsuario(Long userId) throws SQLException {
        String selectSql = "SELECT tentativas_login FROM users WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int tentativas = rs.getInt("tentativas_login");

                    if (tentativas >= AppConfig.getSecurityLoginMaxAttempts()) {
                        bloquearUsuario(userId);
                    }
                }
            }
        }
    }

    /**
     * Bloqueia um usuário por um período de tempo.
     *
     * @param userId ID do usuário
     * @throws SQLException se houver erro ao atualizar
     */
    public void bloquearUsuario(Long userId) throws SQLException {
        LocalDateTime bloqueadoAte = LocalDateTime.now(BRAZIL_ZONE)
                .plusMinutes(AppConfig.getSecurityLoginLockoutMinutes());

        String sql = """
            UPDATE users
            SET bloqueado_ate = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bloqueadoAte.format(FORMATTER));
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();

            logger.warn("Usuário ID {} bloqueado até {}", userId, bloqueadoAte.format(FORMATTER));
        }
    }

    /**
     * Verifica se um usuário está bloqueado.
     *
     * @param user objeto User
     * @return true se estiver bloqueado
     */
    public boolean isUsuarioBloqueado(User user) {
        if (user.getBloqueadoAte() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
        return now.isBefore(user.getBloqueadoAte());
    }

    /**
     * Desbloqueia um usuário manualmente.
     *
     * @param userId ID do usuário
     * @throws SQLException se houver erro ao atualizar
     */
    public void desbloquearUsuario(Long userId) throws SQLException {
        String sql = """
            UPDATE users
            SET bloqueado_ate = NULL,
                tentativas_login = 0
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.executeUpdate();

            logger.info("Usuário ID {} desbloqueado manualmente", userId);
        }
    }

    /**
     * Gera um token único para verificação de email ou recuperação de senha.
     *
     * @return token UUID
     */
    public String gerarToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Salva um token de verificação de email para o usuário.
     *
     * @param userId ID do usuário
     * @param token token gerado
     * @throws SQLException se houver erro ao salvar
     */
    public void salvarTokenVerificacao(Long userId, String token) throws SQLException {
        String sql = """
            UPDATE users
            SET token_verificacao = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, token);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();

            logger.info("Token de verificação salvo para usuário ID: {}", userId);
        }
    }

    /**
     * Verifica um token de email e marca o email como verificado.
     *
     * @param token token de verificação
     * @return true se verificado com sucesso
     * @throws SQLException se houver erro ao acessar banco
     */
    public boolean verificarEmail(String token) throws SQLException {
        String selectSql = "SELECT id FROM users WHERE token_verificacao = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            pstmt.setString(1, token);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Long userId = rs.getLong("id");

                    // Marcar email como verificado
                    String updateSql = """
                        UPDATE users
                        SET email_verificado = 1,
                            token_verificacao = NULL
                        WHERE id = ?
                    """;

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setLong(1, userId);
                        updateStmt.executeUpdate();

                        logger.info("Email verificado com sucesso para usuário ID: {}", userId);
                        return true;
                    }
                }
            }
        }

        logger.warn("Token de verificação inválido: {}", token);
        return false;
    }

    /**
     * Define a data de expiração da senha do usuário.
     *
     * @param userId ID do usuário
     * @throws SQLException se houver erro ao atualizar
     */
    public void definirExpiracaoSenha(Long userId) throws SQLException {
        LocalDateTime dataAlteracao = LocalDateTime.now(BRAZIL_ZONE);
        LocalDateTime expiraEm = dataAlteracao.plusMonths(AppConfig.getSecurityPasswordExpirationMonths());

        String sql = """
            UPDATE users
            SET data_alteracao_senha = ?,
                senha_expira_em = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dataAlteracao.format(FORMATTER));
            pstmt.setString(2, expiraEm.format(FORMATTER));
            pstmt.setLong(3, userId);
            pstmt.executeUpdate();

            logger.info("Expiração de senha definida para usuário ID {}: expira em {}",
                       userId, expiraEm.format(FORMATTER));
        }
    }

    /**
     * Verifica se a senha do usuário expirou.
     *
     * @param user objeto User
     * @return true se a senha expirou
     */
    public boolean isSenhaExpirada(User user) {
        if (user.getSenhaExpiraEm() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(BRAZIL_ZONE);
        return now.isAfter(user.getSenhaExpiraEm());
    }

    /**
     * Verifica se a senha está próxima de expirar e envia aviso se necessário.
     *
     * @param user objeto User
     * @throws Exception se houver erro ao enviar email
     */
    public void verificarEAvisarExpiracaoSenha(User user) throws Exception {
        if (user.getSenhaExpiraEm() == null) {
            return;
        }

        long diasRestantes = user.getDiasParaExpirarSenha();
        int diasAviso = AppConfig.getSecurityPasswordWarningDays();

        // Se faltam menos dias que o configurado, enviar aviso
        if (diasRestantes > 0 && diasRestantes <= diasAviso) {
            logger.info("Enviando aviso de expiração de senha para usuário: {} (faltam {} dias)",
                       user.getEmail(), diasRestantes);
            emailService.enviarEmailAvisoExpiracaoSenha(user.getEmail(), user.getFullName(), (int) diasRestantes);
        }
    }

    /**
     * Envia email de confirmação de cadastro.
     *
     * @param user objeto User
     * @throws Exception se houver erro ao enviar email
     */
    public void enviarEmailConfirmacao(User user) throws Exception {
        String token = gerarToken();
        salvarTokenVerificacao(user.getId(), token);

        emailService.enviarEmailConfirmacao(user.getEmail(), user.getFullName(), token);
        logger.info("Email de confirmação enviado para: {}", user.getEmail());
    }

    /**
     * Envia email de recuperação de senha.
     *
     * @param user objeto User
     * @return token gerado
     * @throws Exception se houver erro ao enviar email
     */
    public String enviarEmailRecuperacaoSenha(User user) throws Exception {
        String token = gerarToken();
        salvarTokenVerificacao(user.getId(), token);

        emailService.enviarEmailRecuperacaoSenha(user.getEmail(), user.getFullName(), token);
        logger.info("Email de recuperação de senha enviado para: {}", user.getEmail());

        return token;
    }

    /**
     * Valida e consome um token de recuperação de senha.
     *
     * @param token token de recuperação
     * @return ID do usuário se o token for válido, null caso contrário
     * @throws SQLException se houver erro ao acessar banco
     */
    public Long validarTokenRecuperacao(String token) throws SQLException {
        String sql = "SELECT id FROM users WHERE token_verificacao = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, token);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        return null;
    }

    /**
     * Limpa o token de verificação após uso.
     *
     * @param userId ID do usuário
     * @throws SQLException se houver erro ao atualizar
     */
    public void limparToken(Long userId) throws SQLException {
        String sql = "UPDATE users SET token_verificacao = NULL WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        }
    }
}
