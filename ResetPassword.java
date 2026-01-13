import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Script simples para resetar a senha do admin.
 * Compile e execute: java ResetPassword.java
 */
public class ResetPassword {
    public static void main(String[] args) {
        String dbPath = "jdbc:sqlite:notisblokk.db";

        // Hash BCrypt da senha "12345" com cost factor 12
        String novaSenhaHash = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYILSBL9C4m";

        try {
            // Conectar ao banco de dados
            Connection conn = DriverManager.getConnection(dbPath);

            // Verificar se o admin existe
            String checkSql = "SELECT id, username, email, role FROM users WHERE email = 'admin@notisblokk.com'";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                Long adminId = rs.getLong("id");
                String username = rs.getString("username");
                String email = rs.getString("email");
                String role = rs.getString("role");

                // Atualizar senha
                String updateSql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, novaSenhaHash);
                updateStmt.setLong(2, adminId);

                int rowsUpdated = updateStmt.executeUpdate();

                if (rowsUpdated > 0) {
                    System.out.println("========================================");
                    System.out.println("✓ SENHA RESETADA COM SUCESSO!");
                    System.out.println("========================================");
                    System.out.println("ID: " + adminId);
                    System.out.println("Username: " + username);
                    System.out.println("Email: " + email);
                    System.out.println("Role: " + role);
                    System.out.println("Nova senha: 12345");
                    System.out.println("========================================");
                    System.out.println("Você já pode fazer login!");
                    System.out.println("========================================");
                } else {
                    System.err.println("Erro: Nenhuma linha foi atualizada.");
                }

                updateStmt.close();
            } else {
                System.err.println("Erro: Usuário admin não encontrado!");
                System.err.println("Email procurado: admin@notisblokk.com");
            }

            rs.close();
            checkStmt.close();
            conn.close();

        } catch (Exception e) {
            System.err.println("Erro ao resetar senha:");
            e.printStackTrace();
        }
    }
}
