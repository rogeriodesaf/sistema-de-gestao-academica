package br.edu.sga.entity;

import br.edu.sga.enums.StatusMatricula;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "matriculas")
public class Matricula extends PanacheEntity {
    @ManyToOne
    @JoinColumn(name = "aluno_id")
    public Aluno aluno;
    @ManyToOne
    @JoinColumn(name = "curso_id")
    public Curso curso;
    @ManyToOne
    @JoinColumn(name = "turma_id")
    public Turma turma;
    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    public Disciplina disciplina;
    @Column(name = "data_matricula")
    public LocalDate dataMatricula = LocalDate.now();
    @Column(name = "data_conclusao")
    public LocalDate dataConclusao;
    @Enumerated(EnumType.STRING)
    public StatusMatricula status = StatusMatricula.ATIVA;
}
