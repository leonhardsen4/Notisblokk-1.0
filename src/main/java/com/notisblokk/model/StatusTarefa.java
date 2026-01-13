package com.notisblokk.model;

import java.time.LocalDateTime;

/**
 * Entidade que representa o status de uma tarefa.
 *
 * <p>Status permite acompanhar o estado atual de cada tarefa
 * (Pendente, Em Andamento, Resolvido, Suspenso, Cancelado, etc.)
 * com cores personalizadas para visualização.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class StatusTarefa {

    private Long id;
    private String nome;
    private String corHex;
    private LocalDateTime dataCriacao;
    private Long sessaoId; // ID da sessão que criou o status
    private Long usuarioId; // ID do usuário que criou o status

    /**
     * Construtor padrão.
     */
    public StatusTarefa() {
    }

    /**
     * Construtor completo.
     *
     * @param id identificador único do status
     * @param nome nome do status
     * @param corHex cor em formato hexadecimal (#RRGGBB)
     * @param dataCriacao data e hora de criação
     * @param sessaoId ID da sessão que criou
     * @param usuarioId ID do usuário que criou
     */
    public StatusTarefa(Long id, String nome, String corHex, LocalDateTime dataCriacao, Long sessaoId, Long usuarioId) {
        this.id = id;
        this.nome = nome;
        this.corHex = corHex;
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

    public String getCorHex() {
        return corHex;
    }

    public void setCorHex(String corHex) {
        this.corHex = corHex;
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
        return "StatusTarefa{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", corHex='" + corHex + '\'' +
                ", dataCriacao=" + dataCriacao +
                ", sessaoId=" + sessaoId +
                ", usuarioId=" + usuarioId +
                '}';
    }
}
