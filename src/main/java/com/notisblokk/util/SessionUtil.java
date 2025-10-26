package com.notisblokk.util;

import com.notisblokk.model.User;
import io.javalin.http.Context;

import java.util.UUID;

/**
 * Utilitário para gerenciamento de sessões HTTP.
 *
 * <p>Fornece métodos para:</p>
 * <ul>
 *   <li>Armazenar e recuperar usuário da sessão</li>
 *   <li>Verificar autenticação</li>
 *   <li>Extrair informações do request (IP, User Agent)</li>
 *   <li>Gerar tokens de sessão</li>
 * </ul>
 *
 * <p><b>Atributos de sessão:</b></p>
 * <ul>
 *   <li>currentUser: usuário logado</li>
 *   <li>sessionId: ID da sessão no banco</li>
 *   <li>theme: tema preferido (light/dark)</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class SessionUtil {

    // Chaves para atributos de sessão
    private static final String ATTR_CURRENT_USER = "currentUser";
    private static final String ATTR_SESSION_ID = "sessionId";
    private static final String ATTR_THEME = "theme";

    // Tema padrão
    private static final String DEFAULT_THEME = "light";

    /**
     * Armazena o usuário atual na sessão HTTP.
     *
     * @param ctx contexto do Javalin
     * @param user usuário a ser armazenado
     */
    public static void setCurrentUser(Context ctx, User user) {
        if (ctx != null && user != null) {
            ctx.sessionAttribute(ATTR_CURRENT_USER, user);
        }
    }

    /**
     * Recupera o usuário atual da sessão HTTP.
     *
     * @param ctx contexto do Javalin
     * @return User usuário logado ou null se não houver
     */
    public static User getCurrentUser(Context ctx) {
        if (ctx == null) {
            return null;
        }

        return ctx.sessionAttribute(ATTR_CURRENT_USER);
    }

    /**
     * Armazena o ID da sessão do banco na sessão HTTP.
     *
     * @param ctx contexto do Javalin
     * @param sessionId ID da sessão no banco de dados
     */
    public static void setSessionId(Context ctx, Long sessionId) {
        if (ctx != null && sessionId != null) {
            ctx.sessionAttribute(ATTR_SESSION_ID, sessionId);
        }
    }

    /**
     * Recupera o ID da sessão do banco da sessão HTTP.
     *
     * @param ctx contexto do Javalin
     * @return Long ID da sessão ou null se não houver
     */
    public static Long getSessionId(Context ctx) {
        if (ctx == null) {
            return null;
        }

        return ctx.sessionAttribute(ATTR_SESSION_ID);
    }

    /**
     * Armazena o tema preferido na sessão HTTP.
     *
     * @param ctx contexto do Javalin
     * @param theme tema (light ou dark)
     */
    public static void setTheme(Context ctx, String theme) {
        if (ctx != null && theme != null) {
            ctx.sessionAttribute(ATTR_THEME, theme);
        }
    }

    /**
     * Recupera o tema preferido da sessão HTTP.
     *
     * @param ctx contexto do Javalin
     * @return String tema (light ou dark), padrão "light"
     */
    public static String getTheme(Context ctx) {
        if (ctx == null) {
            return DEFAULT_THEME;
        }

        String theme = ctx.sessionAttribute(ATTR_THEME);
        return theme != null ? theme : DEFAULT_THEME;
    }

    /**
     * Verifica se há um usuário autenticado na sessão.
     *
     * @param ctx contexto do Javalin
     * @return boolean true se houver usuário logado, false caso contrário
     */
    public static boolean isAuthenticated(Context ctx) {
        return getCurrentUser(ctx) != null;
    }

    /**
     * Verifica se o usuário autenticado é administrador.
     *
     * @param ctx contexto do Javalin
     * @return boolean true se for admin, false caso contrário
     */
    public static boolean isAdmin(Context ctx) {
        User user = getCurrentUser(ctx);
        return user != null && user.isAdmin();
    }

    /**
     * Verifica se o usuário autenticado é operador.
     *
     * @param ctx contexto do Javalin
     * @return boolean true se for operador, false caso contrário
     */
    public static boolean isOperator(Context ctx) {
        User user = getCurrentUser(ctx);
        return user != null && user.isOperator();
    }

    /**
     * Limpa a sessão HTTP (logout).
     *
     * @param ctx contexto do Javalin
     */
    public static void clearSession(Context ctx) {
        if (ctx != null) {
            ctx.req().getSession().invalidate();
        }
    }

    /**
     * Extrai o endereço IP do cliente do contexto.
     *
     * <p>Verifica headers de proxy (X-Forwarded-For, X-Real-IP) antes
     * de usar o IP direto da conexão.</p>
     *
     * @param ctx contexto do Javalin
     * @return String endereço IP do cliente
     */
    public static String getClientIp(Context ctx) {
        if (ctx == null) {
            return "unknown";
        }

        // Verificar headers de proxy primeiro
        String xForwardedFor = ctx.header("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For pode conter múltiplos IPs, pegar o primeiro
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = ctx.header("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }

        // Fallback para IP direto
        String ip = ctx.ip();
        return ip != null ? ip : "unknown";
    }

    /**
     * Extrai o User Agent do navegador do contexto.
     *
     * @param ctx contexto do Javalin
     * @return String user agent do navegador
     */
    public static String getUserAgent(Context ctx) {
        if (ctx == null) {
            return "unknown";
        }

        String userAgent = ctx.userAgent();
        return userAgent != null ? userAgent : "unknown";
    }

    /**
     * Gera um token único para sessão.
     *
     * <p>Utiliza UUID versão 4 (aleatório) para gerar tokens únicos
     * que podem ser usados como identificadores de sessão.</p>
     *
     * @return String token único
     */
    public static String generateSessionToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Obtém o ID do usuário da sessão atual.
     *
     * @param ctx contexto do Javalin
     * @return Long ID do usuário ou null se não autenticado
     */
    public static Long getCurrentUserId(Context ctx) {
        User user = getCurrentUser(ctx);
        return user != null ? user.getId() : null;
    }

    /**
     * Obtém o ID da sessão atual (alias para getSessionId).
     *
     * @param ctx contexto do Javalin
     * @return Long ID da sessão ou null se não houver
     */
    public static Long getCurrentSessionId(Context ctx) {
        return getSessionId(ctx);
    }

    /**
     * Obtém o nome de exibição do usuário da sessão atual.
     *
     * @param ctx contexto do Javalin
     * @return String nome do usuário ou "Visitante" se não autenticado
     */
    public static String getCurrentUserDisplayName(Context ctx) {
        User user = getCurrentUser(ctx);
        return user != null ? user.getFullName() : "Visitante";
    }

    /**
     * Verifica se a requisição é AJAX.
     *
     * @param ctx contexto do Javalin
     * @return boolean true se for requisição AJAX, false caso contrário
     */
    public static boolean isAjaxRequest(Context ctx) {
        if (ctx == null) {
            return false;
        }

        String xRequestedWith = ctx.header("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(xRequestedWith);
    }

    /**
     * Adiciona atributos comuns de sessão ao modelo do template.
     *
     * <p>Útil para disponibilizar dados da sessão em todos os templates.</p>
     *
     * @param ctx contexto do Javalin
     * @return java.util.Map<String, Object> mapa com atributos da sessão
     */
    public static java.util.Map<String, Object> getSessionAttributes(Context ctx) {
        java.util.Map<String, Object> attributes = new java.util.HashMap<>();

        User user = getCurrentUser(ctx);
        if (user != null) {
            attributes.put("currentUser", user);
            attributes.put("isAuthenticated", true);
            attributes.put("isAdmin", user.isAdmin());
            attributes.put("isOperator", user.isOperator());
            attributes.put("userName", user.getFullName());
            attributes.put("userInitials", user.getInitials());
        } else {
            attributes.put("isAuthenticated", false);
            attributes.put("isAdmin", false);
            attributes.put("isOperator", false);
        }

        attributes.put("theme", getTheme(ctx));

        return attributes;
    }

    /**
     * Extrai o navegador do User Agent (simplificado).
     *
     * @param userAgent string do user agent
     * @return String nome do navegador
     */
    public static String extractBrowser(String userAgent) {
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

    /**
     * Extrai o sistema operacional do User Agent (simplificado).
     *
     * @param userAgent string do user agent
     * @return String nome do sistema operacional
     */
    public static String extractOS(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Desconhecido";
        }

        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac")) return "macOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iOS") || userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS";

        return "Outro";
    }
}
