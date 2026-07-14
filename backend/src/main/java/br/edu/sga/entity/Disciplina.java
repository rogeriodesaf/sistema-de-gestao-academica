package br.edu.sga.entity;

import br.edu.sga.enums.TipoComponenteCurricular;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    @ManyToOne
    @JoinColumn(name = "curso_id")
    public Curso curso;
    @ManyToOne
    @JoinColumn(name = "modulo_id")
    public Modulo modulo;
    @ManyToOne
    @JoinColumn(name = "modulo_original_id")
    public Modulo moduloOriginal;
    @ManyToOne
    @JoinColumn(name = "professor_responsavel_id")
    public Professor professorResponsavel;
    @Min(1)
    @Column(name = "carga_horaria")
    public Integer cargaHoraria;
    @Min(0)
    public Integer creditos;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_componente", nullable = false)
    public TipoComponenteCurricular tipoComponente = TipoComponenteCurricular.OBRIGATORIA;
    @Column(length = 4000)
    public String ementa;
    @Column(name = "ementa_resumo", length = 4000)
    public String ementaResumo;
    @Column(name = "ementa_pdf_caminho")
    public String ementaPdfCaminho;
    @Column(name = "ementa_pdf_nome")
    public String ementaPdfNome;
    @Column(name = "ementa_pdf_tipo")
    public String ementaPdfTipo;
    @Column(name = "ementa_pdf_tamanho")
    public Long ementaPdfTamanho;
    @Column(length = 4000)
    public String bibliografia;
    public boolean ativo = true;
}
