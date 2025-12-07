package com.notisblokk.audiencias.dto;

import com.notisblokk.audiencias.model.enums.StatusIntimacao;
import com.notisblokk.audiencias.model.enums.StatusOitiva;

import java.time.LocalDate;

/**
 * DTO para retornar dados de participantes de uma audiência.
 *
 * Inclui dados do participante do processo (pessoa + tipo de participação)
 * e status específicos da audiência (intimação e oitiva).
 */
public class AudienciaParticipanteDTO {

    private Long id;
    private Long audienciaId;
    private ProcessoParticipanteDTO processoParticipante;  // Inclui Pessoa e tipo

    // Status de controle
    private StatusIntimacao statusIntimacao;
    private StatusOitiva statusOitiva;

    // Informações sobre oitiva
    private String observacoesDesistencia;  // Ex: "Desistência da Defesa", "Desistência do MP"
    private LocalDate dataOitivaAnterior;   // Quando foi ouvida anteriormente
    private String observacoesOitiva;       // Detalhes sobre oitiva anterior

    // Presença e observações
    private Boolean presente;
    private String observacoes;

    // Construtores

    public AudienciaParticipanteDTO() {
    }

    public AudienciaParticipanteDTO(Long id, Long audienciaId, ProcessoParticipanteDTO processoParticipante,
                                     StatusIntimacao statusIntimacao, StatusOitiva statusOitiva) {
        this.id = id;
        this.audienciaId = audienciaId;
        this.processoParticipante = processoParticipante;
        this.statusIntimacao = statusIntimacao;
        this.statusOitiva = statusOitiva;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAudienciaId() {
        return audienciaId;
    }

    public void setAudienciaId(Long audienciaId) {
        this.audienciaId = audienciaId;
    }

    public ProcessoParticipanteDTO getProcessoParticipante() {
        return processoParticipante;
    }

    public void setProcessoParticipante(ProcessoParticipanteDTO processoParticipante) {
        this.processoParticipante = processoParticipante;
    }

    public StatusIntimacao getStatusIntimacao() {
        return statusIntimacao;
    }

    public void setStatusIntimacao(StatusIntimacao statusIntimacao) {
        this.statusIntimacao = statusIntimacao;
    }

    public StatusOitiva getStatusOitiva() {
        return statusOitiva;
    }

    public void setStatusOitiva(StatusOitiva statusOitiva) {
        this.statusOitiva = statusOitiva;
    }

    public String getObservacoesDesistencia() {
        return observacoesDesistencia;
    }

    public void setObservacoesDesistencia(String observacoesDesistencia) {
        this.observacoesDesistencia = observacoesDesistencia;
    }

    public LocalDate getDataOitivaAnterior() {
        return dataOitivaAnterior;
    }

    public void setDataOitivaAnterior(LocalDate dataOitivaAnterior) {
        this.dataOitivaAnterior = dataOitivaAnterior;
    }

    public String getObservacoesOitiva() {
        return observacoesOitiva;
    }

    public void setObservacoesOitiva(String observacoesOitiva) {
        this.observacoesOitiva = observacoesOitiva;
    }

    public Boolean getPresente() {
        return presente;
    }

    public void setPresente(Boolean presente) {
        this.presente = presente;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    @Override
    public String toString() {
        return "AudienciaParticipanteDTO{" +
                "id=" + id +
                ", audienciaId=" + audienciaId +
                ", statusIntimacao=" + statusIntimacao +
                ", statusOitiva=" + statusOitiva +
                ", presente=" + presente +
                '}';
    }
}
