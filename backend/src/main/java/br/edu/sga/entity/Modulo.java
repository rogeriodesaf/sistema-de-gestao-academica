package br.edu.sga.entity;

import br.edu.sga.enums.StatusModulo;
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
@Table(name = "modulos")
public class Modulo extends PanacheEntity {
    @NotBlank
    public String nome;
    @Column(length = 2000)
    public String descricao;
    @NotNull
    public Integer ordem;
    @ManyToOne
    @JoinColumn(name = "curso_id")
    public Curso curso;
    @ManyToOne
    @JoinColumn(name = "ano_letivo_id")
    public AnoLetivo anoLetivo;
    @Column(name = "data_inicio")
    public LocalDate dataInicio;
    @Column(name = "data_fim")
    public LocalDate dataFim;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public StatusModulo status = StatusModulo.ABERTO;
    public boolean ativo = true;
}
