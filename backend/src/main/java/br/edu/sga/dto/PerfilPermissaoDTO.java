package br.edu.sga.dto;

import br.edu.sga.entity.PerfilPermissao;

public record PerfilPermissaoDTO(
        Long id,
        String area,
        String recurso,
        boolean visualizar,
        boolean criar,
        boolean editar,
        boolean excluir
) {
    public static PerfilPermissaoDTO de(PerfilPermissao permissao) {
        return new PerfilPermissaoDTO(
                permissao.id,
                permissao.area,
                permissao.recurso,
                permissao.visualizar,
                permissao.criar,
                permissao.editar,
                permissao.excluir
        );
    }
}
