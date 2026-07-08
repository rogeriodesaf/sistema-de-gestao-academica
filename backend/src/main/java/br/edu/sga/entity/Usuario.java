package br.edu.sga.entity;

import br.edu.sga.enums.Perfil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "usuarios")
public class Usuario extends PanacheEntity {
    @NotBlank
    public String nome;

    @Email
    @Column(nullable = false, unique = true)
    public String email;

    @Column(name = "senha_hash", nullable = false)
    public String senhaHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    public Perfil perfil;

    public boolean ativo = true;
}
