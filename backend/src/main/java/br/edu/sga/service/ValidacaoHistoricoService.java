package br.edu.sga.service;

import br.edu.sga.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ValidacaoHistoricoService {
    @ConfigProperty(name = "sga.jwt.secret")
    String segredo;

    public record Dados(Long alunoId, Instant emitidoEm) {}

    public String emitir(Long alunoId, Instant emitidoEm) {
        String corpo = alunoId + ":" + emitidoEm.toEpochMilli();
        return codificar(corpo.getBytes(StandardCharsets.UTF_8)) + "." + codificar(assinar(corpo));
    }

    public Dados validar(String codigo) {
        try {
            String[] partes = codigo.split("\\.");
            if (partes.length != 2) throw new IllegalArgumentException();
            String corpo = new String(Base64.getUrlDecoder().decode(partes[0]), StandardCharsets.UTF_8);
            if (!java.security.MessageDigest.isEqual(assinar(corpo), Base64.getUrlDecoder().decode(partes[1]))) {
                throw new IllegalArgumentException();
            }
            String[] dados = corpo.split(":");
            return new Dados(Long.valueOf(dados[0]), Instant.ofEpochMilli(Long.parseLong(dados[1])));
        } catch (RuntimeException e) {
            throw new ApiException(Response.Status.NOT_FOUND, "Codigo de validacao invalido");
        }
    }

    private byte[] assinar(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(valor.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar historico", e);
        }
    }

    private String codificar(byte[] valor) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(valor);
    }
}
