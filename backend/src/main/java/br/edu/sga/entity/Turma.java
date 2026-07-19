package br.edu.sga.entity;

import br.edu.sga.enums.StatusTurma;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "turmas")
public class Turma extends PanacheEntity {
    @NotBlank
    public String nome;
    @ManyToOne
    @JoinColumn(name = "curso_id")
    public Curso curso;
    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    @JsonIgnore
    @Deprecated(forRemoval = false)
    public Disciplina disciplina;
    @ManyToOne
    @JoinColumn(name = "professor_id")
    @JsonIgnore
    @Deprecated(forRemoval = false)
    public Professor professor;
    @ManyToOne
    @JoinColumn(name = "ano_letivo_id")
    public AnoLetivo anoLetivo;
    @ManyToOne
    @JoinColumn(name = "periodo_letivo_id")
    public PeriodoLetivo periodoLetivo;
    public String descricao;
    public String turno;
    @JsonIgnore
    @Deprecated(forRemoval = false)
    public String horario;
    @JsonIgnore
    @Deprecated(forRemoval = false)
    public String sala;
    @Column(name = "quantidade_maxima_alunos")
    public Integer quantidadeMaximaAlunos;
    @Column(name = "ano_periodo")
    public String anoPeriodo;
    @Column(name = "data_inicio")
    public LocalDate dataInicio;
    @Column(name = "data_termino")
    public LocalDate dataTermino;
    @NotNull
    @Enumerated(EnumType.STRING)
    public StatusTurma status = StatusTurma.PLANEJADA;
}
