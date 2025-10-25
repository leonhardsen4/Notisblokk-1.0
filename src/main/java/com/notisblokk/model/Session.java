package com.notisblokk.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Modelo que representa uma sessão de usuário no sistema.
 *
 * <p>Armazena informações sobre acessos e login/logout dos usuários,
 * servindo como log de auditoria e controle de sessões ativas.</p>
 *
 * <p><b>Campos principais:</b></p>
 * <ul>
 *   <li>id: Identificador único da sessão</li>
 *   <li>userId: ID do usuário que criou a sessão</li>
 *   <li>loginTime: Data/hora do login</li>
 *   <li>logoutTime: Data/hora do logout (null se ainda ativa)</li>
 *   <li>ipAddress: Endereço IP do cliente</li>
 *   <li>userAgent: User agent do navegador</li>
 *   <li>status: Status da sessão (ACTIVE, LOGGED_OUT, EXPIRED)</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class Session {

    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Long id;
    private Long userId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String ipAddress;
    private String userAgent;
    private SessionStatus status;

    /**
     * Construtor padrão.
     */
    public Session() {
        this.loginTime = LocalDateTime.now(BRAZIL_ZONE);
        this.status = SessionStatus.ACTIVE;
    }

    /**
     * Construtor completo.
     *
     * @param id ID da sessão
     * @param userId ID do usuário
     * @param loginTime data/hora do login
     * @param logoutTime data/hora do logout
     * @param ipAddress endereço IP
     * @param userAgent user agent do navegador
     * @param status status da sessão
     */
    public Session(Long id, Long userId, LocalDateTime loginTime, LocalDateTime logoutTime,
                   String ipAddress, String userAgent, SessionStatus status) {
        this.id = id;
        this.userId = userId;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.status = status;
    }

    // ========== Getters e Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    // ========== Métodos Auxiliares ==========

    /**
     * Retorna a data/hora de login formatada no padrão brasileiro.
     *
     * @return String data formatada (dd/MM/yyyy HH:mm:ss)
     */
    public String getFormattedLoginTime() {
        return loginTime != null ? loginTime.format(FORMATTER) : "";
    }

    /**
     * Retorna a data/hora de logout formatada no padrão brasileiro.
     *
     * @return String data formatada (dd/MM/yyyy HH:mm:ss) ou "-" se ainda não fez logout
     */
    public String getFormattedLogoutTime() {
        return logoutTime != null ? logoutTime.format(FORMATTER) : "-";
    }

    /**
     * Retorna o nome de exibição do status da sessão.
     *
     * @return String nome amigável do status
     */
    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "";
    }

    /**
     * Verifica se a sessão está ativa.
     *
     * @return boolean true se status for ACTIVE
     */
    public boolean isActive() {
        return status != null && status.isActive();
    }

    /**
     * Verifica se a sessão foi encerrada (logout ou expirada).
     *
     * @return boolean true se status for LOGGED_OUT ou EXPIRED
     */
    public boolean isTerminated() {
        return status != null && status.isTerminated();
    }

    /**
     * Encerra a sessão por logout.
     * Define logoutTime e atualiza status para LOGGED_OUT.
     */
    public void logout() {
        this.logoutTime = LocalDateTime.now(BRAZIL_ZONE);
        this.status = SessionStatus.LOGGED_OUT;
    }

    /**
     * Marca a sessão como expirada.
     * Define logoutTime e atualiza status para EXPIRED.
     */
    public void expire() {
        this.logoutTime = LocalDateTime.now(BRAZIL_ZONE);
        this.status = SessionStatus.EXPIRED;
    }

    /**
     * Retorna a duração da sessão em minutos.
     *
     * @return long duração em minutos
     */
    public long getDurationMinutes() {
        if (loginTime == null) return 0;

        LocalDateTime end = logoutTime != null ? logoutTime : LocalDateTime.now(BRAZIL_ZONE);
        return java.time.Duration.between(loginTime, end).toMinutes();
    }

    /**
     * Retorna a duração da sessão formatada (Xh Ym).
     *
     * @return String duração formatada
     */
    public String getFormattedDuration() {
        long minutes = getDurationMinutes();
        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours > 0) {
            return hours + "h " + mins + "m";
        } else {
            return mins + "m";
        }
    }

    /**
     * Extrai o navegador do user agent (simplificado).
     *
     * @return String nome do navegador ou "Desconhecido"
     */
    public String getBrowser() {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Desconhecido";
        }

        if (userAgent.contains("Edg")) return "Edge";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Opera") || userAgent.contains("OPR")) return "Opera";

        return "Outro";
    }

    // ========== equals, hashCode e toString ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return Objects.equals(id, session.id) &&
               Objects.equals(userId, session.userId) &&
               Objects.equals(loginTime, session.loginTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, loginTime);
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", userId=" + userId +
                ", loginTime=" + getFormattedLoginTime() +
                ", logoutTime=" + getFormattedLogoutTime() +
                ", ipAddress='" + ipAddress + '\'' +
                ", browser='" + getBrowser() + '\'' +
                ", status=" + status +
                ", duration='" + getFormattedDuration() + '\'' +
                '}';
    }
}
