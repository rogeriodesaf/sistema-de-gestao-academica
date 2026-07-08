package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "professores")
public class Professor extends PanacheEntity {
    @NotBlank
    public String nome;
    @Column(unique = true)
    public String cpf;
    @Email
    public String email;
    public String telefone;
    public String formacao;
    public boolean ativo = true;
    @OneToOne
    public Usuario usuario;
}
