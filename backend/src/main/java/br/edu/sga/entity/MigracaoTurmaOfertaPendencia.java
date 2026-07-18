package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "migracao_turma_oferta_pendencia")
public class MigracaoTurmaOfertaPendencia extends PanacheEntity {
    @Column(name = "turma_id")
    public Long turmaId;
    @Column(name = "oferta_id")
    public Long ofertaId;
    @Column(name = "tipo_pendencia", nullable = false)
    public String tipoPendencia;
    @Column(name = "campos_ausentes")
    public String camposAusentes;
    @Column(name = "divergencias")
    public String divergencias;
    @Column(name = "acao_automatica")
    public String acaoAutomatica;
    @Column(name = "motivo_intervencao_manual")
    public String motivoIntervencaoManual;
    public boolean resolvida;
    @Column(name = "data_criacao")
    public LocalDateTime dataCriacao;
    @Column(name = "data_resolucao")
    public LocalDateTime dataResolucao;
}
