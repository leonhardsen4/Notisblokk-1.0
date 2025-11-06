package com.notisblokk.audiencias.dto;

/**
 * DTO para receber dados de audiência do frontend
 *
 * Converte tipos do JavaScript (strings) para tipos Java (LocalDate, LocalTime)
 */
public class AudienciaRequest {

    // Dados básicos
    private String numeroProcesso;
    private Long varaId;

    // Data e horário (recebidos como String)
    private String dataAudiencia;  // dd/MM/yyyy
    private String horarioInicio;  // HH:mm ou HH:mm:ss
    private String horarioFim;     // HH:mm ou HH:mm:ss

    // Tipo e formato
    private String tipoAudiencia;  // INICIAL, INSTRUCAO, etc
    private String formato;        // PRESENCIAL, VIDEOCONFERENCIA, HIBRIDO
    private String status;         // DESIGNADA, REALIZADA, etc

    // Participantes (IDs)
    private Long juizId;
    private Long promotorId;

    // Informações adicionais
    private String artigo;
    private Boolean reuPreso;
    private Boolean agendamentoTeams;
    private Boolean reconhecimento;
    private Boolean depoimentoEspecial;
    private String observacoes;

    // Participantes
    private java.util.List<ParticipanteRequest> participantes;

    // Construtores

    public AudienciaRequest() {
    }

    // Getters e Setters

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public Long getVaraId() {
        return varaId;
    }

    public void setVaraId(Long varaId) {
        this.varaId = varaId;
    }

    public String getDataAudiencia() {
        return dataAudiencia;
    }

    public void setDataAudiencia(String dataAudiencia) {
        this.dataAudiencia = dataAudiencia;
    }

    public String getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(String horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public String getHorarioFim() {
        return horarioFim;
    }

    public void setHorarioFim(String horarioFim) {
        this.horarioFim = horarioFim;
    }

    public String getTipoAudiencia() {
        return tipoAudiencia;
    }

    public void setTipoAudiencia(String tipoAudiencia) {
        this.tipoAudiencia = tipoAudiencia;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getJuizId() {
        return juizId;
    }

    public void setJuizId(Long juizId) {
        this.juizId = juizId;
    }

    public Long getPromotorId() {
        return promotorId;
    }

    public void setPromotorId(Long promotorId) {
        this.promotorId = promotorId;
    }

    public String getArtigo() {
        return artigo;
    }

    public void setArtigo(String artigo) {
        this.artigo = artigo;
    }

    public Boolean getReuPreso() {
        return reuPreso;
    }

    public void setReuPreso(Boolean reuPreso) {
        this.reuPreso = reuPreso;
    }

    public Boolean getAgendamentoTeams() {
        return agendamentoTeams;
    }

    public void setAgendamentoTeams(Boolean agendamentoTeams) {
        this.agendamentoTeams = agendamentoTeams;
    }

    public Boolean getReconhecimento() {
        return reconhecimento;
    }

    public void setReconhecimento(Boolean reconhecimento) {
        this.reconhecimento = reconhecimento;
    }

    public Boolean getDepoimentoEspecial() {
        return depoimentoEspecial;
    }

    public void setDepoimentoEspecial(Boolean depoimentoEspecial) {
        this.depoimentoEspecial = depoimentoEspecial;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public java.util.List<ParticipanteRequest> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(java.util.List<ParticipanteRequest> participantes) {
        this.participantes = participantes;
    }

    @Override
    public String toString() {
        return "AudienciaRequest{" +
                "numeroProcesso='" + numeroProcesso + '\'' +
                ", varaId=" + varaId +
                ", dataAudiencia='" + dataAudiencia + '\'' +
                ", horarioInicio='" + horarioInicio + '\'' +
                ", horarioFim='" + horarioFim + '\'' +
                ", tipoAudiencia='" + tipoAudiencia + '\'' +
                ", formato='" + formato + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
