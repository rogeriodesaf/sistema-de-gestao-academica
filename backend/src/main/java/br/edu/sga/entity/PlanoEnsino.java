package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "planos_ensino")
public class PlanoEnsino extends PanacheEntity {
    @ManyToOne
    public Disciplina disciplina;
    @ManyToOne
    public Turma turma;
    @Column(length = 4000)
    public String objetivos;
    @Column(length = 4000)
    public String ementa;
    @Column(length = 4000)
    public String conteudoProgramatico;
    @Column(length = 4000)
    public String metodologia;
    @Column(length = 4000)
    public String criteriosAvaliacao;
    @Column(length = 4000)
    public String bibliografiaBasica;
    @Column(length = 4000)
    public String bibliografiaComplementar;
    public LocalDateTime dataCadastro = LocalDateTime.now();
    public LocalDateTime ultimaAtualizacao = LocalDateTime.now();
}
