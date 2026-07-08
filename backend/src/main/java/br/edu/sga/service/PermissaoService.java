package br.edu.sga.service;

import br.edu.sga.enums.Perfil;
import br.edu.sga.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;

@ApplicationScoped
public class PermissaoService {
    public Perfil perfil(ContainerRequestContext contexto) {
        Object valor = contexto.getProperty("perfil");
        return valor == null ? null : Perfil.valueOf(valor.toString());
    }

    public Long usuarioId(ContainerRequestContext contexto) {
        Object valor = contexto.getProperty("sub");
        return valor == null ? null : Long.valueOf(valor.toString());
    }

    public void exigir(ContainerRequestContext contexto, Perfil... perfis) {
        Perfil atual = perfil(contexto);
        if (atual == Perfil.ADMINISTRADOR || Arrays.asList(perfis).contains(atual)) {
            return;
        }
        throw new ApiException(Response.Status.FORBIDDEN, "Perfil sem permissao para esta operacao");
    }
}
