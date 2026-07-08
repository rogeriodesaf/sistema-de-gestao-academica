package br.edu.sga.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@ApplicationScoped
public class SenhaService {
    private static final SecureRandom RANDOM = new SecureRandom();

    public String criptografar(String senha) {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal) + ":" + hash(senha, sal);
    }

    public boolean conferir(String senha, String senhaHash) {
        String[] partes = senhaHash.split(":");
        if (partes.length != 2) {
            return false;
        }
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        return hash(senha, sal).equals(partes[1]);
    }

    private String hash(String senha, byte[] sal) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), sal, 120000, 256);
            byte[] bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel criptografar a senha", e);
        }
    }
}
