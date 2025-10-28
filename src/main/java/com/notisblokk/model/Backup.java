package com.notisblokk.model;

import java.time.LocalDateTime;

/**
 * Entidade que representa um backup realizado no sistema.
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class Backup {

    private Long id;
    private String caminhoArquivo;
    private String tipo; // AUTO, MANUAL, CSV
    private Long tamanhoBytes;
    private LocalDateTime dataBackup;
    private Long usuarioId; // null se automático

    /**
     * Construtor padrão.
     */
    public Backup() {
    }

    /**
     * Construtor completo.
     */
    public Backup(Long id, String caminhoArquivo, String tipo, Long tamanhoBytes,
                  LocalDateTime dataBackup, Long usuarioId) {
        this.id = id;
        this.caminhoArquivo = caminhoArquivo;
        this.tipo = tipo;
        this.tamanhoBytes = tamanhoBytes;
        this.dataBackup = dataBackup;
        this.usuarioId = usuarioId;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public LocalDateTime getDataBackup() {
        return dataBackup;
    }

    public void setDataBackup(LocalDateTime dataBackup) {
        this.dataBackup = dataBackup;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    @Override
    public String toString() {
        return "Backup{" +
                "id=" + id +
                ", caminhoArquivo='" + caminhoArquivo + '\'' +
                ", tipo='" + tipo + '\'' +
                ", tamanhoBytes=" + tamanhoBytes +
                ", dataBackup=" + dataBackup +
                ", usuarioId=" + usuarioId +
                '}';
    }
}
