package br.edu.sga.entity;

import br.edu.sga.enums.StatusAluno;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "alunos")
public class Aluno extends PanacheEntity {
    @NotBlank
    public String nome;
    @Column(unique = true)
    public String cpf;
    @Email
    public String email;
    public String telefone;
    public LocalDate dataNascimento;
    public String endereco;
    @NotNull
    @Enumerated(EnumType.STRING)
    public StatusAluno status = StatusAluno.ATIVO;
    public LocalDate dataIngresso;
    @Column(length = 2000)
    public String observacoes;
    @OneToOne
    public Usuario usuario;
}
