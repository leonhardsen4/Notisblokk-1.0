package com.notisblokk.model;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma configuração do sistema.
 *
 * <p>Configurações podem ser globais (usuario_id = null) ou específicas de usuário.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class Configuracao {

    private Long id;
    private Long usuarioId; // null = configuração global
    private String chave;
    private String valor;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    /**
     * Construtor padrão.
     */
    public Configuracao() {
    }

    /**
     * Construtor completo.
     */
    public Configuracao(Long id, Long usuarioId, String chave, String valor,
                       LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.chave = chave;
        this.valor = valor;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    @Override
    public String toString() {
        return "Configuracao{" +
                "id=" + id +
                ", usuarioId=" + usuarioId +
                ", chave='" + chave + '\'' +
                ", valor='" + valor + '\'' +
                ", dataCriacao=" + dataCriacao +
                ", dataAtualizacao=" + dataAtualizacao +
                '}';
    }
}
