package br.edu.sga.resource;

import br.edu.sga.dto.CadastroUsuarioDTO;
import br.edu.sga.dto.LoginDTO;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.Perfil;
import br.edu.sga.service.AutenticacaoService;
import br.edu.sga.service.PermissaoService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject AutenticacaoService autenticacaoService;
    @Inject PermissaoService permissaoService;
    @Context ContainerRequestContext contexto;

    @POST
    @Path("/login")
    public Object login(@Valid LoginDTO dto) {
        return autenticacaoService.login(dto);
    }

    @POST
    @Path("/usuarios")
    public Usuario cadastrar(@Valid CadastroUsuarioDTO dto) {
        permissaoService.exigir(contexto, Perfil.COORDENADOR, Perfil.SECRETARIA);
        return autenticacaoService.cadastrar(dto);
    }
}
