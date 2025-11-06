package com.notisblokk.audiencias.model;

/**
 * Entidade que representa um Promotor de Justiça.
 *
 * Contém informações básicas sobre o membro do Ministério Público
 * responsável pela audiência.
 */
public class Promotor {

    private Long id;
    private String nome;
    private String telefone;
    private String email;
    private String observacoes;

    // Construtores

    public Promotor() {
    }

    public Promotor(Long id, String nome, String telefone, String email, String observacoes) {
        this.id = id;
        this.nome = nome;
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
        return "Promotor{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", telefone='" + telefone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
