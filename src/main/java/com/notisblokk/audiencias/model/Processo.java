package com.notisblokk.audiencias.model;

import com.notisblokk.audiencias.model.enums.Competencia;
import com.notisblokk.audiencias.model.enums.StatusProcesso;

import java.time.LocalDateTime;

/**
 * Entidade que representa um Processo Judicial.
 *
 * O Processo é a entidade central do sistema, agregando informações do caso judicial
 * e podendo ter múltiplas audiências vinculadas.
 *
 * Campos movidos de Audiencia: numeroProcesso, competencia, artigo, vara
 */
public class Processo {

    private Long id;
    private String numeroProcesso;
    private Competencia competencia;
    private String artigo;
    private Vara vara;
    private StatusProcesso status;
    private String observacoes;

    // Auditoria
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    // Construtores

    public Processo() {
        this.status = StatusProcesso.EM_ANDAMENTO;
    }

    public Processo(Long id, String numeroProcesso, Competencia competencia,
                    String artigo, Vara vara, StatusProcesso status, String observacoes) {
        this.id = id;
        this.numeroProcesso = numeroProcesso;
        this.competencia = competencia;
        this.artigo = artigo;
        this.vara = vara;
        this.status = status != null ? status : StatusProcesso.EM_ANDAMENTO;
        this.observacoes = observacoes;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public void setCompetencia(Competencia competencia) {
        this.competencia = competencia;
    }

    public String getArtigo() {
        return artigo;
    }

    public void setArtigo(String artigo) {
        this.artigo = artigo;
    }

    public Vara getVara() {
        return vara;
    }

    public void setVara(Vara vara) {
        this.vara = vara;
    }

    public StatusProcesso getStatus() {
        return status;
    }

    public void setStatus(StatusProcesso status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    @Override
    public String toString() {
        return "Processo{" +
                "id=" + id +
                ", numeroProcesso='" + numeroProcesso + '\'' +
                ", competencia=" + competencia +
                ", artigo='" + artigo + '\'' +
                ", vara=" + (vara != null ? vara.getNome() : "null") +
                ", status=" + status +
                '}';
    }
}
