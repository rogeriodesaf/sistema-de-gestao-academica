package br.edu.sga.resource;

import br.edu.sga.dto.AtualizarPerfilPermissoesDTO;
import br.edu.sga.dto.PerfilPermissoesDTO;
import br.edu.sga.dto.PerfilResumoDTO;
import br.edu.sga.enums.Perfil;
import br.edu.sga.service.PerfilPermissaoService;
import br.edu.sga.service.PermissaoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/perfis")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PerfilResource {
    @Inject
    PerfilPermissaoService perfilPermissaoService;
    @Inject
    PermissaoService permissaoService;
    @Context
    ContainerRequestContext contexto;

    @GET
    public List<PerfilResumoDTO> listar() {
        exigirAdministrador();
        return perfilPermissaoService.listarPerfis();
    }

    @GET
    @Path("/{perfil}/permissoes")
    public PerfilPermissoesDTO permissoes(@PathParam("perfil") Perfil perfil) {
        exigirAdministrador();
        return perfilPermissaoService.permissoes(perfil);
    }

    @PUT
    @Path("/{perfil}/permissoes")
    public PerfilPermissoesDTO atualizar(@PathParam("perfil") Perfil perfil, AtualizarPerfilPermissoesDTO dto) {
        exigirAdministrador();
        return perfilPermissaoService.atualizar(perfil, dto);
    }

    private void exigirAdministrador() {
        permissaoService.exigir(contexto, Perfil.ADMINISTRADOR);
    }
}
