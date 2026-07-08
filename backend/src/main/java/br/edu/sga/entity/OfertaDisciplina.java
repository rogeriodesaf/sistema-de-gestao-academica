package br.edu.sga.entity;

import br.edu.sga.enums.StatusOfertaDisciplina;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "ofertas_disciplinas")
public class OfertaDisciplina extends PanacheEntity {
    @ManyToOne
    @JoinColumn(name = "turma_id")
    @NotNull
    public Turma turma;
    @ManyToOne
    @JoinColumn(name = "ano_letivo_id")
    @NotNull
    public AnoLetivo anoLetivo;
    @ManyToOne
    @JoinColumn(name = "periodo_letivo_id")
    @NotNull
    public PeriodoLetivo periodoLetivo;
    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    @NotNull
    public Disciplina disciplina;
    @ManyToOne
    @JoinColumn(name = "professor_id")
    public Professor professor;
    public Integer vagas;
    @Column(name = "carga_horaria_prevista")
    public Integer cargaHorariaPrevista;
    @Column(name = "carga_horaria_ministrada")
    public Integer cargaHorariaMinistrada;
    @Column(name = "data_inicio")
    public LocalDate dataInicio;
    @Column(name = "data_fim")
    public LocalDate dataFim;
    @NotNull
    @Enumerated(EnumType.STRING)
    public StatusOfertaDisciplina status = StatusOfertaDisciplina.ABERTA;
}
