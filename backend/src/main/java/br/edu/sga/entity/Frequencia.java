package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "frequencias")
public class Frequencia extends PanacheEntity {
    @ManyToOne
    public AulaMinistrada aula;
    @ManyToOne
    public Aluno aluno;
    public boolean presente;
    public String justificativa;
    @Column(length = 2000)
    public String observacao;
}
