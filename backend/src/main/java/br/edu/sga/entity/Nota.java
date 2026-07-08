package br.edu.sga.entity;

import br.edu.sga.enums.StatusNota;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "notas")
public class Nota extends PanacheEntity {
    @ManyToOne
    public Aluno aluno;
    @ManyToOne
    public Disciplina disciplina;
    @ManyToOne
    public Turma turma;
    public BigDecimal nota1;
    public BigDecimal nota2;
    public BigDecimal trabalho;
    public BigDecimal avaliacaoFinal;
    public BigDecimal mediaFinal;
    @Enumerated(EnumType.STRING)
    public StatusNota situacao = StatusNota.PENDENTE;
}
