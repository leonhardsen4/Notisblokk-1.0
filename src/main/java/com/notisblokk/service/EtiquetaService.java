package com.notisblokk.service;

import com.notisblokk.model.Etiqueta;
import com.notisblokk.repository.EtiquetaRepository;
import com.notisblokk.util.SimpleCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável pela lógica de negócio relacionada a etiquetas.
 *
 * <p>Coordena operações entre controllers e repositories, implementando
 * regras de negócio e cache para gerenciamento de etiquetas.</p>
 *
 * <p><b>CACHE:</b> Implementa cache em memória com TTL de 5 minutos para
 * otimizar consultas frequentes de etiquetas.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-12-05
 */
public class EtiquetaService {

    private static final Logger logger = LoggerFactory.getLogger(EtiquetaService.class);
    private static final int CACHE_TTL_MINUTOS = 5;

    private final EtiquetaRepository etiquetaRepository;
    private final SimpleCache<String, List<Etiqueta>> cacheListaCompleta;
    private final SimpleCache<Long, Etiqueta> cachePorId;
    private final SimpleCache<String, Etiqueta> cachePorNome;

    /**
     * Construtor padrão com inicialização de cache.
     */
    public EtiquetaService() {
        this.etiquetaRepository = new EtiquetaRepository();
        this.cacheListaCompleta = new SimpleCache<>("EtiquetasLista", CACHE_TTL_MINUTOS);
        this.cachePorId = new SimpleCache<>("EtiquetasPorId", CACHE_TTL_MINUTOS);
        this.cachePorNome = new SimpleCache<>("EtiquetasPorNome", CACHE_TTL_MINUTOS);
    }

    /**
     * Construtor com injeção de dependência (para testes).
     *
     * @param etiquetaRepository repositório de etiquetas
     */
    public EtiquetaService(EtiquetaRepository etiquetaRepository) {
        this.etiquetaRepository = etiquetaRepository;
        this.cacheListaCompleta = new SimpleCache<>("EtiquetasLista", CACHE_TTL_MINUTOS);
        this.cachePorId = new SimpleCache<>("EtiquetasPorId", CACHE_TTL_MINUTOS);
        this.cachePorNome = new SimpleCache<>("EtiquetasPorNome", CACHE_TTL_MINUTOS);
    }

    /**
     * Lista todas as etiquetas (com cache).
     *
     * @return List<Etiqueta> lista de todas as etiquetas
     * @throws Exception se houver erro ao listar
     */
    public List<Etiqueta> listarTodas() throws Exception {
        // Tentar buscar do cache primeiro
        List<Etiqueta> cached = cacheListaCompleta.get("all");
        if (cached != null) {
            logger.debug("Retornando {} etiquetas do cache", cached.size());
            return cached;
        }

        // Cache miss - buscar do banco
        try {
            List<Etiqueta> etiquetas = etiquetaRepository.buscarTodos();

            // Armazenar no cache
            cacheListaCompleta.put("all", etiquetas);

            // Também popular cache individual
            for (Etiqueta etiqueta : etiquetas) {
                cachePorId.put(etiqueta.getId(), etiqueta);
                cachePorNome.put(etiqueta.getNome().toLowerCase(), etiqueta);
            }

            logger.debug("Carregadas {} etiquetas do banco e armazenadas no cache", etiquetas.size());
            return etiquetas;

        } catch (SQLException e) {
            logger.error("Erro ao listar etiquetas", e);
            throw new Exception("Erro ao listar etiquetas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca uma etiqueta por ID (com cache).
     *
     * @param id ID da etiqueta
     * @return Optional<Etiqueta> etiqueta encontrada ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<Etiqueta> buscarPorId(Long id) throws Exception {
        // Tentar buscar do cache primeiro
        Etiqueta cached = cachePorId.get(id);
        if (cached != null) {
            logger.debug("Etiqueta ID {} encontrada no cache", id);
            return Optional.of(cached);
        }

        // Cache miss - buscar do banco
        try {
            Optional<Etiqueta> etiquetaOpt = etiquetaRepository.buscarPorId(id);

            // Se encontrou, armazenar no cache
            etiquetaOpt.ifPresent(etiqueta -> {
                cachePorId.put(id, etiqueta);
                cachePorNome.put(etiqueta.getNome().toLowerCase(), etiqueta);
                logger.debug("Etiqueta ID {} carregada do banco e armazenada no cache", id);
            });

            return etiquetaOpt;

        } catch (SQLException e) {
            logger.error("Erro ao buscar etiqueta ID {}", id, e);
            throw new Exception("Erro ao buscar etiqueta: " + e.getMessage(), e);
        }
    }

    /**
     * Busca uma etiqueta por nome (com cache).
     *
     * @param nome nome da etiqueta
     * @return Optional<Etiqueta> etiqueta encontrada ou Optional.empty()
     * @throws Exception se houver erro ao buscar
     */
    public Optional<Etiqueta> buscarPorNome(String nome) throws Exception {
        String nomeKey = nome.toLowerCase();

        // Tentar buscar do cache primeiro
        Etiqueta cached = cachePorNome.get(nomeKey);
        if (cached != null) {
            logger.debug("Etiqueta '{}' encontrada no cache", nome);
            return Optional.of(cached);
        }

        // Cache miss - buscar do banco
        try {
            Optional<Etiqueta> etiquetaOpt = etiquetaRepository.buscarPorNome(nome);

            // Se encontrou, armazenar no cache
            etiquetaOpt.ifPresent(etiqueta -> {
                cachePorId.put(etiqueta.getId(), etiqueta);
                cachePorNome.put(nomeKey, etiqueta);
                logger.debug("Etiqueta '{}' carregada do banco e armazenada no cache", nome);
            });

            return etiquetaOpt;

        } catch (SQLException e) {
            logger.error("Erro ao buscar etiqueta por nome '{}'", nome, e);
            throw new Exception("Erro ao buscar etiqueta: " + e.getMessage(), e);
        }
    }

    /**
     * Conta notas por etiqueta.
     *
     * @param etiquetaId ID da etiqueta
     * @return long quantidade de notas
     * @throws Exception se houver erro ao contar
     */
    public long contarNotasPorEtiqueta(Long etiquetaId) throws Exception {
        try {
            return etiquetaRepository.contarNotasPorEtiqueta(etiquetaId);
        } catch (SQLException e) {
            logger.error("Erro ao contar notas da etiqueta ID {}", etiquetaId, e);
            throw new Exception("Erro ao contar notas: " + e.getMessage(), e);
        }
    }

    /**
     * Cria uma nova etiqueta e invalida o cache.
     *
     * @param nome nome da etiqueta
     * @param sessaoId ID da sessão atual
     * @param usuarioId ID do usuário atual
     * @return Etiqueta etiqueta criada
     * @throws Exception se houver erro ao criar
     */
    public Etiqueta criar(String nome, Long sessaoId, Long usuarioId) throws Exception {
        // Validações
        if (nome == null || nome.trim().isEmpty()) {
            throw new Exception("Nome da etiqueta é obrigatório");
        }

        if (nome.trim().length() > 100) {
            throw new Exception("Nome deve ter no máximo 100 caracteres");
        }

        // Verificar se já existe
        if (buscarPorNome(nome).isPresent()) {
            throw new Exception("Já existe uma etiqueta com este nome");
        }

        try {
            Etiqueta etiqueta = new Etiqueta();
            etiqueta.setNome(nome.trim());

            etiqueta = etiquetaRepository.salvar(etiqueta, sessaoId, usuarioId);

            // Invalidar cache
            invalidarCache();

            logger.info("Etiqueta criada: {} (ID: {})", nome, etiqueta.getId());
            return etiqueta;

        } catch (SQLException e) {
            logger.error("Erro ao criar etiqueta: {}", nome, e);
            throw new Exception("Erro ao criar etiqueta: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza uma etiqueta existente e invalida o cache.
     *
     * @param id ID da etiqueta
     * @param nome novo nome
     * @return Etiqueta etiqueta atualizada
     * @throws Exception se houver erro ao atualizar
     */
    public Etiqueta atualizar(Long id, String nome) throws Exception {
        // Validações
        if (nome == null || nome.trim().isEmpty()) {
            throw new Exception("Nome da etiqueta é obrigatório");
        }

        if (nome.trim().length() > 100) {
            throw new Exception("Nome deve ter no máximo 100 caracteres");
        }

        try {
            // Buscar etiqueta existente
            Etiqueta etiqueta = etiquetaRepository.buscarPorId(id)
                .orElseThrow(() -> new Exception("Etiqueta não encontrada"));

            // Verificar se o novo nome já existe em outra etiqueta
            Optional<Etiqueta> existente = buscarPorNome(nome);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new Exception("Já existe outra etiqueta com este nome");
            }

            // Atualizar
            etiqueta.setNome(nome.trim());
            etiquetaRepository.atualizar(etiqueta);

            // Invalidar cache
            invalidarCache();

            logger.info("Etiqueta ID {} atualizada para: {}", id, nome);
            return etiqueta;

        } catch (SQLException e) {
            logger.error("Erro ao atualizar etiqueta ID {}", id, e);
            throw new Exception("Erro ao atualizar etiqueta: " + e.getMessage(), e);
        }
    }

    /**
     * Deleta uma etiqueta e invalida o cache.
     *
     * @param id ID da etiqueta
     * @return long quantidade de notas deletadas em cascata
     * @throws Exception se houver erro ao deletar
     */
    public long deletar(Long id) throws Exception {
        logger.warn("Deletando etiqueta ID {}", id);

        try {
            // Verificar se existe
            if (etiquetaRepository.buscarPorId(id).isEmpty()) {
                throw new Exception("Etiqueta não encontrada");
            }

            // Contar notas que serão deletadas
            long totalNotas = contarNotasPorEtiqueta(id);

            // Deletar
            etiquetaRepository.deletar(id);

            // Invalidar cache
            invalidarCache();

            logger.info("Etiqueta ID {} deletada (cascata: {} notas)", id, totalNotas);
            return totalNotas;

        } catch (SQLException e) {
            logger.error("Erro ao deletar etiqueta ID {}", id, e);
            throw new Exception("Erro ao deletar etiqueta: " + e.getMessage(), e);
        }
    }

    /**
     * Invalida todo o cache de etiquetas.
     */
    private void invalidarCache() {
        cacheListaCompleta.clear();
        cachePorId.clear();
        cachePorNome.clear();
        logger.debug("Cache de etiquetas invalidado");
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
