package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "cursos")
public class Curso extends PanacheEntity {
    @NotBlank
    public String nome;
    @Column(length = 2000)
    public String descricao;
    @Min(1)
    public Integer cargaHorariaTotal;
    public boolean ativo = true;
}
