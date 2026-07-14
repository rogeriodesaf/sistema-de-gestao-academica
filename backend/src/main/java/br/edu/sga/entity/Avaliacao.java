package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes")
public class Avaliacao extends PanacheEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_disciplina_id", nullable = false)
    public OfertaDisciplina ofertaDisciplina;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    public Professor professor;
    @Column(nullable = false)
    public String nome;
    @Column(length = 2000)
    public String descricao;
    @Column(name = "ordem_avaliacao", nullable = false)
    public Integer ordem;
    @Column(name = "data_avaliacao")
    public LocalDate data;
    @Column(name = "nota_maxima", nullable = false, precision = 7, scale = 2)
    public BigDecimal notaMaxima;
    @Column(nullable = false, precision = 7, scale = 2)
    public BigDecimal peso;
    @Column(name = "data_criacao", nullable = false)
    public LocalDateTime dataCriacao = LocalDateTime.now();
}
