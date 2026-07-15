package br.edu.sga.resource;

import br.edu.sga.dto.CadastroUsuarioDTO;
import br.edu.sga.dto.LoginDTO;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.Perfil;
import br.edu.sga.service.AutenticacaoService;
import br.edu.sga.service.PermissaoService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    public record EsqueciSenhaDTO(@Email @NotBlank String email) {}
    public record RedefinirSenhaDTO(@NotBlank String token, @NotBlank String novaSenha,
                                    @NotBlank String confirmacaoSenha) {}

    @Inject AutenticacaoService autenticacaoService;
    @Inject PermissaoService permissaoService;
    @Context ContainerRequestContext contexto;

    @POST
    @Path("/login")
    public Object login(@Valid LoginDTO dto) {
        return autenticacaoService.login(dto);
    }

    @POST
    @Path("/esqueci-senha")
    public Map<String, String> esqueciSenha(@Valid EsqueciSenhaDTO dto) {
        autenticacaoService.solicitarRedefinicao(dto.email());
        return Map.of("mensagem",
                "Se o e-mail estiver cadastrado, voce recebera as instrucoes para redefinir sua senha.");
    }

    @POST
    @Path("/redefinir-senha")
    public Map<String, String> redefinirSenha(@Valid RedefinirSenhaDTO dto) {
        autenticacaoService.redefinirSenha(dto.token(), dto.novaSenha(), dto.confirmacaoSenha());
        return Map.of("mensagem", "Senha redefinida com sucesso. Entre novamente com a nova senha.");
    }

    @POST
    @Path("/usuarios")
    public Usuario cadastrar(@Valid CadastroUsuarioDTO dto) {
        permissaoService.exigir(contexto, Perfil.COORDENADOR, Perfil.SECRETARIA);
        return autenticacaoService.cadastrar(dto);
    }
}
