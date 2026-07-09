package br.edu.sga.dto;

import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.Perfil;
import java.time.LocalDateTime;

public record UsuarioAdminDTO(
        Long id,
        String nome,
        String email,
        Perfil perfil,
        boolean ativo,
        LocalDateTime dataCriacao,
        LocalDateTime ultimoAcesso,
        String observacoes
) {
    public static UsuarioAdminDTO de(Usuario usuario) {
        return new UsuarioAdminDTO(
                usuario.id,
                usuario.nome,
                usuario.email,
                usuario.perfil,
                usuario.ativo,
                usuario.dataCriacao,
                usuario.ultimoAcesso,
                usuario.observacoes
        );
    }
}
