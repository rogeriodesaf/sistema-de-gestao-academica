package br.edu.sga.entity;

import br.edu.sga.enums.Perfil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "perfil_permissoes", uniqueConstraints = @UniqueConstraint(columnNames = {"perfil", "recurso"}))
public class PerfilPermissao extends PanacheEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Perfil perfil;

    @Column(nullable = false, length = 80)
    public String area;

    @Column(nullable = false, length = 120)
    public String recurso;

    public boolean visualizar;
    public boolean criar;
    public boolean editar;
    public boolean excluir;
}
