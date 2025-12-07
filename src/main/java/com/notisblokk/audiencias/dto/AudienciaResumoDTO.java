package com.notisblokk.audiencias.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para retornar resumo de audiências vinculadas a um processo.
 *
 * Usado na listagem de audiências dentro dos detalhes de um processo.
 */
public class AudienciaResumoDTO {

    private Long id;
    private LocalDate dataAudiencia;
    private LocalTime horarioInicio;
    private LocalTime horarioFim;
    private String tipoAudiencia;   // Ex: "Inicial", "Instrução e Julgamento"
    private String formato;          // Ex: "Presencial", "Videoconferência"
    private String status;           // Ex: "Designada", "Realizada"
    private String juizNome;
    private String promotorNome;

    // Construtores

    public AudienciaResumoDTO() {
    }

    public AudienciaResumoDTO(Long id, LocalDate dataAudiencia, LocalTime horarioInicio,
                              LocalTime horarioFim, String tipoAudiencia, String formato, String status) {
        this.id = id;
        this.dataAudiencia = dataAudiencia;
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
        this.tipoAudiencia = tipoAudiencia;
        this.formato = formato;
        this.status = status;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDataAudiencia() {
        return dataAudiencia;
    }

    public void setDataAudiencia(LocalDate dataAudiencia) {
        this.dataAudiencia = dataAudiencia;
    }

    public LocalTime getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(LocalTime horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public LocalTime getHorarioFim() {
        return horarioFim;
    }

    public void setHorarioFim(LocalTime horarioFim) {
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

    public String getJuizNome() {
        return juizNome;
    }

    public void setJuizNome(String juizNome) {
        this.juizNome = juizNome;
    }

    public String getPromotorNome() {
        return promotorNome;
    }

    public void setPromotorNome(String promotorNome) {
        this.promotorNome = promotorNome;
    }

    @Override
    public String toString() {
        return "AudienciaResumoDTO{" +
                "id=" + id +
                ", dataAudiencia=" + dataAudiencia +
                ", horarioInicio=" + horarioInicio +
                ", tipoAudiencia='" + tipoAudiencia + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
