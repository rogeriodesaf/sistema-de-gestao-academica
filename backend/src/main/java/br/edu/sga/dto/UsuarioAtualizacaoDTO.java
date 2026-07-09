package br.edu.sga.dto;

import br.edu.sga.enums.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioAtualizacaoDTO(
        @NotBlank String nome,
        @Email @NotBlank String email,
        @NotNull Perfil perfil,
        boolean ativo,
        String observacoes
) {
}
