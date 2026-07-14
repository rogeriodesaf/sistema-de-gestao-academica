package br.edu.sga.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class FiltroJwt implements ContainerRequestFilter {
    private static final Set<String> PUBLICOS = Set.of("api/auth/login", "api/status", "api/publico/historicos", "q/openapi", "q/swagger-ui");

    @Inject
    JwtService jwtService;

    @Override
    public void filter(ContainerRequestContext contexto) throws IOException {
        String caminho = contexto.getUriInfo().getPath();
        if (caminho.startsWith("/")) {
            caminho = caminho.substring(1);
        }
        for (String publico : PUBLICOS) {
            if (caminho.startsWith(publico)) return;
        }
        if ("OPTIONS".equals(contexto.getMethod())) return;
        String authorization = contexto.getHeaderString("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new br.edu.sga.exception.ApiException(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED, "Token ausente");
        }
        var dados = jwtService.validar(authorization.substring("Bearer ".length()));
        dados.forEach(contexto::setProperty);
        if ("ALUNO".equals(String.valueOf(dados.get("perfil"))) && !caminho.startsWith("api/aluno")) {
            throw new br.edu.sga.exception.ApiException(jakarta.ws.rs.core.Response.Status.FORBIDDEN,
                    "Perfil de aluno restrito ao portal academico");
        }
    }
}
