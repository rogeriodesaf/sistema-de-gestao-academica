package br.edu.sga.resource;

import br.edu.sga.entity.LogAuditoria;
import br.edu.sga.enums.Perfil;
import br.edu.sga.service.PermissaoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;

@Path("/api/auditoria")
@Produces(MediaType.APPLICATION_JSON)
public class AuditoriaResource {
    @Inject
    PermissaoService permissaoService;
    @Context
    ContainerRequestContext contexto;

    public record LogAuditoriaDTO(Long id, LocalDateTime dataHora, String usuarioNome,
                                  String usuarioEmail, String perfil, String acao,
                                  String metodo, String rota, Integer statusHttp, boolean sucesso) {}

    @GET
    public List<LogAuditoriaDTO> listar(@QueryParam("pagina") @DefaultValue("0") int pagina,
                                        @QueryParam("tamanho") @DefaultValue("500") int tamanho) {
        permissaoService.exigir(contexto, Perfil.ADMINISTRADOR);
        return LogAuditoria.<LogAuditoria>find("order by dataHora desc")
                .page(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 500))
                .list().stream().map(this::dto).toList();
    }

    private LogAuditoriaDTO dto(LogAuditoria log) {
        return new LogAuditoriaDTO(log.id, log.dataHora, log.usuarioNome, log.usuarioEmail,
                log.perfil, log.acao, log.metodo, log.rota, log.statusHttp, log.sucesso);
    }
}
