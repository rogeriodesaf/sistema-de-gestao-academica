package br.edu.sga.entity;

import br.edu.sga.enums.StatusHistorico;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "historicos_escolares")
public class HistoricoEscolar extends PanacheEntity {
    @ManyToOne
    public Aluno aluno;
    @ManyToOne
    public Curso curso;
    @ManyToOne
    public Turma turma;
    @ManyToOne
    public Disciplina disciplina;
    @ManyToOne
    public Professor professorResponsavel;
    public Integer cargaHoraria;
    public BigDecimal notaFinal;
    public BigDecimal frequenciaFinal;
    @Enumerated(EnumType.STRING)
    public StatusHistorico situacao = StatusHistorico.CURSANDO;
    public String periodoCursado;
}
