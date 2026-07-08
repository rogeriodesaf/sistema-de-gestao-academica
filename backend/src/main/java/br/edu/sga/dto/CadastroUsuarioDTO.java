package br.edu.sga.dto;

import br.edu.sga.enums.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CadastroUsuarioDTO(@NotBlank String nome, @Email String email, @NotBlank String senha, @NotNull Perfil perfil) {
}
