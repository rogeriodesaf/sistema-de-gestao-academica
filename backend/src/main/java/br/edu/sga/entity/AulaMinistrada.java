package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "aulas_ministradas")
public class AulaMinistrada extends PanacheEntity {
    @ManyToOne
    public Disciplina disciplina;
    @ManyToOne
    public Turma turma;
    @ManyToOne
    public Professor professor;
    public LocalDate dataAula;
    @Column(length = 4000)
    public String conteudoMinistrado;
    @Column(length = 2000)
    public String observacoes;
    public Integer cargaHorariaAula;
}
