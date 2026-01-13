package com.notisblokk.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Data Transfer Object para Tarefa.
 *
 * <p>Este DTO enriquece a entidade Tarefa com objetos completos de
 * Etiqueta e StatusTarefa, além de campos calculados como dias restantes
 * e formatação de datas.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class TarefaDTO {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Long id;
    private Etiqueta etiqueta;
    private StatusTarefa status;
    private String titulo;
    private String conteudo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    @JsonIgnore
    private LocalDate prazoFinal;

    private Long sessaoId;
    private Long usuarioId;

    // Campos calculados
    private Long diasRestantes;
    private String prazoFinalFormatado;
    private String dataCriacaoFormatada;
    private String dataAtualizacaoFormatada;

    /**
     * Construtor padrão.
     */
    public TarefaDTO() {
    }

    /**
     * Cria um TarefaDTO a partir de uma Tarefa, Etiqueta e StatusTarefa.
     *
     * @param tarefa tarefa original
     * @param etiqueta etiqueta associada
     * @param status status associado
     * @return TarefaDTO objeto DTO completo
     */
    public static TarefaDTO from(Tarefa tarefa, Etiqueta etiqueta, StatusTarefa status) {
        TarefaDTO dto = new TarefaDTO();
        dto.id = tarefa.getId();
        dto.etiqueta = etiqueta;
        dto.status = status;
        dto.titulo = tarefa.getTitulo();
        dto.conteudo = tarefa.getConteudo();
        dto.dataCriacao = tarefa.getDataCriacao();
        dto.dataAtualizacao = tarefa.getDataAtualizacao();
        dto.prazoFinal = tarefa.getPrazoFinal();
        dto.sessaoId = tarefa.getSessaoId();
        dto.usuarioId = tarefa.getUsuarioId();

        // Calcular campos derivados
        dto.calcularDiasRestantes();
        dto.formatarDatas();

        return dto;
    }

    /**
     * Calcula os dias restantes até o prazo final.
     *
     * <p>Tarefas com status "Resolvido" ou "Cancelado" não terão dias restantes calculados,
     * pois já foram concluídas e não precisam mais de alertas de prazo.</p>
     */
    private void calcularDiasRestantes() {
        // Não calcular dias restantes para tarefas resolvidas ou canceladas
        if (status != null) {
            String statusNome = status.getNome().toLowerCase();
            if (statusNome.contains("resolvid") || statusNome.contains("cancelad")) {
                this.diasRestantes = null; // Deixar null para indicar que não precisa contar
                return;
            }
        }

        if (prazoFinal != null) {
            LocalDate hoje = LocalDate.now();
            this.diasRestantes = ChronoUnit.DAYS.between(hoje, prazoFinal);
        }
    }

    /**
     * Formata as datas para apresentação.
     */
    private void formatarDatas() {
        if (prazoFinal != null) {
            this.prazoFinalFormatado = prazoFinal.format(DATE_FORMATTER);
        }
        if (dataCriacao != null) {
            this.dataCriacaoFormatada = dataCriacao.format(DATETIME_FORMATTER);
        }
        if (dataAtualizacao != null) {
            this.dataAtualizacaoFormatada = dataAtualizacao.format(DATETIME_FORMATTER);
        }
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Etiqueta getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(Etiqueta etiqueta) {
        this.etiqueta = etiqueta;
    }

    public StatusTarefa getStatus() {
        return status;
    }

    public void setStatus(StatusTarefa status) {
        this.status = status;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    @JsonIgnore
    public LocalDate getPrazoFinal() {
        return prazoFinal;
    }

    public void setPrazoFinal(LocalDate prazoFinal) {
        this.prazoFinal = prazoFinal;
    }

    /**
     * Retorna o prazo final formatado para o JSON.
     * Este método é usado pelo Jackson para serializar o campo "prazoFinal"
     * no formato brasileiro dd-MM-yyyy.
     */
    @JsonProperty("prazoFinal")
    public String getPrazoFinalParaJson() {
        return prazoFinal != null ? prazoFinal.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : null;
    }

    public Long getSessaoId() {
        return sessaoId;
    }

    public void setSessaoId(Long sessaoId) {
        this.sessaoId = sessaoId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getDiasRestantes() {
        return diasRestantes;
    }

    public void setDiasRestantes(Long diasRestantes) {
        this.diasRestantes = diasRestantes;
    }

    public String getPrazoFinalFormatado() {
        return prazoFinalFormatado;
    }

    public void setPrazoFinalFormatado(String prazoFinalFormatado) {
        this.prazoFinalFormatado = prazoFinalFormatado;
    }

    public String getDataCriacaoFormatada() {
        return dataCriacaoFormatada;
    }

    public void setDataCriacaoFormatada(String dataCriacaoFormatada) {
        this.dataCriacaoFormatada = dataCriacaoFormatada;
    }

    public String getDataAtualizacaoFormatada() {
        return dataAtualizacaoFormatada;
    }

    public void setDataAtualizacaoFormatada(String dataAtualizacaoFormatada) {
        this.dataAtualizacaoFormatada = dataAtualizacaoFormatada;
    }

    @Override
    public String toString() {
        return "TarefaDTO{" +
                "id=" + id +
                ", etiqueta=" + etiqueta +
                ", status=" + status +
                ", titulo='" + titulo + '\'' +
                ", diasRestantes=" + diasRestantes +
                ", prazoFinal=" + prazoFinal +
                '}';
    }
}
