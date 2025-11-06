package com.notisblokk.audiencias.model;

import com.notisblokk.audiencias.model.enums.TipoRepresentacao;

/**
 * Entidade que representa a representação de um advogado em uma audiência.
 *
 * Relaciona um Advogado com uma Audiência e com a Pessoa (cliente) que ele representa,
 * indicando o tipo de representação (constituído, dativo, ad hoc, etc.).
 */
public class RepresentacaoAdvogado {

    private Long id;
    private Audiencia audiencia;
    private Advogado advogado;
    private Pessoa cliente; // A pessoa que está sendo representada
    private TipoRepresentacao tipo;

    // Construtores

    public RepresentacaoAdvogado() {
    }

    public RepresentacaoAdvogado(Long id, Audiencia audiencia, Advogado advogado,
                                 Pessoa cliente, TipoRepresentacao tipo) {
        this.id = id;
        this.audiencia = audiencia;
        this.advogado = advogado;
        this.cliente = cliente;
        this.tipo = tipo;
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
        // DEBUG_AUDIENCIAS: Verificar vínculo de representação
        if (audiencia != null && advogado != null && cliente != null) {
            System.out.println("DEBUG_AUDIENCIAS: Vinculando advogado " + advogado.getNome() +
                " (OAB: " + advogado.getOab() + ") ao cliente " + cliente.getNome() +
                " na audiência ID: " + audiencia.getId());
        }
        this.audiencia = audiencia;
    }

    public Advogado getAdvogado() {
        return advogado;
    }

    public void setAdvogado(Advogado advogado) {
        this.advogado = advogado;
    }

    public Pessoa getCliente() {
        return cliente;
    }

    public void setCliente(Pessoa cliente) {
        this.cliente = cliente;
    }

    public TipoRepresentacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoRepresentacao tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return "RepresentacaoAdvogado{" +
                "id=" + id +
                ", audienciaId=" + (audiencia != null ? audiencia.getId() : "null") +
                ", advogado=" + (advogado != null ? advogado.getNome() : "null") +
                ", cliente=" + (cliente != null ? cliente.getNome() : "null") +
                ", tipo=" + tipo +
                '}';
    }
}
