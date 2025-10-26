package com.notisblokk.model;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma etiqueta (tag) para categorização de notas.
 *
 * <p>Etiquetas permitem organizar e filtrar notas por categorias
 * personalizadas pelo usuário.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class Etiqueta {

    private Long id;
    private String nome;
    private LocalDateTime dataCriacao;
    private Long sessaoId; // ID da sessão que criou a etiqueta
    private Long usuarioId; // ID do usuário que criou a etiqueta

    /**
     * Construtor padrão.
     */
    public Etiqueta() {
    }

    /**
     * Construtor completo.
     *
     * @param id identificador único da etiqueta
     * @param nome nome da etiqueta
     * @param dataCriacao data e hora de criação
     * @param sessaoId ID da sessão que criou
     * @param usuarioId ID do usuário que criou
     */
    public Etiqueta(Long id, String nome, LocalDateTime dataCriacao, Long sessaoId, Long usuarioId) {
        this.id = id;
        this.nome = nome;
        this.dataCriacao = dataCriacao;
        this.sessaoId = sessaoId;
        this.usuarioId = usuarioId;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Long getSessaoId() {
        return sessaoId;
    }

    public void setSessaoId(Long sessaoId) {
        this.sessaoId = sessaoId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    @Override
    public String toString() {
        return "Etiqueta{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", dataCriacao=" + dataCriacao +
                ", sessaoId=" + sessaoId +
                ", usuarioId=" + usuarioId +
                '}';
    }
}
