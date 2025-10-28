package com.notisblokk.model;

import java.time.LocalDateTime;

/**
 * Entidade que representa um arquivo anexado a uma nota.
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class Anexo {

    private Long id;
    private Long notaId;
    private String nomeArquivo;
    private String caminhoArquivo;
    private String tipoMime;
    private Long tamanhoBytes;
    private LocalDateTime dataUpload;
    private Long usuarioId;

    /**
     * Construtor padrão.
     */
    public Anexo() {
    }

    /**
     * Construtor completo.
     */
    public Anexo(Long id, Long notaId, String nomeArquivo, String caminhoArquivo,
                 String tipoMime, Long tamanhoBytes, LocalDateTime dataUpload, Long usuarioId) {
        this.id = id;
        this.notaId = notaId;
        this.nomeArquivo = nomeArquivo;
        this.caminhoArquivo = caminhoArquivo;
        this.tipoMime = tipoMime;
        this.tamanhoBytes = tamanhoBytes;
        this.dataUpload = dataUpload;
        this.usuarioId = usuarioId;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNotaId() {
        return notaId;
    }

    public void setNotaId(Long notaId) {
        this.notaId = notaId;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public LocalDateTime getDataUpload() {
        return dataUpload;
    }

    public void setDataUpload(LocalDateTime dataUpload) {
        this.dataUpload = dataUpload;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    @Override
    public String toString() {
        return "Anexo{" +
                "id=" + id +
                ", notaId=" + notaId +
                ", nomeArquivo='" + nomeArquivo + '\'' +
                ", caminhoArquivo='" + caminhoArquivo + '\'' +
                ", tipoMime='" + tipoMime + '\'' +
                ", tamanhoBytes=" + tamanhoBytes +
                ", dataUpload=" + dataUpload +
                ", usuarioId=" + usuarioId +
                '}';
    }
}
