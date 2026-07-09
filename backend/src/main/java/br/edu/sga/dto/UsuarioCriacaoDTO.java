package br.edu.sga.dto;

import br.edu.sga.enums.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioCriacaoDTO(
        @NotBlank String nome,
        @Email @NotBlank String email,
        @NotBlank String senha,
        @NotBlank String confirmarSenha,
        @NotNull Perfil perfil,
        boolean ativo,
        String observacoes
) {
}
