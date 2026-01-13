package com.notisblokk.util;

import com.notisblokk.config.DatabaseConfig;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Utilitário para resetar a senha do usuário admin.
 *
 * Execute este arquivo diretamente para redefinir a senha do admin.
 */
public class ResetAdminPassword {

    private static final Logger logger = LoggerFactory.getLogger(ResetAdminPassword.class);

    public static void main(String[] args) {
        try {
            // Inicializar banco de dados
            DatabaseConfig.initialize();

            // Nova senha
            String novaSenha = "12345";
            String senhaHash = BCrypt.hashpw(novaSenha, BCrypt.gensalt(12));

            // Atualizar senha do admin
            try (Connection conn = DatabaseConfig.getConnection()) {
                // Primeiro, verificar se o admin existe
                String checkSql = "SELECT id, username, email FROM users WHERE email = 'admin@notisblokk.com'";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                     ResultSet rs = checkStmt.executeQuery()) {

                    if (rs.next()) {
                        Long adminId = rs.getLong("id");
                        String username = rs.getString("username");
                        String email = rs.getString("email");

                        // Atualizar senha
                        String updateSql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, senhaHash);
                            updateStmt.setLong(2, adminId);

                            int rowsAffected = updateStmt.executeUpdate();

                            if (rowsAffected > 0) {
                                System.out.println("========================================");
                                System.out.println("✓ SENHA RESETADA COM SUCESSO!");
                                System.out.println("========================================");
                                System.out.println("Usuário: " + username);
                                System.out.println("Email: " + email);
                                System.out.println("Nova senha: " + novaSenha);
                                System.out.println("========================================");
                                System.out.println("Você já pode fazer login com as novas credenciais.");
                                System.out.println("========================================");
                            } else {
                                System.err.println("Erro: Nenhuma linha foi atualizada.");
                            }
                        }
                    } else {
                        System.err.println("Erro: Usuário admin não encontrado no banco de dados.");
                        System.err.println("Email procurado: admin@notisblokk.com");
                    }
                }
            }

            // Fechar conexão
            DatabaseConfig.close();

        } catch (Exception e) {
            System.err.println("Erro ao resetar senha do admin:");
            e.printStackTrace();
        }
    }
}
