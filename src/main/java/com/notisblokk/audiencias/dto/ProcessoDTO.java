package com.notisblokk.audiencias.dto;

import com.notisblokk.audiencias.model.Vara;
import com.notisblokk.audiencias.model.enums.Competencia;
import com.notisblokk.audiencias.model.enums.StatusProcesso;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para retornar dados completos de um processo ao frontend.
 *
 * Inclui objetos relacionados (Vara, participantes, audiências).
 */
public class ProcessoDTO {

    private Long id;
    private String numeroProcesso;
    private Competencia competencia;
    private String artigo;
    private Vara vara;                              // Objeto completo
    private StatusProcesso status;
    private String observacoes;
    private List<ProcessoParticipanteDTO> participantes;  // Opcional
    private List<AudienciaResumoDTO> audiencias;          // Opcional
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    // Construtores

    public ProcessoDTO() {
    }

    public ProcessoDTO(Long id, String numeroProcesso, Competencia competencia, String artigo,
                       Vara vara, StatusProcesso status, String observacoes,
                       LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.numeroProcesso = numeroProcesso;
        this.competencia = competencia;
        this.artigo = artigo;
        this.vara = vara;
        this.status = status;
        this.observacoes = observacoes;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
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

    public List<ProcessoParticipanteDTO> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<ProcessoParticipanteDTO> participantes) {
        this.participantes = participantes;
    }

    public List<AudienciaResumoDTO> getAudiencias() {
        return audiencias;
    }

    public void setAudiencias(List<AudienciaResumoDTO> audiencias) {
        this.audiencias = audiencias;
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
        return "ProcessoDTO{" +
                "id=" + id +
                ", numeroProcesso='" + numeroProcesso + '\'' +
                ", competencia=" + competencia +
                ", vara=" + (vara != null ? vara.getNome() : "null") +
                ", status=" + status +
                '}';
    }
}
