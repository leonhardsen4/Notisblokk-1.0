package com.notisblokk.model;

import java.time.LocalDateTime;

/**
 * Entidade que representa o bloco de notas de um usuário.
 *
 * <p>Cada usuário possui um único documento de bloco de notas que pode
 * conter texto em Markdown. O sistema armazena tanto o conteúdo original
 * em Markdown quanto a versão renderizada em HTML.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class BlocoNota {

    private Long id;
    private Long usuarioId; // UNIQUE - um documento por usuário
    private String conteudoMarkdown; // Texto Markdown original
    private String conteudoHtml;     // HTML renderizado (cache)
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private Long sessaoId; // ID da sessão que criou/atualizou

    /**
     * Construtor padrão.
     */
    public BlocoNota() {
    }

    /**
     * Construtor completo.
     *
     * @param id identificador único do documento
     * @param usuarioId ID do usuário (único)
     * @param conteudoMarkdown conteúdo em formato Markdown
     * @param conteudoHtml conteúdo renderizado em HTML
     * @param dataCriacao data e hora de criação do documento
     * @param dataAtualizacao data e hora da última atualização
     * @param sessaoId ID da sessão que criou/atualizou
     */
    public BlocoNota(Long id, Long usuarioId, String conteudoMarkdown, String conteudoHtml,
                     LocalDateTime dataCriacao, LocalDateTime dataAtualizacao, Long sessaoId) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.conteudoMarkdown = conteudoMarkdown;
        this.conteudoHtml = conteudoHtml;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.sessaoId = sessaoId;
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

    public String getConteudoMarkdown() {
        return conteudoMarkdown;
    }

    public void setConteudoMarkdown(String conteudoMarkdown) {
        this.conteudoMarkdown = conteudoMarkdown;
    }

    public String getConteudoHtml() {
        return conteudoHtml;
    }

    public void setConteudoHtml(String conteudoHtml) {
        this.conteudoHtml = conteudoHtml;
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

    public Long getSessaoId() {
        return sessaoId;
    }

    public void setSessaoId(Long sessaoId) {
        this.sessaoId = sessaoId;
    }

    @Override
    public String toString() {
        return "BlocoNota{" +
                "id=" + id +
                ", usuarioId=" + usuarioId +
                ", conteudoMarkdown='" + (conteudoMarkdown != null ? conteudoMarkdown.substring(0, Math.min(50, conteudoMarkdown.length())) + "..." : "null") + '\'' +
                ", dataCriacao=" + dataCriacao +
                ", dataAtualizacao=" + dataAtualizacao +
                ", sessaoId=" + sessaoId +
                '}';
    }
}
