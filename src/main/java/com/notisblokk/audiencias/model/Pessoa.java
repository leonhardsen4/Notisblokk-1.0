package com.notisblokk.audiencias.model;

/**
 * Entidade que representa uma Pessoa (parte processual).
 *
 * Pode ser autor, réu, vítima, testemunha ou qualquer outra parte
 * que participe da audiência.
 */
public class Pessoa {

    private Long id;
    private String nome;
    private String cpf;
    private String telefone;
    private String email;
    private String observacoes;

    // Construtores

    public Pessoa() {
    }

    public Pessoa(Long id, String nome, String cpf, String telefone,
                  String email, String observacoes) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
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
        return "Pessoa{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cpf='" + cpf + '\'' +
                ", telefone='" + telefone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
