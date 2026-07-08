package br.edu.sga.entity;

import br.edu.sga.enums.StatusTurma;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    public Curso curso;
    public String anoPeriodo;
    public LocalDate dataInicio;
    public LocalDate dataTermino;
    @NotNull
    @Enumerated(EnumType.STRING)
    public StatusTurma status = StatusTurma.PLANEJADA;
}
