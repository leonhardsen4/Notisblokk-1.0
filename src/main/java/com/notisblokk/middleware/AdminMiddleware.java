package com.notisblokk.middleware;

import com.notisblokk.util.SessionUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Middleware para verificação de permissão de administrador.
 *
 * <p>Intercepta requisições e verifica se o usuário autenticado possui
 * role de ADMIN. Se não possuir, retorna erro 403 (Forbidden).</p>
 *
 * <p><b>Importante:</b> Este middleware deve ser usado APÓS o AuthMiddleware,
 * pois assume que o usuário já está autenticado.</p>
 *
 * <p><b>Uso:</b></p>
 * <pre>
 * app.get("/admin/users", AuthMiddleware.require(), AdminMiddleware.require(), userController::list);
 * </pre>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class AdminMiddleware implements Handler {

    private static final Logger logger = LoggerFactory.getLogger(AdminMiddleware.class);

    /**
     * Cria uma instância do middleware de verificação de admin.
     *
     * @return Handler middleware configurado
     */
    public static Handler require() {
        return new AdminMiddleware();
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // Verificar se usuário está autenticado (failsafe)
        if (!SessionUtil.isAuthenticated(ctx)) {
            logger.warn("AdminMiddleware: Usuário não autenticado tentou acessar: {}", ctx.path());
            ctx.redirect("/auth/login");
            return;
        }

        // Verificar se usuário é admin
        if (!SessionUtil.isAdmin(ctx)) {
            logger.warn("Acesso negado (não-admin): {} (Usuário: {}, IP: {})",
                       ctx.path(),
                       SessionUtil.getCurrentUserDisplayName(ctx),
                       SessionUtil.getClientIp(ctx));

            // Retornar erro 403 Forbidden
            ctx.status(403);
            ctx.result("Acesso negado. Apenas administradores podem acessar esta página.");
            return;
        }

        // Usuário é admin, continuar para o próximo handler
        logger.debug("Acesso admin autorizado: {} (Usuário: {})",
                    ctx.path(), SessionUtil.getCurrentUserDisplayName(ctx));
    }
}
