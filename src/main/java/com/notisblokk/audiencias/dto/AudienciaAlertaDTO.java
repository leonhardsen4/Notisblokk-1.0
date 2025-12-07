package com.notisblokk.audiencias.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO para retornar audiências com alertas de informações faltantes.
 *
 * Usado no widget de alertas do dashboard e na tela de audiências.
 * Inclui criticidade baseada em dias restantes e lista de informações ausentes.
 */
public class AudienciaAlertaDTO {

    private Long audienciaId;
    private String numeroProcesso;
    private LocalDate dataAudiencia;
    private LocalTime horarioInicio;
    private String varaNome;
    private String competencia;
    private String juizNome;        // Pode ser null
    private String promotorNome;    // Pode ser null
    private String tipoAudiencia;   // Pode ser null
    private Integer diasRestantes;

    // Lista de informações faltantes
    // Ex: ["Juiz", "2 pessoas não intimadas", "Tipo de audiência"]
    private List<String> informacoesAusentes;

    // Níveis de criticidade
    // CRITICO (0-3 dias), ALTO (4-7 dias), MEDIO (8-15 dias), BAIXO (>15 dias)
    private String nivelCriticidade;

    // Cor do badge para exibição
    // #EF4444 (vermelho), #F97316 (laranja), #F59E0B (amarelo), #10B981 (verde)
    private String corBadge;

    // Ícone de criticidade
    // 🔴 CRITICO, 🟠 ALTO, 🟡 MEDIO, 🟢 BAIXO
    private String iconeCriticidade;

    // Construtores

    public AudienciaAlertaDTO() {
    }

    // Getters e Setters

    public Long getAudienciaId() {
        return audienciaId;
    }

    public void setAudienciaId(Long audienciaId) {
        this.audienciaId = audienciaId;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
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

    public String getVaraNome() {
        return varaNome;
    }

    public void setVaraNome(String varaNome) {
        this.varaNome = varaNome;
    }

    public String getCompetencia() {
        return competencia;
    }

    public void setCompetencia(String competencia) {
        this.competencia = competencia;
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

    public String getTipoAudiencia() {
        return tipoAudiencia;
    }

    public void setTipoAudiencia(String tipoAudiencia) {
        this.tipoAudiencia = tipoAudiencia;
    }

    public Integer getDiasRestantes() {
        return diasRestantes;
    }

    public void setDiasRestantes(Integer diasRestantes) {
        this.diasRestantes = diasRestantes;
    }

    public List<String> getInformacoesAusentes() {
        return informacoesAusentes;
    }

    public void setInformacoesAusentes(List<String> informacoesAusentes) {
        this.informacoesAusentes = informacoesAusentes;
    }

    public String getNivelCriticidade() {
        return nivelCriticidade;
    }

    public void setNivelCriticidade(String nivelCriticidade) {
        this.nivelCriticidade = nivelCriticidade;
    }

    public String getCorBadge() {
        return corBadge;
    }

    public void setCorBadge(String corBadge) {
        this.corBadge = corBadge;
    }

    public String getIconeCriticidade() {
        return iconeCriticidade;
    }

    public void setIconeCriticidade(String iconeCriticidade) {
        this.iconeCriticidade = iconeCriticidade;
    }

    @Override
    public String toString() {
        return "AudienciaAlertaDTO{" +
                "audienciaId=" + audienciaId +
                ", numeroProcesso='" + numeroProcesso + '\'' +
                ", dataAudiencia=" + dataAudiencia +
                ", diasRestantes=" + diasRestantes +
                ", nivelCriticidade='" + nivelCriticidade + '\'' +
                ", informacoesAusentes=" + (informacoesAusentes != null ? informacoesAusentes.size() : 0) +
                '}';
    }
}
