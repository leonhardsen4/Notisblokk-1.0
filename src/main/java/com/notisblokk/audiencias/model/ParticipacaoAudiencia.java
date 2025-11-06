package com.notisblokk.audiencias.model;

import com.notisblokk.audiencias.model.enums.TipoParticipacao;

/**
 * Entidade que representa a participação de uma pessoa em uma audiência.
 *
 * Relaciona uma Pessoa com uma Audiência, indicando seu tipo de participação
 * (autor, réu, vítima, testemunha, etc.) e se foi intimada.
 */
public class ParticipacaoAudiencia {

    private Long id;
    private Audiencia audiencia;
    private Pessoa pessoa;
    private TipoParticipacao tipo;
    private Boolean intimado;
    private String observacoes;

    // Construtores

    public ParticipacaoAudiencia() {
        this.intimado = false;
    }

    public ParticipacaoAudiencia(Long id, Audiencia audiencia, Pessoa pessoa,
                                 TipoParticipacao tipo, Boolean intimado, String observacoes) {
        this.id = id;
        this.audiencia = audiencia;
        this.pessoa = pessoa;
        this.tipo = tipo;
        this.intimado = intimado != null ? intimado : false;
        this.observacoes = observacoes;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Audiencia getAudiencia() {
        return audiencia;
    }

    public void setAudiencia(Audiencia audiencia) {
        // DEBUG_AUDIENCIAS: Verificar vínculo de participação
        if (audiencia != null) {
            System.out.println("DEBUG_AUDIENCIAS: Vinculando participação à audiência ID: " +
                audiencia.getId());
        }
        this.audiencia = audiencia;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public TipoParticipacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoParticipacao tipo) {
        this.tipo = tipo;
    }

    public Boolean getIntimado() {
        return intimado;
    }

    public void setIntimado(Boolean intimado) {
        this.intimado = intimado;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    @Override
    public String toString() {
        return "ParticipacaoAudiencia{" +
                "id=" + id +
                ", audienciaId=" + (audiencia != null ? audiencia.getId() : "null") +
                ", pessoa=" + (pessoa != null ? pessoa.getNome() : "null") +
                ", tipo=" + tipo +
                ", intimado=" + intimado +
                '}';
    }
}
