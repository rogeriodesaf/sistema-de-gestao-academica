package br.edu.sga.dto;

import br.edu.sga.enums.Perfil;

public record PerfilResumoDTO(Perfil codigo, String nome, String descricao) {
}
