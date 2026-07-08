package br.edu.sga.entity;

import br.edu.sga.enums.StatusPeriodoLetivo;
import br.edu.sga.enums.TipoPeriodoLetivo;
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
@Table(name = "periodos_letivos")
public class PeriodoLetivo extends PanacheEntity {
    @ManyToOne
    @JoinColumn(name = "ano_letivo_id")
    @NotNull
    public AnoLetivo anoLetivo;
    @NotBlank
    public String nome;
    @NotNull
    public Integer ordem;
    @NotNull
    @Enumerated(EnumType.STRING)
    public TipoPeriodoLetivo tipo = TipoPeriodoLetivo.MODULO;
    @Column(name = "data_inicio")
    public LocalDate dataInicio;
    @Column(name = "data_fim")
    public LocalDate dataFim;
    @NotNull
    @Enumerated(EnumType.STRING)
    public StatusPeriodoLetivo status = StatusPeriodoLetivo.PLANEJADO;
}
