package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "aulas_ministradas")
public class AulaMinistrada extends PanacheEntity {
    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    public Disciplina disciplina;
    @ManyToOne
    @JoinColumn(name = "turma_id")
    public Turma turma;
    @ManyToOne
    @JoinColumn(name = "professor_id")
    public Professor professor;
    @ManyToOne
    @JoinColumn(name = "oferta_disciplina_id")
    public OfertaDisciplina ofertaDisciplina;
    @Column(name = "data_aula")
    public LocalDate dataAula;
    @Column(name = "conteudo_ministrado", length = 4000)
    public String conteudoMinistrado;
    @Column(length = 2000)
    public String observacoes;
    @Column(name = "carga_horaria_aula")
    public Integer cargaHorariaAula;
}
