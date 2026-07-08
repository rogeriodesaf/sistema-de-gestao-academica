package br.edu.sga.entity;

import br.edu.sga.enums.StatusMatricula;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "matriculas")
public class Matricula extends PanacheEntity {
    @ManyToOne
    public Aluno aluno;
    @ManyToOne
    public Curso curso;
    @ManyToOne
    public Turma turma;
    @ManyToOne
    public Disciplina disciplina;
    public LocalDate dataMatricula = LocalDate.now();
    @Enumerated(EnumType.STRING)
    public StatusMatricula status = StatusMatricula.ATIVA;
}
