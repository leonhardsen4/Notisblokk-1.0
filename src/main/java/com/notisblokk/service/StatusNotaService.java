package com.notisblokk.service;

import com.notisblokk.model.StatusNota;
import com.notisblokk.repository.StatusNotaRepository;
import com.notisblokk.util.SimpleCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Serviço responsável pela lógica de negócio relacionada a status de notas.
 *
 * <p>Coordena operações entre controllers e repositories, implementando
 * regras de negócio e cache para gerenciamento de status de notas.</p>
 *
 * <p><b>CACHE:</b> Implementa cache em memória com TTL de 5 minutos para
 * otimizar consultas frequentes de status.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-12-05
 */
public class StatusNotaService {

    private static final Logger logger = LoggerFactory.getLogger(StatusNotaService.class);
    private static final int CACHE_TTL_MINUTOS = 5;
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([0-9a-fA-F]{6})$");

    private final StatusNotaRepository statusRepository;
    private final SimpleCache<String, List<StatusNota>> cacheListaCompleta;
    private final SimpleCache<Long, StatusNota> cachePorId;
    private final SimpleCache<String, StatusNota> cachePorNome;

    /**
     * Construtor padrão com inicialização de cache.
     */
    public StatusNotaService() {
        this.statusRepository = new StatusNotaRepository();
        this.cacheListaCompleta = new SimpleCache<>("StatusLista", CACHE_TTL_MINUTOS);
        this.cachePorId = new SimpleCache<>("StatusPorId", CACHE_TTL_MINUTOS);
        this.cachePorNome = new SimpleCache<>("StatusPorNome", CACHE_TTL_MINUTOS);
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param statusRepository repositório de status
     */
    public StatusNotaService(StatusNotaRepository statusRepository) {
        this.statusRepository = statusRepository;
        this.cacheListaCompleta = new SimpleCache<>("StatusLista", CACHE_TTL_MINUTOS);
        this.cachePorId = new SimpleCache<>("StatusPorId", CACHE_TTL_MINUTOS);
        this.cachePorNome = new SimpleCache<>("StatusPorNome", CACHE_TTL_MINUTOS);
    }

    /**
     * Lista todos os status (com cache).
     *
     * @return List<StatusNota> lista de todos os status
     * @throws Exception se houver erro ao listar
     */
    public List<StatusNota> listarTodos() throws Exception {
        // Tentar buscar do cache primeiro
        List<StatusNota> cached = cacheListaCompleta.get("all");
        if (cached != null) {
            logger.debug("Retornando {} status do cache", cached.size());
            return cached;
        }

        // Cache miss - buscar do banco
        try {
            List<StatusNota> statusList = statusRepository.buscarTodos();

            // Armazenar no cache
            cacheListaCompleta.put("all", statusList);

            // Também popular cache individual
            for (StatusNota status : statusList) {
                cachePorId.put(status.getId(), status);
                cachePorNome.put(status.getNome().toLowerCase(), status);
            }

            logger.debug("Carregados {} status do banco e armazenados no cache", statusList.size());
            return statusList;

        } catch (SQLException e) {
            logger.error("Erro ao listar status", e);
            throw new Exception("Erro ao listar status: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um status por ID (com cache).
     *
     * @param id ID do status
     * @return Optional<StatusNota> status encontrado ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<StatusNota> buscarPorId(Long id) throws Exception {
        // Tentar buscar do cache primeiro
        StatusNota cached = cachePorId.get(id);
        if (cached != null) {
            logger.debug("Status ID {} encontrado no cache", id);
            return Optional.of(cached);
        }

        // Cache miss - buscar do banco
        try {
            Optional<StatusNota> statusOpt = statusRepository.buscarPorId(id);

            // Se encontrou, armazenar no cache
            statusOpt.ifPresent(status -> {
                cachePorId.put(id, status);
                cachePorNome.put(status.getNome().toLowerCase(), status);
                logger.debug("Status ID {} carregado do banco e armazenado no cache", id);
            });

            return statusOpt;

        } catch (SQLException e) {
            logger.error("Erro ao buscar status ID {}", id, e);
            throw new Exception("Erro ao buscar status: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um status por nome (com cache).
     *
     * @param nome nome do status
     * @return Optional<StatusNota> status encontrado ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<StatusNota> buscarPorNome(String nome) throws Exception {
        String nomeKey = nome.toLowerCase();

        // Tentar buscar do cache primeiro
        StatusNota cached = cachePorNome.get(nomeKey);
        if (cached != null) {
            logger.debug("Status '{}' encontrado no cache", nome);
            return Optional.of(cached);
        }

        // Cache miss - buscar do banco
        try {
            Optional<StatusNota> statusOpt = statusRepository.buscarPorNome(nome);

            // Se encontrou, armazenar no cache
            statusOpt.ifPresent(status -> {
                cachePorId.put(status.getId(), status);
                cachePorNome.put(nomeKey, status);
                logger.debug("Status '{}' carregado do banco e armazenado no cache", nome);
            });

            return statusOpt;

        } catch (SQLException e) {
            logger.error("Erro ao buscar status por nome '{}'", nome, e);
            throw new Exception("Erro ao buscar status: " + e.getMessage(), e);
        }
    }

    /**
     * Conta notas por status.
     *
     * @param statusId ID do status
     * @return long quantidade de notas
     * @throws Exception se houver erro ao contar
     */
    public long contarNotasPorStatus(Long statusId) throws Exception {
        try {
            return statusRepository.contarNotasPorStatus(statusId);
        } catch (SQLException e) {
            logger.error("Erro ao contar notas do status ID {}", statusId, e);
            throw new Exception("Erro ao contar notas: " + e.getMessage(), e);
        }
    }

    /**
     * Cria um novo status e invalida o cache.
     *
     * @param nome nome do status
     * @param corHex cor em formato hexadecimal (#RRGGBB)
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return StatusNota status criado
     * @throws Exception se houver erro ao criar
     */
    public StatusNota criar(String nome, String corHex, Long sessaoId, Long usuarioId) throws Exception {
        // Validações
        if (nome == null || nome.trim().isEmpty()) {
            throw new Exception("Nome do status é obrigatório");
        }

        if (nome.trim().length() > 100) {
            throw new Exception("Nome deve ter no máximo 100 caracteres");
        }

        if (corHex == null || !HEX_COLOR_PATTERN.matcher(corHex).matches()) {
            throw new Exception("Cor em formato hexadecimal (#RRGGBB) é obrigatória");
        }

        // Verificar se já existe
        if (buscarPorNome(nome).isPresent()) {
            throw new Exception("Já existe um status com este nome");
        }

        try {
            StatusNota status = new StatusNota();
            status.setNome(nome.trim());
            status.setCorHex(corHex.toUpperCase());

            status = statusRepository.salvar(status, sessaoId, usuarioId);

            // Invalidar cache
            invalidarCache();

            logger.info("Status criado: {} (ID: {})", nome, status.getId());
            return status;

        } catch (SQLException e) {
            logger.error("Erro ao criar status: {}", nome, e);
            throw new Exception("Erro ao criar status: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza um status existente e invalida o cache.
     *
     * @param id ID do status
     * @param nome novo nome
     * @param corHex nova cor
     * @return StatusNota status atualizado
     * @throws Exception se houver erro ao atualizar
     */
    public StatusNota atualizar(Long id, String nome, String corHex) throws Exception {
        // Validações
        if (nome == null || nome.trim().isEmpty()) {
            throw new Exception("Nome do status é obrigatório");
        }

        if (nome.trim().length() > 100) {
            throw new Exception("Nome deve ter no máximo 100 caracteres");
        }

        if (corHex == null || !HEX_COLOR_PATTERN.matcher(corHex).matches()) {
            throw new Exception("Cor em formato hexadecimal (#RRGGBB) é obrigatória");
        }

        try {
            // Buscar status existente
            StatusNota status = statusRepository.buscarPorId(id)
                .orElseThrow(() -> new Exception("Status não encontrado"));

            // Verificar se o novo nome já existe em outro status
            Optional<StatusNota> existente = buscarPorNome(nome);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new Exception("Já existe outro status com este nome");
            }

            // Atualizar
            status.setNome(nome.trim());
            status.setCorHex(corHex.toUpperCase());
            statusRepository.atualizar(status);

            // Invalidar cache
            invalidarCache();

            logger.info("Status ID {} atualizado para: {}", id, nome);
            return status;

        } catch (SQLException e) {
            logger.error("Erro ao atualizar status ID {}", id, e);
            throw new Exception("Erro ao atualizar status: " + e.getMessage(), e);
        }
    }

    /**
     * Deleta um status e invalida o cache.
     * Verifica se há notas vinculadas antes de deletar.
     *
     * @param id ID do status
     * @throws Exception se houver erro ao deletar ou se há notas vinculadas
     */
    public void deletar(Long id) throws Exception {
        logger.warn("Deletando status ID {}", id);

        try {
            // Verificar se existe
            if (statusRepository.buscarPorId(id).isEmpty()) {
                throw new Exception("Status não encontrado");
            }

            // Verificar se há notas com esse status
            long totalNotas = contarNotasPorStatus(id);
            if (totalNotas > 0) {
                throw new Exception(String.format(
                    "Não é possível deletar este status pois há %d nota(s) vinculada(s)", totalNotas
                ));
            }

            // Deletar
            statusRepository.deletar(id);

            // Invalidar cache
            invalidarCache();

            logger.info("Status ID {} deletado com sucesso", id);

        } catch (SQLException e) {
            logger.error("Erro ao deletar status ID {}", id, e);
            throw new Exception("Erro ao deletar status: " + e.getMessage(), e);
        }
    }

    /**
     * Invalida todo o cache de status.
     */
    private void invalidarCache() {
        cacheListaCompleta.clear();
        cachePorId.clear();
        cachePorNome.clear();
        logger.debug("Cache de status invalidado");
    }

    /**
     * Limpa entradas expiradas do cache (manutenção).
     */
    public void limparCacheExpirado() {
        int removed = 0;
        removed += cacheListaCompleta.evictExpired();
        removed += cachePorId.evictExpired();
        removed += cachePorNome.evictExpired();

        if (removed > 0) {
            logger.info("Limpeza de cache: {} entradas expiradas removidas", removed);
        }
    }
}
