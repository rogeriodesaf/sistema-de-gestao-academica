package br.edu.sga.resource;

import br.edu.sga.dto.RedefinirSenhaDTO;
import br.edu.sga.dto.SenhaProvisoriaDTO;
import br.edu.sga.dto.UsuarioAdminDTO;
import br.edu.sga.dto.UsuarioAtualizacaoDTO;
import br.edu.sga.dto.UsuarioCriacaoDTO;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.Perfil;
import br.edu.sga.exception.ApiException;
import br.edu.sga.security.SenhaService;
import br.edu.sga.service.PermissaoService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.SecureRandom;
import java.util.List;

@Path("/api/usuarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UsuarioResource {
    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#";

    @Inject
    SenhaService senhaService;
    @Inject
    PermissaoService permissaoService;
    @Context
    ContainerRequestContext contexto;

    private final SecureRandom random = new SecureRandom();

    @GET
    public List<UsuarioAdminDTO> listar(@QueryParam("pagina") @DefaultValue("0") int pagina, @QueryParam("tamanho") @DefaultValue("500") int tamanho) {
        exigirAdministrador();
        return Usuario.<Usuario>find("order by nome")
                .page(Math.max(pagina, 0), Math.max(tamanho, 1))
                .list()
                .stream()
                .map(UsuarioAdminDTO::de)
                .toList();
    }

    @GET
    @Path("/{id}")
    public UsuarioAdminDTO buscar(@PathParam("id") Long id) {
        exigirAdministrador();
        return UsuarioAdminDTO.de(buscarUsuario(id));
    }

    @POST
    @Transactional
    public UsuarioAdminDTO criar(@Valid UsuarioCriacaoDTO dto) {
        exigirAdministrador();
        validarSenha(dto.senha(), dto.confirmarSenha());
        if (Usuario.count("lower(email) = ?1", dto.email().toLowerCase()) > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Ja existe usuario com este e-mail");
        }
        Usuario usuario = new Usuario();
        usuario.nome = dto.nome();
        usuario.email = dto.email().toLowerCase();
        usuario.senhaHash = senhaService.criptografar(dto.senha());
        usuario.perfil = dto.perfil();
        usuario.ativo = dto.ativo();
        usuario.observacoes = dto.observacoes();
        usuario.persist();
        return UsuarioAdminDTO.de(usuario);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public UsuarioAdminDTO atualizar(@PathParam("id") Long id, @Valid UsuarioAtualizacaoDTO dto) {
        exigirAdministrador();
        Usuario usuario = buscarUsuario(id);
        String email = dto.email().toLowerCase();
        if (Usuario.count("lower(email) = ?1 and id <> ?2", email, id) > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Ja existe usuario com este e-mail");
        }
        usuario.nome = dto.nome();
        usuario.email = email;
        usuario.perfil = dto.perfil();
        usuario.ativo = dto.ativo();
        usuario.observacoes = dto.observacoes();
        return UsuarioAdminDTO.de(usuario);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public UsuarioAdminDTO inativar(@PathParam("id") Long id) {
        exigirAdministrador();
        Usuario usuario = buscarUsuario(id);
        usuario.ativo = false;
        return UsuarioAdminDTO.de(usuario);
    }

    @PUT
    @Path("/{id}/reativar")
    @Transactional
    public UsuarioAdminDTO reativar(@PathParam("id") Long id) {
        exigirAdministrador();
        Usuario usuario = buscarUsuario(id);
        usuario.ativo = true;
        return UsuarioAdminDTO.de(usuario);
    }

    @PUT
    @Path("/{id}/redefinir-senha")
    @Transactional
    public SenhaProvisoriaDTO redefinirSenha(@PathParam("id") Long id, RedefinirSenhaDTO dto) {
        exigirAdministrador();
        Usuario usuario = buscarUsuario(id);
        String senha = dto != null && dto.gerarAutomatica() ? gerarSenhaProvisoria() : dto == null ? "" : dto.senha();
        String confirmarSenha = dto != null && dto.gerarAutomatica() ? senha : dto == null ? "" : dto.confirmarSenha();
        validarSenha(senha, confirmarSenha);
        usuario.senhaHash = senhaService.criptografar(senha);
        return new SenhaProvisoriaDTO(senha);
    }

    private Usuario buscarUsuario(Long id) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) throw new NotFoundException();
        return usuario;
    }

    private void exigirAdministrador() {
        permissaoService.exigir(contexto, Perfil.ADMINISTRADOR);
    }

    private void validarSenha(String senha, String confirmarSenha) {
        if (senha == null || senha.length() < 8) {
            throw new ApiException(Response.Status.BAD_REQUEST, "A senha deve ter no minimo 8 caracteres");
        }
        if (confirmarSenha == null || !senha.equals(confirmarSenha)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "A confirmacao de senha nao confere");
        }
    }

    private String gerarSenhaProvisoria() {
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            senha.append(CARACTERES.charAt(random.nextInt(CARACTERES.length())));
        }
        return senha.toString();
    }
}
