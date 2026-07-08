package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "disciplinas")
public class Disciplina extends PanacheEntity {
    @NotBlank
    public String nome;
    @Column(unique = true)
    public String codigo;
    @Min(1)
    public Integer cargaHoraria;
    @Column(length = 4000)
    public String ementa;
    @Column(length = 4000)
    public String bibliografia;
    public boolean ativo = true;
}
