package br.edu.sga.entity;

import br.edu.sga.enums.StatusFrequencia;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "frequencias")
public class Frequencia extends PanacheEntity {
    @ManyToOne
    public AulaMinistrada aula;
    @ManyToOne
    public Aluno aluno;
    @ManyToOne
    @JoinColumn(name = "matricula_disciplina_id")
    public MatriculaDisciplina matriculaDisciplina;
    @Enumerated(EnumType.STRING)
    public StatusFrequencia status = StatusFrequencia.PRESENTE;
    public boolean presente;
    public String justificativa;
    @Column(length = 2000)
    public String observacao;
}
