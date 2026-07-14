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
    @Column(name = "carga_horaria_total")
    public Integer cargaHorariaTotal;
    @Min(0)
    @Column(name = "creditos_totais")
    public Integer creditosTotais;
    @Column(name = "grade_pdf_caminho")
    public String gradePdfCaminho;
    @Column(name = "grade_pdf_nome")
    public String gradePdfNome;
    @Column(name = "grade_pdf_tipo")
    public String gradePdfTipo;
    @Column(name = "grade_pdf_tamanho")
    public Long gradePdfTamanho;
    public boolean ativo = true;
}
