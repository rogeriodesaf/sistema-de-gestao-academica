package br.edu.sga.dto;

import br.edu.sga.enums.Perfil;
import java.util.List;

public record PerfilPermissoesDTO(Perfil perfil, List<PerfilPermissaoDTO> permissoes) {
}
