package com.notisblokk.audiencias.dto;

/**
 * DTO para receber dados de participante do frontend
 */
public class ParticipanteRequest {

    private Long pessoaId;
    private String pessoaNome;
    private String tipoParticipacao;
    private Boolean intimado;
    private String observacoes;

    // Dados do advogado (opcionais)
    private Long advogadoId;
    private String advogadoNome;
    private String advogadoOab;
    private String tipoRepresentacao;

    // Construtores

    public ParticipanteRequest() {
    }

    // Getters e Setters

    public Long getPessoaId() {
        return pessoaId;
    }

    public void setPessoaId(Long pessoaId) {
        this.pessoaId = pessoaId;
    }

    public String getPessoaNome() {
        return pessoaNome;
    }

    public void setPessoaNome(String pessoaNome) {
        this.pessoaNome = pessoaNome;
    }

    public String getTipoParticipacao() {
        return tipoParticipacao;
    }

    public void setTipoParticipacao(String tipoParticipacao) {
        this.tipoParticipacao = tipoParticipacao;
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

    public Long getAdvogadoId() {
        return advogadoId;
    }

    public void setAdvogadoId(Long advogadoId) {
        this.advogadoId = advogadoId;
    }

    public String getAdvogadoNome() {
        return advogadoNome;
    }

    public void setAdvogadoNome(String advogadoNome) {
        this.advogadoNome = advogadoNome;
    }

    public String getAdvogadoOab() {
        return advogadoOab;
    }

    public void setAdvogadoOab(String advogadoOab) {
        this.advogadoOab = advogadoOab;
    }

    public String getTipoRepresentacao() {
        return tipoRepresentacao;
    }

    public void setTipoRepresentacao(String tipoRepresentacao) {
        this.tipoRepresentacao = tipoRepresentacao;
    }

    @Override
    public String toString() {
        return "ParticipanteRequest{" +
                "pessoaId=" + pessoaId +
                ", pessoaNome='" + pessoaNome + '\'' +
                ", tipoParticipacao='" + tipoParticipacao + '\'' +
                ", intimado=" + intimado +
                ", advogadoId=" + advogadoId +
                '}';
    }
}
