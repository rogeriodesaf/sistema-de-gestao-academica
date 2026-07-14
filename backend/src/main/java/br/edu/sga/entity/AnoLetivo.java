package br.edu.sga.entity;

import br.edu.sga.enums.StatusAnoLetivo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "anos_letivos")
public class AnoLetivo extends PanacheEntity {
    @NotNull
    public Integer ano;
    @NotNull
    @Column(name = "data_inicio")
    public LocalDate dataInicio;
    @NotNull
    @Column(name = "data_fim")
    public LocalDate dataFim;
    @NotNull
    @Enumerated(EnumType.STRING)
    public StatusAnoLetivo status = StatusAnoLetivo.PLANEJADO;
    @JsonIgnore
    public boolean legado;
}
