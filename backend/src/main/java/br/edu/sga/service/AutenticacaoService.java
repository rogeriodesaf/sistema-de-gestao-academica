package br.edu.sga.service;

import br.edu.sga.dto.CadastroUsuarioDTO;
import br.edu.sga.dto.LoginDTO;
import br.edu.sga.dto.TokenDTO;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.Perfil;
import br.edu.sga.exception.ApiException;
import br.edu.sga.security.JwtService;
import br.edu.sga.security.SenhaService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AutenticacaoService {
    @Inject
    SenhaService senhaService;
    @Inject
    JwtService jwtService;

    @ConfigProperty(name = "sga.admin.email")
    String adminEmail;
    @ConfigProperty(name = "sga.admin.password")
    String adminPassword;

    @Transactional
    void criarAdministradorInicial(@Observes StartupEvent evento) {
        if (Usuario.count("email", adminEmail) == 0) {
            Usuario usuario = new Usuario();
            usuario.nome = "Administrador";
            usuario.email = adminEmail;
            usuario.senhaHash = senhaService.criptografar(adminPassword);
            usuario.perfil = Perfil.ADMINISTRADOR;
            usuario.persist();
        }
    }

    public TokenDTO login(LoginDTO dto) {
        Usuario usuario = Usuario.find("email = ?1 and ativo = true", dto.email()).firstResult();
        if (usuario == null || !senhaService.conferir(dto.senha(), usuario.senhaHash)) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "E-mail ou senha invalidos");
        }
        return new TokenDTO(jwtService.gerar(usuario), usuario.id, usuario.nome, usuario.email, usuario.perfil);
    }

    @Transactional
    public Usuario cadastrar(CadastroUsuarioDTO dto) {
        if (Usuario.count("email", dto.email()) > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Ja existe usuario com este e-mail");
        }
        Usuario usuario = new Usuario();
        usuario.nome = dto.nome();
        usuario.email = dto.email();
        usuario.senhaHash = senhaService.criptografar(dto.senha());
        usuario.perfil = dto.perfil();
        usuario.persist();
        return usuario;
    }
}
