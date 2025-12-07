package com.notisblokk.audiencias.model;

import com.notisblokk.audiencias.model.enums.StatusIntimacao;
import com.notisblokk.audiencias.model.enums.StatusOitiva;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade que representa a participação de uma pessoa do processo
 * em uma audiência específica.
 *
 * Controla status de intimação, status de oitiva (depoimento), e
 * informações sobre desistência ou oitivas anteriores.
 */
public class AudienciaParticipante {

    private Long id;
    private Long audienciaId;
    private Long processoParticipanteId;

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

    // Relacionamentos (eager loaded)
    private ProcessoParticipante processoParticipante;

    // Auditoria
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    // Construtores

    public AudienciaParticipante() {
        this.statusIntimacao = StatusIntimacao.NAO_INTIMADA;
        this.statusOitiva = StatusOitiva.AGUARDANDO;
        this.presente = false;
    }

    public AudienciaParticipante(Long id, Long audienciaId, Long processoParticipanteId,
                                 StatusIntimacao statusIntimacao, StatusOitiva statusOitiva) {
        this();
        this.id = id;
        this.audienciaId = audienciaId;
        this.processoParticipanteId = processoParticipanteId;
        this.statusIntimacao = statusIntimacao != null ? statusIntimacao : StatusIntimacao.NAO_INTIMADA;
        this.statusOitiva = statusOitiva != null ? statusOitiva : StatusOitiva.AGUARDANDO;
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

    public Long getProcessoParticipanteId() {
        return processoParticipanteId;
    }

    public void setProcessoParticipanteId(Long processoParticipanteId) {
        this.processoParticipanteId = processoParticipanteId;
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

    public ProcessoParticipante getProcessoParticipante() {
        return processoParticipante;
    }

    public void setProcessoParticipante(ProcessoParticipante processoParticipante) {
        this.processoParticipante = processoParticipante;
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
        return "AudienciaParticipante{" +
                "id=" + id +
                ", audienciaId=" + audienciaId +
                ", processoParticipanteId=" + processoParticipanteId +
                ", statusIntimacao=" + statusIntimacao +
                ", statusOitiva=" + statusOitiva +
                ", presente=" + presente +
                '}';
    }
}
