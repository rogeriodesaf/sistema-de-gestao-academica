package br.edu.sga.entity;

import br.edu.sga.enums.StatusHistorico;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "historicos_escolares")
public class HistoricoEscolar extends PanacheEntity {
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
    @ManyToOne
    @JoinColumn(name = "oferta_disciplina_id")
    public OfertaDisciplina ofertaDisciplina;
    @ManyToOne
    @JoinColumn(name = "matricula_disciplina_id")
    public MatriculaDisciplina matriculaDisciplina;
    @ManyToOne
    @JoinColumn(name = "professor_responsavel_id")
    public Professor professorResponsavel;
    @Column(name = "carga_horaria")
    public Integer cargaHoraria;
    @Column(name = "nota_final")
    public BigDecimal notaFinal;
    @Column(name = "frequencia_final")
    public BigDecimal frequenciaFinal;
    @Enumerated(EnumType.STRING)
    public StatusHistorico situacao = StatusHistorico.CURSANDO;
    @Column(name = "periodo_cursado")
    public String periodoCursado;
}
