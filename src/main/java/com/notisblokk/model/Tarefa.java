package com.notisblokk.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade que representa uma tarefa no sistema.
 *
 * <p>Tarefas são o elemento central do sistema, contendo título, conteúdo,
 * prazo final, etiqueta e status.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class Tarefa {

    private Long id;
    private Long etiquetaId;
    private Long statusId;
    private String titulo;
    private String conteudo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private LocalDate prazoFinal;
    private Long sessaoId; // ID da sessão que criou a tarefa
    private Long usuarioId; // ID do usuário que criou a tarefa

    /**
     * Construtor padrão.
     */
    public Tarefa() {
    }

    /**
     * Construtor completo.
     *
     * @param id identificador único da tarefa
     * @param etiquetaId ID da etiqueta associada
     * @param statusId ID do status atual
     * @param titulo título da tarefa
     * @param conteudo conteúdo/descrição da tarefa
     * @param dataCriacao data e hora de criação
     * @param dataAtualizacao data e hora da última atualização
     * @param prazoFinal data limite para conclusão
     * @param sessaoId ID da sessão que criou
     * @param usuarioId ID do usuário que criou
     */
    public Tarefa(Long id, Long etiquetaId, Long statusId, String titulo, String conteudo,
                LocalDateTime dataCriacao, LocalDateTime dataAtualizacao, LocalDate prazoFinal,
                Long sessaoId, Long usuarioId) {
        this.id = id;
        this.etiquetaId = etiquetaId;
        this.statusId = statusId;
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.prazoFinal = prazoFinal;
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

    public Long getEtiquetaId() {
        return etiquetaId;
    }

    public void setEtiquetaId(Long etiquetaId) {
        this.etiquetaId = etiquetaId;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
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

    public LocalDate getPrazoFinal() {
        return prazoFinal;
    }

    public void setPrazoFinal(LocalDate prazoFinal) {
        this.prazoFinal = prazoFinal;
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
        return "Tarefa{" +
                "id=" + id +
                ", etiquetaId=" + etiquetaId +
                ", statusId=" + statusId +
                ", titulo='" + titulo + '\'' +
                ", conteudo='" + conteudo + '\'' +
                ", dataCriacao=" + dataCriacao +
                ", dataAtualizacao=" + dataAtualizacao +
                ", prazoFinal=" + prazoFinal +
                ", sessaoId=" + sessaoId +
                ", usuarioId=" + usuarioId +
                '}';
    }
}
