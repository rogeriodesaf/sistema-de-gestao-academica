package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notas_avaliacoes")
public class NotaAvaliacao extends PanacheEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliacao_id", nullable = false)
    public Avaliacao avaliacao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false)
    public Aluno aluno;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matricula_disciplina_id", nullable = false)
    public MatriculaDisciplina matriculaDisciplina;
    @Column(nullable = false, precision = 7, scale = 2)
    public BigDecimal nota;
    @Column(length = 2000)
    public String observacao;
    @Column(name = "data_atualizacao", nullable = false)
    public LocalDateTime dataAtualizacao = LocalDateTime.now();
}
