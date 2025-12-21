package com.notisblokk.service;

import com.notisblokk.model.Session;
import com.notisblokk.model.User;
import com.notisblokk.repository.SessionRepository;
import com.notisblokk.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável pela lógica de negócio relacionada a sessões.
 *
 * <p>Coordena operações entre controllers e repository, implementando
 * regras de negócio para gerenciamento de sessões de usuário.</p>
 *
 * <p><b>Funcionalidades:</b></p>
 * <ul>
 *   <li>Criar nova sessão (login)</li>
 *   <li>Encerrar sessão (logout)</li>
 *   <li>Listar sessões ativas</li>
 *   <li>Expirar sessões antigas</li>
 *   <li>Buscar histórico de sessões</li>
 * </ul>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-24
 */
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private final SessionRepository sessionRepository;

    /**
     * Timeout padrão para expiração de sessões (em minutos).
     */
    private static final int DEFAULT_SESSION_TIMEOUT = 30;

    /**
     * Limite máximo de sessões simultâneas por usuário.
     */
    private static final int MAX_SESSIONS_PER_USER = 3;

    /**
     * Construtor padrão.
     */
    public SessionService() {
        this.sessionRepository = new SessionRepository();
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param sessionRepository repositório de sessões
     */
    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Cria uma nova sessão para um usuário.
     *
     * @param userId ID do usuário
     * @param ipAddress endereço IP do cliente
     * @param userAgent user agent do navegador
     * @return Session sessão criada
     * @throws Exception se houver erro ao criar sessão
     */
    public Session criarSessao(Long userId, String ipAddress, String userAgent) throws Exception {
        logger.info("Criando nova sessão para usuário ID {}", userId);

        try {
            Session session = new Session();
            session.setUserId(userId);
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);

            session = sessionRepository.salvar(session);

            logger.info("Sessão criada com sucesso: ID {}", session.getId());
            return session;

        } catch (SQLException e) {
            logger.error("Erro ao criar sessão para usuário ID {}", userId, e);
            throw new Exception("Erro ao criar sessão: " + e.getMessage(), e);
        }
    }

    /**
     * Encerra uma sessão (logout).
     *
     * @param sessionId ID da sessão a ser encerrada
     * @throws Exception se houver erro ao encerrar sessão
     */
    public void encerrarSessao(Long sessionId) throws Exception {
        logger.info("Encerrando sessão ID {}", sessionId);

        try {
            sessionRepository.encerrar(sessionId);
            logger.info("Sessão ID {} encerrada com sucesso", sessionId);

        } catch (SQLException e) {
            logger.error("Erro ao encerrar sessão ID {}", sessionId, e);
            throw new Exception("Erro ao encerrar sessão: " + e.getMessage(), e);
        }
    }

    /**
     * Encerra todas as sessões ativas de um usuário.
     *
     * @param userId ID do usuário
     * @return int número de sessões encerradas
     * @throws Exception se houver erro ao encerrar sessões
     */
    public int encerrarTodasSessoesDoUsuario(Long userId) throws Exception {
        logger.info("Encerrando todas as sessões do usuário ID {}", userId);

        try {
            int count = sessionRepository.encerrarTodasDoUsuario(userId);
            logger.info("Encerradas {} sessões do usuário ID {}", count, userId);
            return count;

        } catch (SQLException e) {
            logger.error("Erro ao encerrar sessões do usuário ID {}", userId, e);
            throw new Exception("Erro ao encerrar sessões: " + e.getMessage(), e);
        }
    }

    /**
     * Busca uma sessão por ID.
     *
     * @param id ID da sessão
     * @return Optional<Session> sessão encontrada ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<Session> buscarPorId(Long id) throws Exception {
        try {
            return sessionRepository.buscarPorId(id);

        } catch (SQLException e) {
            logger.error("Erro ao buscar sessão ID {}", id, e);
            throw new Exception("Erro ao buscar sessão: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todas as sessões de um usuário.
     *
     * @param userId ID do usuário
     * @return List<Session> lista de sessões do usuário
     * @throws Exception se houver erro ao listar
     */
    public List<Session> listarSessoesDoUsuario(Long userId) throws Exception {
        try {
            return sessionRepository.buscarPorUsuario(userId);

        } catch (SQLException e) {
            logger.error("Erro ao listar sessões do usuário ID {}", userId, e);
            throw new Exception("Erro ao listar sessões: " + e.getMessage(), e);
        }
    }

    /**
     * Lista todas as sessões ativas do sistema.
     *
     * @return List<Session> lista de sessões ativas
     * @throws Exception se houver erro ao listar
     */
    public List<Session> listarSessoesAtivas() throws Exception {
        try {
            return sessionRepository.buscarSessoesAtivas();

        } catch (SQLException e) {
            logger.error("Erro ao listar sessões ativas", e);
            throw new Exception("Erro ao listar sessões ativas: " + e.getMessage(), e);
        }
    }

    /**
     * Lista as últimas N sessões do sistema.
     *
     * @param limit número máximo de sessões
     * @return List<Session> lista das últimas sessões
     * @throws Exception se houver erro ao listar
     */
    public List<Session> listarUltimasSessoes(int limit) throws Exception {
        try {
            return sessionRepository.buscarUltimas(limit);

        } catch (SQLException e) {
            logger.error("Erro ao listar últimas sessões", e);
            throw new Exception("Erro ao listar últimas sessões: " + e.getMessage(), e);
        }
    }

    /**
     * Expira sessões antigas baseado no timeout configurado.
     *
     * <p>Usa o timeout padrão de 30 minutos.</p>
     *
     * @return int número de sessões expiradas
     * @throws Exception se houver erro ao expirar
     */
    public int expirarSessoesAntigas() throws Exception {
        return expirarSessoesAntigas(DEFAULT_SESSION_TIMEOUT);
    }

    /**
     * Expira sessões antigas baseado em um timeout customizado.
     *
     * @param timeoutMinutes timeout em minutos
     * @return int número de sessões expiradas
     * @throws Exception se houver erro ao expirar
     */
    public int expirarSessoesAntigas(int timeoutMinutes) throws Exception {
        logger.info("Expirando sessões antigas (timeout: {} min)", timeoutMinutes);

        try {
            int count = sessionRepository.expirarSessoes(timeoutMinutes);

            if (count > 0) {
                logger.info("Expiradas {} sessões antigas", count);
            }

            return count;

        } catch (SQLException e) {
            logger.error("Erro ao expirar sessões antigas", e);
            throw new Exception("Erro ao expirar sessões: " + e.getMessage(), e);
        }
    }

    /**
     * Conta o total de sessões no sistema.
     *
     * @return long total de sessões
     * @throws Exception se houver erro ao contar
     */
    public long contarTotalSessoes() throws Exception {
        try {
            return sessionRepository.contarTotal();

        } catch (SQLException e) {
            logger.error("Erro ao contar total de sessões", e);
            throw new Exception("Erro ao contar sessões: " + e.getMessage(), e);
        }
    }

    /**
     * Conta as sessões ativas no sistema.
     *
     * @return long total de sessões ativas
     * @throws Exception se houver erro ao contar
     */
    public long contarSessoesAtivas() throws Exception {
        try {
            return sessionRepository.contarAtivas();

        } catch (SQLException e) {
            logger.error("Erro ao contar sessões ativas", e);
            throw new Exception("Erro ao contar sessões ativas: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se um usuário tem sessões ativas.
     *
     * @param userId ID do usuário
     * @return boolean true se houver sessões ativas, false caso contrário
     * @throws Exception se houver erro ao verificar
     */
    public boolean temSessoesAtivas(Long userId) throws Exception {
        try {
            List<Session> sessions = sessionRepository.buscarSessoesAtivasPorUsuario(userId);
            return !sessions.isEmpty();

        } catch (SQLException e) {
            logger.error("Erro ao verificar sessões ativas do usuário ID {}", userId, e);
            throw new Exception("Erro ao verificar sessões: " + e.getMessage(), e);
        }
    }

    /**
     * Conta o número de sessões ativas de um usuário.
     *
     * @param userId ID do usuário
     * @return long número de sessões ativas
     * @throws Exception se houver erro ao contar
     */
    public long contarSessoesAtivasPorUsuario(Long userId) throws Exception {
        try {
            return sessionRepository.contarAtivasPorUsuario(userId);

        } catch (SQLException e) {
            logger.error("Erro ao contar sessões ativas do usuário ID {}", userId, e);
            throw new Exception("Erro ao contar sessões: " + e.getMessage(), e);
        }
    }

    /**
     * Valida e gerencia o limite de sessões simultâneas antes de criar nova sessão.
     * Se o limite for atingido, encerra a sessão mais antiga automaticamente.
     *
     * <p>Administradores não possuem limite de sessões simultâneas.</p>
     *
     * @param userId ID do usuário
     * @return boolean true se pode criar nova sessão, false caso contrário
     * @throws Exception se houver erro ao validar
     */
    public boolean validarLimiteDeSessoes(Long userId) throws Exception {
        try {
            // Verificar se o usuário é administrador
            // Administradores não têm limite de sessões
            UserRepository userRepository = new UserRepository();
            Optional<User> userOpt = userRepository.buscarPorId(userId);

            if (userOpt.isPresent() && userOpt.get().isAdmin()) {
                logger.info("Usuário ID {} é administrador - sem limite de sessões", userId);
                return true; // Administrador pode criar quantas sessões quiser
            }

            long sessoesAtivas = sessionRepository.contarAtivasPorUsuario(userId);

            if (sessoesAtivas >= MAX_SESSIONS_PER_USER) {
                logger.warn("Usuário ID {} atingiu o limite de {} sessões simultâneas", userId, MAX_SESSIONS_PER_USER);

                // Encerrar a sessão mais antiga automaticamente
                boolean encerrada = sessionRepository.encerrarSessaoMaisAntigaDoUsuario(userId);

                if (encerrada) {
                    logger.info("Sessão mais antiga do usuário ID {} encerrada automaticamente", userId);
                    return true;
                } else {
                    logger.error("Falha ao encerrar sessão antiga do usuário ID {}", userId);
                    return false;
                }
            }

            return true; // Pode criar nova sessão

        } catch (SQLException e) {
            logger.error("Erro ao validar limite de sessões para usuário ID {}", userId, e);
            throw new Exception("Erro ao validar limite de sessões: " + e.getMessage(), e);
        }
    }

    /**
     * Cria uma nova sessão verificando o limite de sessões simultâneas.
     * Se o limite for atingido, encerra a sessão mais antiga antes de criar a nova.
     *
     * @param userId ID do usuário
     * @param ipAddress endereço IP do cliente
     * @param userAgent user agent do navegador
     * @return Session sessão criada
     * @throws Exception se houver erro ao criar sessão ou se limite não puder ser aplicado
     */
    public Session criarSessaoComLimite(Long userId, String ipAddress, String userAgent) throws Exception {
        logger.info("Criando nova sessão para usuário ID {} com validação de limite", userId);

        try {
            // Validar limite de sessões
            if (!validarLimiteDeSessoes(userId)) {
                throw new Exception("Limite de sessões simultâneas atingido e não foi possível encerrar sessão antiga");
            }

            // Criar a nova sessão
            Session session = new Session();
            session.setUserId(userId);
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);

            session = sessionRepository.salvar(session);

            logger.info("Sessão criada com sucesso: ID {}", session.getId());
            return session;

        } catch (SQLException e) {
            logger.error("Erro ao criar sessão para usuário ID {}", userId, e);
            throw new Exception("Erro ao criar sessão: " + e.getMessage(), e);
        }
    }

    /**
     * Retorna o limite máximo de sessões simultâneas por usuário.
     *
     * @return int limite de sessões
     */
    public int getMaxSessionsPerUser() {
        return MAX_SESSIONS_PER_USER;
    }

    /**
     * Retorna o timeout padrão de sessões em minutos.
     *
     * @return int timeout em minutos
     */
    public int getDefaultSessionTimeout() {
        return DEFAULT_SESSION_TIMEOUT;
    }
}
