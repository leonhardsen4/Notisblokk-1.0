package com.notisblokk.controller;

import com.notisblokk.model.User;
import com.notisblokk.model.UserRole;
import com.notisblokk.service.AuthService;
import com.notisblokk.service.UserService;
import com.notisblokk.service.SecurityService;
import com.notisblokk.util.SessionUtil;
import com.notisblokk.util.ValidationUtil;
import io.javalin.http.Context;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller responsável pelas rotas de autenticação.
 *
 * <p>Gerencia os endpoints relacionados a:</p>
 * <ul>
 *   <li>Login (GET e POST)</li>
 *   <li>Registro de novos usuários (GET e POST)</li>
 *   <li>Recuperação de senha (GET e POST)</li>
 *   <li>Logout</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final UserService userService;
    private final SecurityService securityService;

    /**
     * Construtor padrão.
     */
    public AuthController() {
        this.authService = new AuthService();
        this.userService = new UserService();
        this.securityService = new SecurityService();
    }

    /**
     * GET /auth/login
     * Exibe a página de login.
     */
    public void showLogin(Context ctx) {
        // Se já estiver autenticado, redirecionar para dashboard
        if (SessionUtil.isAuthenticated(ctx)) {
            ctx.redirect("/dashboard");
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Login - Notisblokk");
        model.put("theme", SessionUtil.getTheme(ctx));

        // Recuperar mensagem de erro se houver
        String error = ctx.sessionAttribute("loginError");
        if (error != null) {
            model.put("error", error);
            ctx.sessionAttribute("loginError", null); // Limpar após exibir
        }

        // Recuperar mensagem de sucesso se houver
        String success = ctx.sessionAttribute("loginSuccess");
        if (success != null) {
            model.put("success", success);
            ctx.sessionAttribute("loginSuccess", null); // Limpar após exibir
        }

        // Configurar charset UTF-8
        ctx.contentType("text/html; charset=utf-8");
        ctx.render("auth/login", model);
    }

    /**
     * POST /auth/login
     * Processa o login do usuário.
     */
    public void processLogin(Context ctx) {
        String usernameOrEmail = null;
        try {
            // Obter dados do formulário
            usernameOrEmail = ctx.formParam("username");
            String password = ctx.formParam("password");
            String remember = ctx.formParam("remember");

            // Validar campos vazios
            if (ValidationUtil.isNullOrEmpty(usernameOrEmail) || ValidationUtil.isNullOrEmpty(password)) {
                ctx.sessionAttribute("loginError", "Por favor, preencha todos os campos");
                ctx.redirect("/auth/login");
                return;
            }

            // Obter IP e User Agent
            String ipAddress = SessionUtil.getClientIp(ctx);
            String userAgent = SessionUtil.getUserAgent(ctx);

            // Tentar autenticar
            AuthService.LoginResult result = authService.autenticar(
                    usernameOrEmail.trim(),
                    password,
                    ipAddress,
                    userAgent
            );

            // Armazenar usuário e session ID na sessão HTTP
            SessionUtil.setCurrentUser(ctx, result.getUser());
            SessionUtil.setSessionId(ctx, result.getSession().getId());

            logger.info("Login bem-sucedido: {} (IP: {})", result.getUser().getUsername(), ipAddress);

            // Redirecionar para URL original ou dashboard
            String redirectUrl = ctx.sessionAttribute("redirectUrl");
            ctx.sessionAttribute("redirectUrl", null); // Limpar

            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                ctx.redirect(redirectUrl);
            } else {
                ctx.redirect("/dashboard");
            }

        } catch (AuthService.AuthenticationException e) {
            logger.warn("Falha no login: {}", e.getMessage());
            ctx.sessionAttribute("loginError", e.getMessage());
            ctx.redirect("/auth/login");

        } catch (Exception e) {
            logger.error("Erro ao processar login - Username: {}", usernameOrEmail, e);
            logger.error("Stack trace completo:", e);
            ctx.sessionAttribute("loginError", "Erro ao processar login: " + e.getMessage());
            ctx.redirect("/auth/login");
        }
    }

    /**
     * GET /auth/register
     * Exibe a página de registro.
     */
    public void showRegister(Context ctx) {
        // Se já estiver autenticado, redirecionar para dashboard
        if (SessionUtil.isAuthenticated(ctx)) {
            ctx.redirect("/dashboard");
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Cadastro - Notisblokk");
        model.put("theme", SessionUtil.getTheme(ctx));

        // Recuperar mensagem de erro se houver
        String error = ctx.sessionAttribute("registerError");
        if (error != null) {
            model.put("error", error);
            ctx.sessionAttribute("registerError", null);
        }

        // Configurar charset UTF-8
        ctx.contentType("text/html; charset=utf-8");
        ctx.render("auth/register", model);
    }

    /**
     * POST /auth/register
     * Processa o registro de novo usuário.
     */
    public void processRegister(Context ctx) {
        try {
            // Obter dados do formulário
            String username = ctx.formParam("username");
            String email = ctx.formParam("email");
            String password = ctx.formParam("password");
            String confirmPassword = ctx.formParam("confirmPassword");
            String fullName = ctx.formParam("fullName");
            String termsAccepted = ctx.formParam("terms");

            // Validar campos vazios
            if (ValidationUtil.isNullOrEmpty(username) ||
                ValidationUtil.isNullOrEmpty(email) ||
                ValidationUtil.isNullOrEmpty(password) ||
                ValidationUtil.isNullOrEmpty(fullName)) {

                ctx.sessionAttribute("registerError", "Por favor, preencha todos os campos");
                ctx.redirect("/auth/register");
                return;
            }

            // Validar confirmação de senha
            if (!ValidationUtil.areEqual(password, confirmPassword)) {
                ctx.sessionAttribute("registerError", "As senhas não coincidem");
                ctx.redirect("/auth/register");
                return;
            }

            // Validar aceitação dos termos
            if (termsAccepted == null || !termsAccepted.equals("on")) {
                ctx.sessionAttribute("registerError", "Você deve aceitar os termos de uso");
                ctx.redirect("/auth/register");
                return;
            }

            // Criar usuário (sempre como OPERATOR)
            User newUser = userService.criarUsuario(
                username.trim(),
                email.trim(),
                password,
                fullName.trim(),
                UserRole.OPERATOR
            );

            logger.info("Novo usuário registrado: {} ({})", username, email);

            // Enviar email de confirmação
            try {
                securityService.enviarEmailConfirmacao(newUser);
                logger.info("Email de confirmação enviado para: {}", email);
            } catch (Exception emailError) {
                logger.error("Erro ao enviar email de confirmação", emailError);
                // Não bloquear o cadastro se email falhar
            }

            // Redirecionar para login com mensagem de sucesso
            ctx.sessionAttribute("loginSuccess",
                "Cadastro realizado com sucesso! Verifique seu email para confirmar sua conta.");
            ctx.redirect("/auth/login");

        } catch (Exception e) {
            logger.error("Erro ao registrar usuário", e);
            ctx.sessionAttribute("registerError", e.getMessage());
            ctx.redirect("/auth/register");
        }
    }

    /**
     * GET /auth/recover-password
     * Exibe a página de recuperação de senha.
     */
    public void showRecoverPassword(Context ctx) {
        // Se já estiver autenticado, redirecionar para dashboard
        if (SessionUtil.isAuthenticated(ctx)) {
            ctx.redirect("/dashboard");
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Recuperar Senha - Notisblokk");
        model.put("theme", SessionUtil.getTheme(ctx));

        // Recuperar mensagens se houver
        String error = ctx.sessionAttribute("recoverError");
        String success = ctx.sessionAttribute("recoverSuccess");

        if (error != null) {
            model.put("error", error);
            ctx.sessionAttribute("recoverError", null);
        }

        if (success != null) {
            model.put("success", success);
            ctx.sessionAttribute("recoverSuccess", null);
        }

        // Configurar charset UTF-8
        ctx.contentType("text/html; charset=utf-8");
        ctx.render("auth/recover-password", model);
    }

    /**
     * POST /auth/recover-password
     * Processa a solicitação de recuperação de senha.
     */
    public void processRecoverPassword(Context ctx) {
        try {
            String email = ctx.formParam("email");

            // Validar email
            if (ValidationUtil.isNullOrEmpty(email)) {
                ctx.sessionAttribute("recoverError", "Por favor, informe seu email");
                ctx.redirect("/auth/recover-password");
                return;
            }

            if (!ValidationUtil.isValidEmail(email)) {
                ctx.sessionAttribute("recoverError", "Email inválido");
                ctx.redirect("/auth/recover-password");
                return;
            }

            // Verificar se usuário existe
            var userOpt = userService.buscarPorEmail(email.trim());

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Enviar email de recuperação
                try {
                    securityService.enviarEmailRecuperacaoSenha(user);
                    logger.info("Email de recuperação enviado para: {}", user.getEmail());
                } catch (Exception emailError) {
                    logger.error("Erro ao enviar email de recuperação", emailError);
                }
            }

            // Sempre mostrar mensagem de sucesso por segurança
            // (não revelar se email existe ou não)
            ctx.sessionAttribute("recoverSuccess",
                "Se o email informado estiver cadastrado, você receberá instruções para recuperar sua senha.");
            ctx.redirect("/auth/recover-password");

        } catch (Exception e) {
            logger.error("Erro ao processar recuperação de senha", e);
            ctx.sessionAttribute("recoverError", "Erro ao processar solicitação. Tente novamente.");
            ctx.redirect("/auth/recover-password");
        }
    }

    /**
     * GET /auth/logout
     * Realiza o logout do usuário.
     */
    public void logout(Context ctx) {
        try {
            // Obter session ID antes de limpar
            Long sessionId = SessionUtil.getSessionId(ctx);
            String username = SessionUtil.getCurrentUserDisplayName(ctx);

            // Encerrar sessão no banco
            if (sessionId != null) {
                authService.logout(sessionId);
            }

            // Limpar sessão HTTP
            SessionUtil.clearSession(ctx);

            logger.info("Logout realizado: {} (Sessão ID: {})", username, sessionId);

            // Redirecionar para login com mensagem
            ctx.sessionAttribute("loginSuccess", "Logout realizado com sucesso!");
            ctx.redirect("/auth/login");

        } catch (Exception e) {
            logger.error("Erro ao processar logout", e);
            // Mesmo com erro, limpar sessão e redirecionar
            SessionUtil.clearSession(ctx);
            ctx.redirect("/auth/login");
        }
    }

    /**
     * GET /auth/verificar-email
     * Verifica o email do usuário através do token.
     */
    public void verificarEmail(Context ctx) {
        try {
            String token = ctx.queryParam("token");

            if (ValidationUtil.isNullOrEmpty(token)) {
                ctx.sessionAttribute("loginError", "Token de verificação inválido");
                ctx.redirect("/auth/login");
                return;
            }

            // Verificar token
            boolean verificado = securityService.verificarEmail(token);

            if (verificado) {
                ctx.sessionAttribute("loginSuccess",
                    "Email verificado com sucesso! Agora você pode fazer login.");
                logger.info("Email verificado com sucesso via token");
            } else {
                ctx.sessionAttribute("loginError",
                    "Token de verificação inválido ou expirado. Solicite um novo email de confirmação.");
            }

            ctx.redirect("/auth/login");

        } catch (Exception e) {
            logger.error("Erro ao verificar email", e);
            ctx.sessionAttribute("loginError", "Erro ao verificar email");
            ctx.redirect("/auth/login");
        }
    }

    /**
     * GET /auth/nova-senha
     * Exibe o formulário para definir nova senha.
     */
    public void showNovaSenha(Context ctx) {
        String token = ctx.queryParam("token");

        if (ValidationUtil.isNullOrEmpty(token)) {
            ctx.sessionAttribute("loginError", "Token inválido");
            ctx.redirect("/auth/login");
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Nova Senha - Notisblokk");
        model.put("theme", SessionUtil.getTheme(ctx));
        model.put("token", token);

        // Recuperar mensagens se houver
        String error = ctx.sessionAttribute("novaSenhaError");
        if (error != null) {
            model.put("error", error);
            ctx.sessionAttribute("novaSenhaError", null);
        }

        ctx.contentType("text/html; charset=utf-8");
        ctx.render("auth/nova-senha", model);
    }

    /**
     * POST /auth/nova-senha
     * Processa a definição de nova senha.
     */
    public void processNovaSenha(Context ctx) {
        try {
            String token = ctx.formParam("token");
            String password = ctx.formParam("password");
            String confirmPassword = ctx.formParam("confirmPassword");

            // Validar campos
            if (ValidationUtil.isNullOrEmpty(token) ||
                ValidationUtil.isNullOrEmpty(password) ||
                ValidationUtil.isNullOrEmpty(confirmPassword)) {

                ctx.sessionAttribute("novaSenhaError", "Por favor, preencha todos os campos");
                ctx.redirect("/auth/nova-senha?token=" + token);
                return;
            }

            // Validar confirmação de senha
            if (!ValidationUtil.areEqual(password, confirmPassword)) {
                ctx.sessionAttribute("novaSenhaError", "As senhas não coincidem");
                ctx.redirect("/auth/nova-senha?token=" + token);
                return;
            }

            // Validar token e obter userId
            Long userId = securityService.validarTokenRecuperacao(token);

            if (userId == null) {
                ctx.sessionAttribute("loginError", "Token inválido ou expirado");
                ctx.redirect("/auth/login");
                return;
            }

            // Atualizar senha
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            userService.atualizarSenha(userId, passwordHash);

            // Limpar token
            securityService.limparToken(userId);

            logger.info("Senha redefinida com sucesso para usuário ID: {}", userId);

            ctx.sessionAttribute("loginSuccess", "Senha redefinida com sucesso! Faça login com sua nova senha.");
            ctx.redirect("/auth/login");

        } catch (Exception e) {
            logger.error("Erro ao processar nova senha", e);
            ctx.sessionAttribute("novaSenhaError", "Erro ao redefinir senha. Tente novamente.");

            String token = ctx.formParam("token");
            if (token != null) {
                ctx.redirect("/auth/nova-senha?token=" + token);
            } else {
                ctx.redirect("/auth/login");
            }
        }
    }
}
