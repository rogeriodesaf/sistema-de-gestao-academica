package br.edu.sga.entity;

import br.edu.sga.enums.StatusNota;
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
@Table(name = "notas")
public class Nota extends PanacheEntity {
    @ManyToOne
    @JoinColumn(name = "aluno_id")
    public Aluno aluno;
    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    public Disciplina disciplina;
    @ManyToOne
    @JoinColumn(name = "turma_id")
    public Turma turma;
    @ManyToOne
    @JoinColumn(name = "oferta_disciplina_id")
    public OfertaDisciplina ofertaDisciplina;
    public BigDecimal nota1;
    public BigDecimal nota2;
    public BigDecimal trabalho;
    @Column(name = "avaliacao_final")
    public BigDecimal avaliacaoFinal;
    @Column(name = "media_final")
    public BigDecimal mediaFinal;
    @Enumerated(EnumType.STRING)
    public StatusNota situacao = StatusNota.PENDENTE;
}
