package br.edu.sga.security;

import br.edu.sga.service.AuditoriaService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;
import org.jboss.logging.Logger;

@Provider
@Priority(Priorities.USER + 1000)
public class FiltroAuditoria implements ContainerResponseFilter {
    private static final Logger LOG = Logger.getLogger(FiltroAuditoria.class);
    private static final Set<String> METODOS_AUDITADOS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Inject
    AuditoriaService auditoriaService;

    @Override
    public void filter(ContainerRequestContext requisicao, ContainerResponseContext resposta) {
        String metodo = requisicao.getMethod();
        Object usuario = requisicao.getProperty("sub");
        if (!METODOS_AUDITADOS.contains(metodo) || usuario == null) return;

        try {
            auditoriaService.registrar(Long.valueOf(usuario.toString()),
                    valor(requisicao, "nome"), valor(requisicao, "email"), valor(requisicao, "perfil"),
                    acao(metodo, requisicao.getUriInfo().getPath()), metodo,
                    requisicao.getUriInfo().getPath(), resposta.getStatus());
        } catch (RuntimeException erro) {
            LOG.error("Nao foi possivel registrar a auditoria da requisicao", erro);
        }
    }

    private String valor(ContainerRequestContext contexto, String propriedade) {
        Object valor = contexto.getProperty(propriedade);
        return valor == null ? null : valor.toString();
    }

    private String acao(String metodo, String rota) {
        if ("DELETE".equals(metodo)) return "EXCLUSAO";
        if ("PUT".equals(metodo) || "PATCH".equals(metodo)) return "ALTERACAO";
        String caminho = rota.toLowerCase();
        if (caminho.contains("homologar") || caminho.contains("reabrir")
                || caminho.contains("encerrar") || caminho.contains("frequencias")
                || caminho.contains("notas") || caminho.contains("arquivos")) return "ACAO";
        return "INCLUSAO";
    }
}
