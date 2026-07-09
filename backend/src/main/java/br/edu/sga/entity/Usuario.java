package br.edu.sga.entity;

import br.edu.sga.enums.Perfil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

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

    @Column(name = "data_criacao")
    public LocalDateTime dataCriacao;

    @Column(name = "ultimo_acesso")
    public LocalDateTime ultimoAcesso;

    @Column(length = 2000)
    public String observacoes;

    @PrePersist
    void antesDePersistir() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
