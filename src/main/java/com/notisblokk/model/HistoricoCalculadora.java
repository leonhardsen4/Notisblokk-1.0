package com.notisblokk.model;

import java.time.LocalDateTime;

/**
 * Entidade que representa um item do histórico da calculadora.
 *
 * <p>Armazena expressões matemáticas calculadas pelo usuário, incluindo
 * o resultado e o tipo de operação realizada.</p>
 *
 * @author Notisblokk Team
 * @version 1.0
 * @since 2025-01-26
 */
public class HistoricoCalculadora {

    private Long id;
    private Long usuarioId;
    private String expressao;
    private Double resultado;
    private String tipoOperacao; // SOMA, SUBTRACAO, MULTIPLICACAO, DIVISAO, PORCENTAGEM, RAIZ, QUADRADO
    private LocalDateTime dataCriacao;
    private Long sessaoId; // ID da sessão que criou o registro

    /**
     * Construtor padrão.
     */
    public HistoricoCalculadora() {
    }

    /**
     * Construtor completo.
     *
     * @param id identificador único do registro
     * @param usuarioId ID do usuário que realizou o cálculo
     * @param expressao expressão matemática calculada
     * @param resultado resultado do cálculo
     * @param tipoOperacao tipo de operação (SOMA, SUBTRACAO, etc.)
     * @param dataCriacao data e hora do cálculo
     * @param sessaoId ID da sessão ativa
     */
    public HistoricoCalculadora(Long id, Long usuarioId, String expressao, Double resultado,
                                String tipoOperacao, LocalDateTime dataCriacao, Long sessaoId) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.expressao = expressao;
        this.resultado = resultado;
        this.tipoOperacao = tipoOperacao;
        this.dataCriacao = dataCriacao;
        this.sessaoId = sessaoId;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getExpressao() {
        return expressao;
    }

    public void setExpressao(String expressao) {
        this.expressao = expressao;
    }

    public Double getResultado() {
        return resultado;
    }

    public void setResultado(Double resultado) {
        this.resultado = resultado;
    }

    public String getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(String tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Long getSessaoId() {
        return sessaoId;
    }

    public void setSessaoId(Long sessaoId) {
        this.sessaoId = sessaoId;
    }

    @Override
    public String toString() {
        return "HistoricoCalculadora{" +
                "id=" + id +
                ", usuarioId=" + usuarioId +
                ", expressao='" + expressao + '\'' +
                ", resultado=" + resultado +
                ", tipoOperacao='" + tipoOperacao + '\'' +
                ", dataCriacao=" + dataCriacao +
                ", sessaoId=" + sessaoId +
                '}';
    }
}
