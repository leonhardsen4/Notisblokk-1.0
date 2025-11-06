package com.notisblokk.audiencias.model;

import com.notisblokk.audiencias.model.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidade principal que representa uma Audiência Judicial.
 *
 * Contém todas as informações sobre a audiência, incluindo data, horário,
 * tipo, formato, participantes e status.
 *
 * IMPORTANTE: Formatação de datas seguindo padrão brasileiro (dd/MM/yyyy)
 * conforme decisão técnica do projeto (ver PROGRESS.md).
 */
public class Audiencia {

    private Long id;
    private String numeroProcesso;

    // Relacionamentos
    private Vara vara;
    private Juiz juiz;
    private Promotor promotor;

    // Data e horário
    private LocalDate dataAudiencia;
    private LocalTime horarioInicio;
    private Integer duracao; // em minutos
    private LocalTime horarioFim; // calculado automaticamente
    private String diaSemana; // calculado automaticamente

    // Tipo e formato
    private TipoAudiencia tipoAudiencia;
    private FormatoAudiencia formato;
    private Competencia competencia;
    private StatusAudiencia status;

    // Informações adicionais
    private String artigo;
    private String observacoes;

    // Flags especiais
    private Boolean reuPreso;
    private Boolean agendamentoTeams;
    private Boolean reconhecimento;
    private Boolean depoimentoEspecial;

    // Auditoria
    private LocalDateTime criacao;
    private LocalDateTime atualizacao;

    // Construtores

    public Audiencia() {
        // Valores padrão
        this.reuPreso = false;
        this.agendamentoTeams = false;
        this.reconhecimento = false;
        this.depoimentoEspecial = false;
        this.status = StatusAudiencia.DESIGNADA;
    }

    public Audiencia(Long id, String numeroProcesso, Vara vara, LocalDate dataAudiencia,
                     LocalTime horarioInicio, Integer duracao, TipoAudiencia tipoAudiencia,
                     FormatoAudiencia formato, Competencia competencia) {
        this();
        this.id = id;
        this.numeroProcesso = numeroProcesso;
        this.vara = vara;
        this.dataAudiencia = dataAudiencia;
        this.horarioInicio = horarioInicio;
        this.duracao = duracao;
        this.tipoAudiencia = tipoAudiencia;
        this.formato = formato;
        this.competencia = competencia;

        // Calcular campos derivados
        calcularHorarioFim();
        calcularDiaSemana();
    }

    // Métodos auxiliares

    /**
     * Calcula o horário de fim baseado no horário de início e duração.
     * DEBUG_AUDIENCIAS: Log importante para detectar problemas de cálculo.
     */
    public void calcularHorarioFim() {
        if (this.horarioInicio != null && this.duracao != null && this.duracao > 0) {
            this.horarioFim = this.horarioInicio.plusMinutes(this.duracao);
            // DEBUG_AUDIENCIAS: Verificar cálculo de horário
            System.out.println("DEBUG_AUDIENCIAS: Horário calculado - Início: " +
                this.horarioInicio + ", Duração: " + this.duracao +
                " min, Fim: " + this.horarioFim);
        }
    }

    /**
     * Calcula o dia da semana baseado na data da audiência.
     * DEBUG_AUDIENCIAS: Log importante para verificar formatação de data.
     */
    public void calcularDiaSemana() {
        if (this.dataAudiencia != null) {
            String[] diasSemana = {"Segunda-feira", "Terça-feira", "Quarta-feira",
                                   "Quinta-feira", "Sexta-feira", "Sábado", "Domingo"};
            this.diaSemana = diasSemana[this.dataAudiencia.getDayOfWeek().getValue() - 1];
            // DEBUG_AUDIENCIAS: Verificar cálculo de dia da semana
            System.out.println("DEBUG_AUDIENCIAS: Data: " + this.dataAudiencia +
                ", Dia da semana: " + this.diaSemana);
        }
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

    public Vara getVara() {
        return vara;
    }

    public void setVara(Vara vara) {
        this.vara = vara;
    }

    public Juiz getJuiz() {
        return juiz;
    }

    public void setJuiz(Juiz juiz) {
        this.juiz = juiz;
    }

    public Promotor getPromotor() {
        return promotor;
    }

    public void setPromotor(Promotor promotor) {
        this.promotor = promotor;
    }

    public LocalDate getDataAudiencia() {
        return dataAudiencia;
    }

    public void setDataAudiencia(LocalDate dataAudiencia) {
        this.dataAudiencia = dataAudiencia;
        calcularDiaSemana();
    }

    public LocalTime getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(LocalTime horarioInicio) {
        this.horarioInicio = horarioInicio;
        calcularHorarioFim();
    }

    public Integer getDuracao() {
        return duracao;
    }

    public void setDuracao(Integer duracao) {
        this.duracao = duracao;
        calcularHorarioFim();
    }

    public LocalTime getHorarioFim() {
        return horarioFim;
    }

    public void setHorarioFim(LocalTime horarioFim) {
        this.horarioFim = horarioFim;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(String diaSemana) {
        this.diaSemana = diaSemana;
    }

    public TipoAudiencia getTipoAudiencia() {
        return tipoAudiencia;
    }

    public void setTipoAudiencia(TipoAudiencia tipoAudiencia) {
        this.tipoAudiencia = tipoAudiencia;
    }

    public FormatoAudiencia getFormato() {
        return formato;
    }

    public void setFormato(FormatoAudiencia formato) {
        this.formato = formato;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public void setCompetencia(Competencia competencia) {
        this.competencia = competencia;
    }

    public StatusAudiencia getStatus() {
        return status;
    }

    public void setStatus(StatusAudiencia status) {
        this.status = status;
    }

    public String getArtigo() {
        return artigo;
    }

    public void setArtigo(String artigo) {
        this.artigo = artigo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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

    public LocalDateTime getCriacao() {
        return criacao;
    }

    public void setCriacao(LocalDateTime criacao) {
        this.criacao = criacao;
    }

    public LocalDateTime getAtualizacao() {
        return atualizacao;
    }

    public void setAtualizacao(LocalDateTime atualizacao) {
        this.atualizacao = atualizacao;
    }

    @Override
    public String toString() {
        return "Audiencia{" +
                "id=" + id +
                ", numeroProcesso='" + numeroProcesso + '\'' +
                ", vara=" + (vara != null ? vara.getNome() : "null") +
                ", dataAudiencia=" + dataAudiencia +
                ", horarioInicio=" + horarioInicio +
                ", horarioFim=" + horarioFim +
                ", diaSemana='" + diaSemana + '\'' +
                ", tipoAudiencia=" + tipoAudiencia +
                ", formato=" + formato +
                ", status=" + status +
                '}';
    }
}
