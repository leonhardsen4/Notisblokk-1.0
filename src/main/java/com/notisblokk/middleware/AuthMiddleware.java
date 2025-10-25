package com.notisblokk.middleware;

import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware para verificação de autenticação.
 *
 * <p>Intercepta requisições e verifica se o usuário está autenticado.
 * Se não estiver, redireciona para a página de login.</p>
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * app.get("/dashboard", AuthMiddleware.require(), dashboardController::index);
 * </pre>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class AuthMiddleware implements Handler {

    private static final Logger logger = LoggerFactory.getLogger(AuthMiddleware.class);

    /**
     * Cria uma instância do middleware de autenticação.
     *
     * @return Handler middleware configurado
     */
    public static Handler require() {
        return new AuthMiddleware();
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // Verificar se usuário está autenticado
        if (!SessionUtil.isAuthenticated(ctx)) {
            logger.warn("Acesso não autorizado tentado: {} (IP: {})",
                       ctx.path(), SessionUtil.getClientIp(ctx));

            // Salvar URL original para redirecionar após login
            ctx.sessionAttribute("redirectUrl", ctx.path());

            // Redirecionar para login
            ctx.redirect("/auth/login");
            return;
        }

        // Usuário autenticado, continuar para o próximo handler
        logger.debug("Acesso autorizado: {} (Usuário: {})",
                    ctx.path(), SessionUtil.getCurrentUserDisplayName(ctx));
    }
}
