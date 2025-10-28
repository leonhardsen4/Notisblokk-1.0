package com.notisblokk.model;

import java.util.List;

/**
 * Resposta paginada genérica para APIs REST.
 *
 * @param <T> Tipo dos dados retornados
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class PaginatedResponse<T> {
    private List<T> dados;
    private int paginaAtual;
    private int tamanhoPagina;
    private long totalRegistros;
    private int totalPaginas;
    private boolean temProxima;
    private boolean temAnterior;

    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> dados, int paginaAtual, int tamanhoPagina, long totalRegistros) {
        this.dados = dados;
        this.paginaAtual = paginaAtual;
        this.tamanhoPagina = tamanhoPagina;
        this.totalRegistros = totalRegistros;
        this.totalPaginas = (int) Math.ceil((double) totalRegistros / tamanhoPagina);
        this.temProxima = paginaAtual < totalPaginas;
        this.temAnterior = paginaAtual > 1;
    }

    // Getters e Setters
    public List<T> getDados() {
        return dados;
    }

    public void setDados(List<T> dados) {
        this.dados = dados;
    }

    public int getPaginaAtual() {
        return paginaAtual;
    }

    public void setPaginaAtual(int paginaAtual) {
        this.paginaAtual = paginaAtual;
    }

    public int getTamanhoPagina() {
        return tamanhoPagina;
    }

    public void setTamanhoPagina(int tamanhoPagina) {
        this.tamanhoPagina = tamanhoPagina;
    }

    public long getTotalRegistros() {
        return totalRegistros;
    }

    public void setTotalRegistros(long totalRegistros) {
        this.totalRegistros = totalRegistros;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public void setTotalPaginas(int totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    public boolean isTemProxima() {
        return temProxima;
    }

    public void setTemProxima(boolean temProxima) {
        this.temProxima = temProxima;
    }

    public boolean isTemAnterior() {
        return temAnterior;
    }

    public void setTemAnterior(boolean temAnterior) {
        this.temAnterior = temAnterior;
    }
}
