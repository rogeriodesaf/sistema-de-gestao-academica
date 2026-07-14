package br.edu.sga.entity;

import br.edu.sga.enums.TipoVinculoArquivo;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "arquivos_professor")
public class ArquivoProfessor extends PanacheEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    public Professor professor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oferta_disciplina_id", nullable = false)
    public OfertaDisciplina ofertaDisciplina;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id")
    public AulaMinistrada aula;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliacao_id")
    public Avaliacao avaliacao;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vinculo", nullable = false)
    public TipoVinculoArquivo tipoVinculo;
    @Column(nullable = false)
    public String titulo;
    @Column(name = "nome_original", nullable = false)
    public String nomeOriginal;
    @Column(name = "mime_type", nullable = false)
    public String mimeType;
    @Column(nullable = false)
    public Long tamanho;
    @Column(nullable = false, length = 2000)
    public String caminho;
    @Column(name = "data_envio", nullable = false)
    public LocalDateTime dataEnvio = LocalDateTime.now();
}
