package com.notisblokk.audiencias.dto;

/**
 * DTO para exibir detalhes completos de um participante em audiência,
 * incluindo dados da pessoa, tipo de participação, intimação e advogado (se houver).
 */
public class ParticipanteDetalhesDTO {

    // Dados da pessoa
    private Long pessoaId;
    private String pessoaNome;
    private String pessoaCpf;
    private String pessoaTelefone;
    private String pessoaEmail;

    // Dados da participação
    private String tipoParticipacao;
    private Boolean intimado;
    private String observacoes;

    // Dados do advogado (opcionais)
    private Long advogadoId;
    private String advogadoNome;
    private String advogadoOab;
    private String advogadoTelefone;
    private String advogadoEmail;
    private String tipoRepresentacao;

    // Construtor vazio
    public ParticipanteDetalhesDTO() {
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

    public String getPessoaCpf() {
        return pessoaCpf;
    }

    public void setPessoaCpf(String pessoaCpf) {
        this.pessoaCpf = pessoaCpf;
    }

    public String getPessoaTelefone() {
        return pessoaTelefone;
    }

    public void setPessoaTelefone(String pessoaTelefone) {
        this.pessoaTelefone = pessoaTelefone;
    }

    public String getPessoaEmail() {
        return pessoaEmail;
    }

    public void setPessoaEmail(String pessoaEmail) {
        this.pessoaEmail = pessoaEmail;
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

    public String getAdvogadoTelefone() {
        return advogadoTelefone;
    }

    public void setAdvogadoTelefone(String advogadoTelefone) {
        this.advogadoTelefone = advogadoTelefone;
    }

    public String getAdvogadoEmail() {
        return advogadoEmail;
    }

    public void setAdvogadoEmail(String advogadoEmail) {
        this.advogadoEmail = advogadoEmail;
    }

    public String getTipoRepresentacao() {
        return tipoRepresentacao;
    }

    public void setTipoRepresentacao(String tipoRepresentacao) {
        this.tipoRepresentacao = tipoRepresentacao;
    }

    @Override
    public String toString() {
        return "ParticipanteDetalhesDTO{" +
                "pessoaNome='" + pessoaNome + '\'' +
                ", tipoParticipacao='" + tipoParticipacao + '\'' +
                ", intimado=" + intimado +
                ", advogadoNome='" + advogadoNome + '\'' +
                '}';
    }
}
