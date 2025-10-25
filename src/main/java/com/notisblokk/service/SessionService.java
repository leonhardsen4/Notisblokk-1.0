package com.notisblokk.service;

import com.notisblokk.model.Session;
import com.notisblokk.repository.SessionRepository;
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
}
