package com.notisblokk.audiencias.model;

import com.notisblokk.audiencias.model.enums.TipoParticipacao;

import java.time.LocalDateTime;

/**
 * Entidade que representa a participação de uma Pessoa em um Processo.
 *
 * Liga uma Pessoa a um Processo com um tipo específico de participação
 * (REU, VITIMA, TESTEMUNHA, etc.).
 *
 * Esta é a entidade de nível de Processo - os participantes do processo
 * podem ser selecionados para audiências específicas via AudienciaParticipante.
 */
public class ProcessoParticipante {

    private Long id;
    private Long processoId;
    private Long pessoaId;
    private TipoParticipacao tipoParticipacao;
    private String observacoes;

    // Relacionamentos (eager loaded)
    private Pessoa pessoa;

    // Auditoria
    private LocalDateTime criadoEm;

    // Construtores

    public ProcessoParticipante() {
    }

    public ProcessoParticipante(Long id, Long processoId, Long pessoaId,
                                TipoParticipacao tipoParticipacao, String observacoes) {
        this.id = id;
        this.processoId = processoId;
        this.pessoaId = pessoaId;
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

    public Long getPessoaId() {
        return pessoaId;
    }

    public void setPessoaId(Long pessoaId) {
        this.pessoaId = pessoaId;
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

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    @Override
    public String toString() {
        return "ProcessoParticipante{" +
                "id=" + id +
                ", processoId=" + processoId +
                ", pessoaId=" + pessoaId +
                ", tipoParticipacao=" + tipoParticipacao +
                ", pessoa=" + (pessoa != null ? pessoa.getNome() : "null") +
                '}';
    }
}
