package com.notisblokk.audiencias.model;

/**
 * Entidade que representa uma Vara judicial.
 *
 * Contém informações sobre a vara onde a audiência será realizada,
 * incluindo dados de localização e contato.
 */
public class Vara {

    private Long id;
    private String nome;
    private String comarca;
    private String endereco;
    private String telefone;
    private String email;
    private String observacoes;

    // Construtores

    public Vara() {
    }

    public Vara(Long id, String nome, String comarca, String endereco,
                String telefone, String email, String observacoes) {
        this.id = id;
        this.nome = nome;
        this.comarca = comarca;
        this.endereco = endereco;
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

    public String getComarca() {
        return comarca;
    }

    public void setComarca(String comarca) {
        this.comarca = comarca;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
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
        return "Vara{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", comarca='" + comarca + '\'' +
                ", endereco='" + endereco + '\'' +
                ", telefone='" + telefone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
