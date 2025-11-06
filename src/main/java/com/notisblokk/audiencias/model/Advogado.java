package com.notisblokk.audiencias.model;

/**
 * Entidade que representa um Advogado.
 *
 * Contém informações sobre advogados que atuam nas audiências,
 * incluindo número de registro na OAB.
 */
public class Advogado {

    private Long id;
    private String nome;
    private String oab;
    private String telefone;
    private String email;
    private String observacoes;

    // Construtores

    public Advogado() {
    }

    public Advogado(Long id, String nome, String oab, String telefone,
                    String email, String observacoes) {
        this.id = id;
        this.nome = nome;
        this.oab = oab;
        this.telefone = telefone;
        this.email = email;
        this.observacoes = observacoes;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getOab() {
        return oab;
    }

    public void setOab(String oab) {
        this.oab = oab;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    @Override
    public String toString() {
        return "Advogado{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", oab='" + oab + '\'' +
                ", telefone='" + telefone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
