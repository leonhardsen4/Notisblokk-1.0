package com.notisblokk.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Modelo que representa um usuário do sistema.
 *
 * <p>Armazena informações de autenticação, perfil e auditoria dos usuários.
 * As senhas são armazenadas como hash BCrypt, nunca em texto plano.</p>
 *
 * <p><b>Campos principais:</b></p>
 * <ul>
 *   <li>id: Identificador único</li>
 *   <li>username: Nome de usuário único para login</li>
 *   <li>email: Email único para login e comunicação</li>
 *   <li>passwordHash: Hash BCrypt da senha</li>
 *   <li>fullName: Nome completo do usuário</li>
 *   <li>role: Papel do usuário (ADMIN ou OPERATOR)</li>
 *   <li>active: Indica se o usuário está ativo no sistema</li>
 *   <li>createdAt: Data/hora de criação</li>
 *   <li>updatedAt: Data/hora da última atualização</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class User {

    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private UserRole role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Novos campos de segurança e perfil
    private String fotoPerfil;
    private boolean emailVerificado;
    private String tokenVerificacao;
    private int tentativasLogin;
    private LocalDateTime bloqueadoAte;
    private LocalDateTime dataAlteracaoSenha;
    private LocalDateTime senhaExpiraEm;

    /**
     * Construtor padrão.
     */
    public User() {
        this.active = true;
        this.role = UserRole.OPERATOR; // Role padrão
        this.createdAt = LocalDateTime.now(BRAZIL_ZONE);
        this.updatedAt = LocalDateTime.now(BRAZIL_ZONE);
    }

    /**
     * Construtor completo.
     *
     * @param id ID do usuário
     * @param username nome de usuário
     * @param email email do usuário
     * @param passwordHash hash BCrypt da senha
     * @param fullName nome completo
     * @param role papel do usuário
     * @param active status de ativação
     * @param createdAt data/hora de criação
     * @param updatedAt data/hora de atualização
     */
    public User(Long id, String username, String email, String passwordHash, String fullName,
                UserRole role, boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.emailVerificado = false;
        this.tentativasLogin = 0;
    }

    // ========== Getters e Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public String getTokenVerificacao() {
        return tokenVerificacao;
    }

    public void setTokenVerificacao(String tokenVerificacao) {
        this.tokenVerificacao = tokenVerificacao;
    }

    public int getTentativasLogin() {
        return tentativasLogin;
    }

    public void setTentativasLogin(int tentativasLogin) {
        this.tentativasLogin = tentativasLogin;
    }

    public LocalDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(LocalDateTime bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }

    public LocalDateTime getDataAlteracaoSenha() {
        return dataAlteracaoSenha;
    }

    public void setDataAlteracaoSenha(LocalDateTime dataAlteracaoSenha) {
        this.dataAlteracaoSenha = dataAlteracaoSenha;
    }

    public LocalDateTime getSenhaExpiraEm() {
        return senhaExpiraEm;
    }

    public void setSenhaExpiraEm(LocalDateTime senhaExpiraEm) {
        this.senhaExpiraEm = senhaExpiraEm;
    }

    // ========== Métodos Auxiliares ==========

    /**
     * Retorna a data de criação formatada no padrão brasileiro.
     *
     * @return String data formatada (dd/MM/yyyy HH:mm:ss)
     */
    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(FORMATTER) : "";
    }

    /**
     * Retorna a data de atualização formatada no padrão brasileiro.
     *
     * @return String data formatada (dd/MM/yyyy HH:mm:ss)
     */
    public String getFormattedUpdatedAt() {
        return updatedAt != null ? updatedAt.format(FORMATTER) : "";
    }

    /**
     * Verifica se o usuário é administrador.
     *
     * @return boolean true se role for ADMIN
     */
    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }

    /**
     * Verifica se o usuário é operador.
     *
     * @return boolean true se role for OPERATOR
     */
    public boolean isOperator() {
        return role != null && role.isOperator();
    }

    /**
     * Retorna o nome de exibição do papel do usuário.
     *
     * @return String nome amigável do papel
     */
    public String getRoleDisplayName() {
        return role != null ? role.getDisplayName() : "";
    }

    /**
     * Retorna as iniciais do nome completo do usuário.
     * Útil para exibir avatares.
     *
     * @return String iniciais (máximo 2 letras)
     */
    public String getInitials() {
        if (fullName == null || fullName.trim().isEmpty()) {
            return username != null && !username.isEmpty()
                ? username.substring(0, 1).toUpperCase()
                : "?";
        }

        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
    }

    /**
     * Verifica se o usuário está bloqueado.
     *
     * @return boolean true se estiver bloqueado
     */
    public boolean isBloqueado() {
        if (bloqueadoAte == null) return false;
        return LocalDateTime.now(BRAZIL_ZONE).isBefore(bloqueadoAte);
    }

    /**
     * Verifica se a senha expirou.
     *
     * @return boolean true se a senha expirou
     */
    public boolean isSenhaExpirada() {
        if (senhaExpiraEm == null) return false;
        return LocalDateTime.now(BRAZIL_ZONE).isAfter(senhaExpiraEm);
    }

    /**
     * Retorna quantos dias faltam para a senha expirar.
     *
     * @return int dias restantes (negativo se já expirou)
     */
    public long getDiasParaExpirarSenha() {
        if (senhaExpiraEm == null) return Long.MAX_VALUE;
        return java.time.Duration.between(LocalDateTime.now(BRAZIL_ZONE), senhaExpiraEm).toDays();
    }

    // ========== equals, hashCode e toString ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
               Objects.equals(username, user.username) &&
               Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", active=" + active +
                ", createdAt=" + getFormattedCreatedAt() +
                ", updatedAt=" + getFormattedUpdatedAt() +
                '}';
    }
}
