package br.edu.sga.entity;

import br.edu.sga.enums.StatusVinculo;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vinculos_professor_disciplina_turma")
public class VinculoProfessorDisciplinaTurma extends PanacheEntity {
    @ManyToOne
    public Professor professor;
    @ManyToOne
    public Disciplina disciplina;
    @ManyToOne
    public Turma turma;
    public String periodo;
    @Enumerated(EnumType.STRING)
    public StatusVinculo status = StatusVinculo.ATIVO;
}
