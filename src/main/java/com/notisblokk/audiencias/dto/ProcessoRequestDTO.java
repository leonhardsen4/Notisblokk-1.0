package com.notisblokk.audiencias.dto;

/**
 * DTO para receber dados de processo do frontend.
 *
 * Usado em operações de criação e atualização de processos.
 */
public class ProcessoRequestDTO {

    private String numeroProcesso;
    private String competencia;    // Enum: CRIMINAL, VIOLENCIA_DOMESTICA, INFANCIA_JUVENTUDE
    private String artigo;
    private Long varaId;
    private String status;          // Enum: StatusProcesso
    private String observacoes;

    // Construtores

    public ProcessoRequestDTO() {
    }

    public ProcessoRequestDTO(String numeroProcesso, String competencia, String artigo,
                              Long varaId, String status, String observacoes) {
        this.numeroProcesso = numeroProcesso;
        this.competencia = competencia;
        this.artigo = artigo;
        this.varaId = varaId;
        this.status = status;
        this.observacoes = observacoes;
    }

    // Getters e Setters

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public String getCompetencia() {
        return competencia;
    }

    public void setCompetencia(String competencia) {
        this.competencia = competencia;
    }

    public String getArtigo() {
        return artigo;
    }

    public void setArtigo(String artigo) {
        this.artigo = artigo;
    }

    public Long getVaraId() {
        return varaId;
    }

    public void setVaraId(Long varaId) {
        this.varaId = varaId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    @Override
    public String toString() {
        return "ProcessoRequestDTO{" +
                "numeroProcesso='" + numeroProcesso + '\'' +
                ", competencia='" + competencia + '\'' +
                ", artigo='" + artigo + '\'' +
                ", varaId=" + varaId +
                ", status='" + status + '\'' +
                '}';
    }
}
