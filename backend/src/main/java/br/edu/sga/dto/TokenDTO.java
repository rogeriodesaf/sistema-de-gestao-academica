package br.edu.sga.dto;

import br.edu.sga.enums.Perfil;

public record TokenDTO(String token, Long usuarioId, String nome, String email, Perfil perfil) {
}
