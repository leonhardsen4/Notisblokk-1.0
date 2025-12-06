package com.notisblokk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache genérico em memória com suporte a TTL (Time To Live).
 *
 * <p>Implementa um cache simples e thread-safe usando ConcurrentHashMap.
 * Cada entrada tem um tempo de expiração (TTL) configurável.</p>
 *
 * <p><b>Thread-Safety:</b> Utiliza ConcurrentHashMap para garantir acesso seguro
 * em ambientes multi-thread.</p>
 *
 * @param <K> tipo da chave
 * @param <V> tipo do valor
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-12-05
 */
public class SimpleCache<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleCache.class);

    private final Map<K, CacheEntry<V>> cache;
    private final long ttlMillis;
    private final String cacheName;

    /**
     * Construtor do cache.
     *
     * @param cacheName nome do cache (para logs)
     * @param ttlMinutos tempo de vida das entradas em minutos
     */
    public SimpleCache(String cacheName, int ttlMinutos) {
        this.cacheName = cacheName;
        this.ttlMillis = ttlMinutos * 60L * 1000L;
        this.cache = new ConcurrentHashMap<>();
        logger.info("Cache '{}' inicializado com TTL de {} minutos", cacheName, ttlMinutos);
    }

    /**
     * Adiciona ou atualiza um valor no cache.
     *
     * @param key chave
     * @param value valor
     */
    public void put(K key, V value) {
        long expirationTime = System.currentTimeMillis() + ttlMillis;
        cache.put(key, new CacheEntry<>(value, expirationTime));
        logger.debug("[{}] Cache PUT: key={}", cacheName, key);
    }

    /**
     * Busca um valor no cache.
     *
     * @param key chave
     * @return valor ou null se não existir ou estiver expirado
     */
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);

        if (entry == null) {
            logger.debug("[{}] Cache MISS: key={}", cacheName, key);
            return null;
        }

        // Verificar se expirou
        if (System.currentTimeMillis() > entry.expirationTime) {
            logger.debug("[{}] Cache EXPIRED: key={}", cacheName, key);
            cache.remove(key);
            return null;
        }

        logger.debug("[{}] Cache HIT: key={}", cacheName, key);
        return entry.value;
    }

    /**
     * Remove um valor do cache.
     *
     * @param key chave
     */
    public void remove(K key) {
        cache.remove(key);
        logger.debug("[{}] Cache REMOVE: key={}", cacheName, key);
    }

    /**
     * Limpa todo o cache.
     */
    public void clear() {
        cache.clear();
        logger.info("[{}] Cache CLEARED", cacheName);
    }

    /**
     * Retorna o tamanho atual do cache (incluindo entradas expiradas).
     *
     * @return quantidade de entradas
     */
    public int size() {
        return cache.size();
    }

    /**
     * Remove entradas expiradas do cache.
     *
     * @return quantidade de entradas removidas
     */
    public int evictExpired() {
        long now = System.currentTimeMillis();
        int removed = 0;

        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            if (now > entry.getValue().expirationTime) {
                cache.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            logger.info("[{}] Evicted {} expired entries", cacheName, removed);
        }

        return removed;
    }

    /**
     * Verifica se o cache contém uma chave válida (não expirada).
     *
     * @param key chave
     * @return true se contém e não expirou
     */
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    /**
     * Classe interna para armazenar valor e tempo de expiração.
     *
     * @param <V> tipo do valor
     */
    private static class CacheEntry<V> {
        final V value;
        final long expirationTime;

        CacheEntry(V value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }
    }
}
