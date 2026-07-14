package br.edu.sga.resource;

import br.edu.sga.entity.Aluno;
import br.edu.sga.exception.ApiException;
import br.edu.sga.service.ValidacaoHistoricoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;

@Path("/api/publico/historicos")
@Produces(MediaType.APPLICATION_JSON)
public class HistoricoPublicoResource {
    @Inject ValidacaoHistoricoService validacaoService;

    public record ValidacaoDTO(boolean valido, String aluno, String matricula,
                                String curso, String situacao, Instant emitidoEm) {}

    @GET
    @Path("/{codigo}")
    public ValidacaoDTO validar(@PathParam("codigo") String codigo) {
        var dados = validacaoService.validar(codigo);
        Aluno aluno = Aluno.findById(dados.alunoId());
        if (aluno == null) throw new ApiException(Response.Status.NOT_FOUND, "Historico nao encontrado");
        return new ValidacaoDTO(true, aluno.nome, String.format("SGA-%06d", aluno.id),
                aluno.curso == null ? null : aluno.curso.nome,
                aluno.status == null ? null : aluno.status.name(), dados.emitidoEm());
    }
}
