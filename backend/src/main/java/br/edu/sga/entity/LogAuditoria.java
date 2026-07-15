package br.edu.sga.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs_auditoria")
public class LogAuditoria extends PanacheEntity {
    @Column(name = "usuario_id")
    public Long usuarioId;
    @Column(name = "usuario_nome")
    public String usuarioNome;
    @Column(name = "usuario_email")
    public String usuarioEmail;
    public String perfil;
    public String acao;
    public String metodo;
    public String rota;
    @Column(name = "status_http")
    public Integer statusHttp;
    public boolean sucesso;
    @Column(name = "data_hora")
    public LocalDateTime dataHora;

    @PrePersist
    void antesDePersistir() {
        if (dataHora == null) dataHora = LocalDateTime.now();
    }
}
