package br.edu.sga.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "planos_ensino")
public class PlanoEnsino extends PanacheEntity {
    @ManyToOne
    @JoinColumn(name = "oferta_disciplina_id")
    @JsonIgnore
    public OfertaDisciplina ofertaDisciplina;
    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    @NotNull
    public Disciplina disciplina;
    @ManyToOne
    @JoinColumn(name = "turma_id")
    @JsonIgnore
    public Turma turma;
    @Column(length = 4000)
    public String objetivos;
    @Column(length = 4000)
    @JsonIgnore
    public String ementa;
    @Column(name = "conteudo_programatico", length = 4000)
    public String conteudoProgramatico;
    @Column(length = 4000)
    public String metodologia;
    @Column(name = "criterios_avaliacao", length = 4000)
    @JsonIgnore
    public String criteriosAvaliacao;
    @Column(name = "bibliografia_basica", length = 4000)
    public String bibliografiaBasica;
    @Column(name = "bibliografia_complementar", length = 4000)
    public String bibliografiaComplementar;
    @Column(length = 4000)
    public String observacoes;
    @Column(name = "plano_pdf_caminho")
    public String planoPdfCaminho;
    @Column(name = "plano_pdf_nome")
    public String planoPdfNome;
    @Column(name = "plano_pdf_tipo")
    public String planoPdfTipo;
    @Column(name = "plano_pdf_tamanho")
    public Long planoPdfTamanho;
    @Column(name = "data_cadastro")
    public LocalDateTime dataCadastro = LocalDateTime.now();
    @Column(name = "ultima_atualizacao")
    public LocalDateTime ultimaAtualizacao = LocalDateTime.now();
}
