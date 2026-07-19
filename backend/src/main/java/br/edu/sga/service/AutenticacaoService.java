package br.edu.sga.service;

import br.edu.sga.dto.CadastroUsuarioDTO;
import br.edu.sga.dto.LoginDTO;
import br.edu.sga.dto.TokenDTO;
import br.edu.sga.entity.Usuario;
import br.edu.sga.entity.TokenRedefinicaoSenha;
import br.edu.sga.enums.Perfil;
import br.edu.sga.exception.ApiException;
import br.edu.sga.security.JwtService;
import br.edu.sga.security.SenhaService;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AutenticacaoService {
    private static final Logger LOG = Logger.getLogger(AutenticacaoService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, LocalDateTime> ULTIMAS_SOLICITACOES = new ConcurrentHashMap<>();

    @Inject
    SenhaService senhaService;
    @Inject
    JwtService jwtService;
    @Inject
    Mailer mailer;

    @ConfigProperty(name = "sga.admin.email")
    String adminEmail;
    @ConfigProperty(name = "sga.admin.password")
    String adminPassword;
    @ConfigProperty(name = "sga.frontend.url")
    String frontendUrl;
    @ConfigProperty(name = "sga.reset.expiration-minutes")
    int expiracaoMinutos;
    @ConfigProperty(name = "sga.reset.log-link")
    boolean registrarLink;

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

    @Transactional
    public TokenDTO login(LoginDTO dto) {
        Usuario usuario = Usuario.find("email = ?1 and ativo = true", dto.email()).firstResult();
        if (usuario == null || !senhaService.conferir(dto.senha(), usuario.senhaHash)) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "E-mail ou senha invalidos");
        }
        usuario.ultimoAcesso = LocalDateTime.now();
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

    @Transactional
    public void solicitarRedefinicao(String emailInformado) {
        String email = emailInformado == null ? "" : emailInformado.trim().toLowerCase();
        LocalDateTime agora = LocalDateTime.now();
        ULTIMAS_SOLICITACOES.entrySet().removeIf(entrada -> entrada.getValue().isBefore(agora.minusHours(1)));
        LocalDateTime anterior = ULTIMAS_SOLICITACOES.put(email, agora);
        if (anterior != null && anterior.isAfter(agora.minusMinutes(1))) return;

        Usuario usuario = Usuario.find("lower(email) = ?1 and ativo = true", email).firstResult();
        if (usuario == null) return;

        TokenRedefinicaoSenha.update(
                "situacao = 'INVALIDADO' where usuario = ?1 and situacao = 'ATIVO'", usuario);
        String tokenPuro = gerarToken();
        TokenRedefinicaoSenha token = new TokenRedefinicaoSenha();
        token.usuario = usuario;
        token.tokenHash = hashToken(tokenPuro);
        token.dataCriacao = agora;
        token.dataExpiracao = agora.plusMinutes(expiracaoMinutos);
        token.persist();

        String link = frontendUrl.replaceAll("/+$", "") + "/redefinir-senha?token=" + tokenPuro;
        String html = """
                <h2>Redefinição de senha - SGA</h2>
                <p>Foi solicitada uma redefinição de senha para sua conta.</p>
                <p><a href="%s">Redefinir minha senha</a></p>
                <p>Este link expira em %d minutos e pode ser utilizado apenas uma vez.</p>
                <p>Se você não fez esta solicitação, ignore este e-mail.</p>
                """.formatted(link, expiracaoMinutos);
        try {
            mailer.send(Mail.withHtml(usuario.email, "Redefinição de senha - SGA", html));
            if (registrarLink) LOG.infof("Link de redefinicao para teste local: %s", link);
        } catch (RuntimeException erro) {
            token.situacao = "INVALIDADO";
            LOG.error("Nao foi possivel enviar o e-mail de redefinicao de senha", erro);
        }
    }

    @Transactional(dontRollbackOn = ApiException.class)
    public void redefinirSenha(String tokenPuro, String novaSenha, String confirmacaoSenha) {
        if (novaSenha == null || novaSenha.length() < 8) {
            throw new ApiException(Response.Status.BAD_REQUEST, "A senha deve ter no mínimo 8 caracteres");
        }
        if (confirmacaoSenha == null || !novaSenha.equals(confirmacaoSenha)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "A confirmação de senha não confere");
        }

        TokenRedefinicaoSenha token = TokenRedefinicaoSenha.find("tokenHash", hashToken(tokenPuro)).firstResult();
        if (token == null || "INVALIDADO".equals(token.situacao)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Token de redefinição inválido");
        }
        if ("UTILIZADO".equals(token.situacao)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Este token de redefinição já foi utilizado");
        }
        if (!"ATIVO".equals(token.situacao) || token.dataExpiracao.isBefore(LocalDateTime.now())) {
            token.situacao = "EXPIRADO";
            throw new ApiException(Response.Status.BAD_REQUEST, "O token de redefinição expirou");
        }

        token.usuario.senhaHash = senhaService.criptografar(novaSenha);
        token.situacao = "UTILIZADO";
        token.dataUtilizacao = LocalDateTime.now();
        TokenRedefinicaoSenha.update(
                "situacao = 'INVALIDADO' where usuario = ?1 and situacao = 'ATIVO' and id <> ?2",
                token.usuario, token.id);
    }

    private String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((token == null ? "" : token).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception erro) {
            throw new IllegalStateException("Nao foi possivel proteger o token de redefinicao", erro);
        }
    }
}
