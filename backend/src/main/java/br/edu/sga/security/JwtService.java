package br.edu.sga.security;

import br.edu.sga.entity.Usuario;
import br.edu.sga.exception.ApiException;
import io.smallrye.config.ConfigMapping;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class JwtService {
    @ConfigMapping(prefix = "sga.jwt")
    public interface JwtConfig {
        String secret();
        Long expirationSeconds();
    }

    @Inject
    JwtConfig config;
    @Inject
    ObjectMapper mapper;

    public String gerar(Usuario usuario) {
        try {
            String header = base64(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = base64(mapper.writeValueAsBytes(Map.of(
                    "sub", usuario.id.toString(),
                    "nome", usuario.nome,
                    "email", usuario.email,
                    "perfil", usuario.perfil.name(),
                    "exp", Instant.now().plusSeconds(config.expirationSeconds()).getEpochSecond()
            )));
            String assinatura = assinar(header + "." + payload);
            return header + "." + payload + "." + assinatura;
        } catch (Exception e) {
            throw new ApiException(Response.Status.INTERNAL_SERVER_ERROR, "Nao foi possivel gerar token");
        }
    }

    public Map<String, Object> validar(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3 || !assinar(partes[0] + "." + partes[1]).equals(partes[2])) {
                throw new IllegalArgumentException();
            }
            Map<String, Object> payload = mapper.readValue(Base64.getUrlDecoder().decode(partes[1]), Map.class);
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new IllegalArgumentException();
            }
            return payload;
        } catch (Exception e) {
            throw new ApiException(Response.Status.UNAUTHORIZED, "Token invalido ou expirado");
        }
    }

    private String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String assinar(String conteudo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(config.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
    }
}
