package br.edu.sga.entity;

import br.edu.sga.enums.StatusMatriculaDisciplina;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "matriculas_disciplinas")
public class MatriculaDisciplina extends PanacheEntity {
    @ManyToOne
    @JoinColumn(name = "aluno_id")
    @NotNull
    public Aluno aluno;
    @ManyToOne
    @JoinColumn(name = "curso_id")
    public Curso curso;
    @ManyToOne
    @JoinColumn(name = "periodo_letivo_id")
    public PeriodoLetivo periodoLetivo;
    @ManyToOne
    @JoinColumn(name = "oferta_disciplina_id")
    @NotNull
    public OfertaDisciplina ofertaDisciplina;
    @Column(name = "data_matricula")
    public LocalDate dataMatricula = LocalDate.now();
    @NotNull
    @Enumerated(EnumType.STRING)
    public StatusMatriculaDisciplina status = StatusMatriculaDisciplina.MATRICULADO;
    @Column(name = "nota_final")
    public java.math.BigDecimal notaFinal;
    @Column(name = "frequencia_final")
    public java.math.BigDecimal frequenciaFinal;
    @Column(length = 2000)
    public String observacoes;
}
