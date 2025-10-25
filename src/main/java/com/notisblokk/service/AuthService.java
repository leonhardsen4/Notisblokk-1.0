package com.notisblokk.service;

import com.notisblokk.model.Session;
import com.notisblokk.model.User;
import com.notisblokk.repository.UserRepository;
import com.notisblokk.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Serviço responsável pela autenticação de usuários.
 *
 * <p>Coordena os processos de login, logout e validação de credenciais,
 * integrando UserService e SessionService.</p>
 *
 * <p><b>Funcionalidades:</b></p>
 * <ul>
 *   <li>Autenticar usuário (login)</li>
 *   <li>Validar credenciais</li>
 *   <li>Criar sessão de usuário</li>
 *   <li>Encerrar sessão (logout)</li>
 *   <li>Verificar status de usuário (ativo/inativo)</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final SessionService sessionService;

    /**
     * Construtor padrão.
     */
    public AuthService() {
        this.userRepository = new UserRepository();
        this.sessionService = new SessionService();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param userRepository repositório de usuários
     * @param sessionService serviço de sessões
     */
    public AuthService(UserRepository userRepository, SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    /**
     * Autentica um usuário com base nas credenciais fornecidas.
     *
     * <p>Valida username/email e senha, verifica se o usuário está ativo,
     * e cria uma sessão no banco de dados.</p>
     *
     * @param usernameOrEmail username ou email do usuário
     * @param password senha em texto plano
     * @param ipAddress endereço IP do cliente
     * @param userAgent user agent do navegador
     * @return LoginResult resultado da autenticação com usuário e sessão
     * @throws Exception se houver erro na autenticação
     */
    public LoginResult autenticar(String usernameOrEmail, String password,
                                  String ipAddress, String userAgent) throws Exception {

        logger.info("Tentativa de login: {}", usernameOrEmail);

        // Validar campos obrigatórios
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            logger.warn("Tentativa de login com username/email vazio");
            throw new AuthenticationException("Username ou email não pode ser vazio");
        }

        if (password == null || password.isEmpty()) {
            logger.warn("Tentativa de login com senha vazia");
            throw new AuthenticationException("Senha não pode ser vazia");
        }

        try {
            // Buscar usuário por username ou email
            Optional<User> userOpt = userRepository.buscarPorUsernameOuEmail(usernameOrEmail.trim());

            if (userOpt.isEmpty()) {
                logger.warn("Tentativa de login com credenciais inválidas: {}", usernameOrEmail);
                throw new AuthenticationException("Credenciais inválidas");
            }

            User user = userOpt.get();

            // Verificar senha
            if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                logger.warn("Senha incorreta para usuário: {}", user.getUsername());
                throw new AuthenticationException("Credenciais inválidas");
            }

            // Verificar se usuário está ativo
            if (!user.isActive()) {
                logger.warn("Tentativa de login com usuário inativo: {}", user.getUsername());
                throw new AuthenticationException("Usuário inativo. Contate o administrador.");
            }

            // Criar sessão
            Session session = sessionService.criarSessao(user.getId(), ipAddress, userAgent);

            logger.info("Login bem-sucedido: {} (ID: {}, Role: {})",
                       user.getUsername(), user.getId(), user.getRole());

            return new LoginResult(user, session);

        } catch (AuthenticationException e) {
            throw e;
        } catch (SQLException e) {
            logger.error("Erro ao autenticar usuário: {}", usernameOrEmail, e);
            throw new Exception("Erro ao processar autenticação: " + e.getMessage(), e);
        }
    }

    /**
     * Encerra a sessão de um usuário (logout).
     *
     * @param sessionId ID da sessão a ser encerrada
     * @throws Exception se houver erro ao encerrar sessão
     */
    public void logout(Long sessionId) throws Exception {
        if (sessionId == null) {
            logger.warn("Tentativa de logout com sessionId nulo");
            return;
        }

        logger.info("Logout: sessão ID {}", sessionId);
        sessionService.encerrarSessao(sessionId);
        logger.info("Logout realizado com sucesso: sessão ID {}", sessionId);
    }

    /**
     * Verifica se um usuário existe e está ativo.
     *
     * @param usernameOrEmail username ou email do usuário
     * @return boolean true se existir e estiver ativo, false caso contrário
     * @throws Exception se houver erro ao verificar
     */
    public boolean isUsuarioAtivoExiste(String usernameOrEmail) throws Exception {
        try {
            Optional<User> userOpt = userRepository.buscarPorUsernameOuEmail(usernameOrEmail);
            return userOpt.isPresent() && userOpt.get().isActive();

        } catch (SQLException e) {
            logger.error("Erro ao verificar usuário: {}", usernameOrEmail, e);
            throw new Exception("Erro ao verificar usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um usuário por username ou email.
     *
     * @param usernameOrEmail username ou email do usuário
     * @return Optional<User> usuário encontrado ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<User> buscarUsuario(String usernameOrEmail) throws Exception {
        try {
            return userRepository.buscarPorUsernameOuEmail(usernameOrEmail);

        } catch (SQLException e) {
            logger.error("Erro ao buscar usuário: {}", usernameOrEmail, e);
            throw new Exception("Erro ao buscar usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Classe para representar resultado de login.
     */
    public static class LoginResult {
        private final User user;
        private final Session session;

        public LoginResult(User user, Session session) {
            this.user = user;
            this.session = session;
        }

        public User getUser() {
            return user;
        }

        public Session getSession() {
            return session;
        }
    }

    /**
     * Exceção customizada para erros de autenticação.
     */
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }

        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
