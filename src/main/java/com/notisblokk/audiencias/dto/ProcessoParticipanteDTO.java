package com.notisblokk.audiencias.dto;

import com.notisblokk.audiencias.model.Pessoa;
import com.notisblokk.audiencias.model.enums.TipoParticipacao;

/**
 * DTO para retornar dados de participantes de um processo.
 *
 * Inclui dados completos da pessoa.
 */
public class ProcessoParticipanteDTO {

    private Long id;
    private Long processoId;
    private Pessoa pessoa;              // Objeto completo
    private TipoParticipacao tipoParticipacao;
    private String observacoes;

    // Construtores

    public ProcessoParticipanteDTO() {
    }

    public ProcessoParticipanteDTO(Long id, Long processoId, Pessoa pessoa,
                                   TipoParticipacao tipoParticipacao, String observacoes) {
        this.id = id;
        this.processoId = processoId;
        this.pessoa = pessoa;
        this.tipoParticipacao = tipoParticipacao;
        this.observacoes = observacoes;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProcessoId() {
        return processoId;
    }

    public void setProcessoId(Long processoId) {
        this.processoId = processoId;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public TipoParticipacao getTipoParticipacao() {
        return tipoParticipacao;
    }

    public void setTipoParticipacao(TipoParticipacao tipoParticipacao) {
        this.tipoParticipacao = tipoParticipacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    @Override
    public String toString() {
        return "ProcessoParticipanteDTO{" +
                "id=" + id +
                ", processoId=" + processoId +
                ", pessoa=" + (pessoa != null ? pessoa.getNome() : "null") +
                ", tipoParticipacao=" + tipoParticipacao +
                '}';
    }
}
