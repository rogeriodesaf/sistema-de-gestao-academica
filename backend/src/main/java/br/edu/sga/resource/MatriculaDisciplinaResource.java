package br.edu.sga.resource;

import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.service.AcademicoService;
import br.edu.sga.service.FrequenciaAcademicaService;
import br.edu.sga.service.ResultadoAcademicoService;
import br.edu.sga.exception.ApiException;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/api/matriculas-disciplinas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MatriculaDisciplinaResource {
    @Inject
    AcademicoService academicoService;
    @Inject
    FrequenciaAcademicaService frequenciaService;
    @Inject
    ResultadoAcademicoService resultadoService;

    @GET
    public List<MatriculaDisciplina> listar() {
        return MatriculaDisciplina.<MatriculaDisciplina>list("order by dataMatricula desc, id desc")
                .stream().map(this::comResultado).toList();
    }

    @GET
    @Path("/{id}")
    public MatriculaDisciplina buscar(@PathParam("id") Long id) {
        MatriculaDisciplina matricula = MatriculaDisciplina.findById(id);
        if (matricula == null) throw new NotFoundException();
        return comResultado(matricula);
    }

    @POST
    public MatriculaDisciplina criar(@Valid MatriculaDisciplina matricula) {
        matricula.notaFinal = null;
        matricula.frequenciaFinal = null;
        matricula.dataConsolidacao = null;
        matricula.status = StatusMatriculaDisciplina.ATIVA;
        return academicoService.matricularEmDisciplina(matricula);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public MatriculaDisciplina atualizar(@PathParam("id") Long id, @Valid MatriculaDisciplina entrada) {
        MatriculaDisciplina atual = buscar(id);
        if (atual.dataConsolidacao != null) {
            throw new ApiException(Response.Status.CONFLICT, "Matricula consolidada nao pode ser alterada");
        }
        if (statusAdministrativo(entrada.status)) atual.status = entrada.status;
        if (entrada.dataMatricula != null) atual.dataMatricula = entrada.dataMatricula;
        atual.observacoes = entrada.observacoes;
        return comResultado(atual);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void excluir(@PathParam("id") Long id) {
        MatriculaDisciplina matricula = buscar(id);
        if (matricula.dataConsolidacao != null) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Matricula consolidada nao pode ser cancelada ou excluida");
        }
        matricula.status = StatusMatriculaDisciplina.CANCELADO;
    }

    @GET
    @Path("/{id}/desempenho")
    public Map<String, Object> desempenho(@PathParam("id") Long id) {
        MatriculaDisciplina matricula = buscar(id);
        var frequencia = frequenciaService.resumirOferta(matricula.ofertaDisciplina).stream()
                .filter(item -> item.matriculaId().equals(matricula.id)).findFirst().orElse(null);
        var resultado = resultadoService.calcular(matricula, frequencia);
        HistoricoEscolar historico = historico(matricula);
        return Map.of(
                "mediaFinal", resultado.media() == null ? "" : resultado.media(),
                "frequenciaFinal", frequencia == null || frequencia.percentualPresenca() == null ? "" : frequencia.percentualPresenca(),
                "situacao", resultado.situacao(),
                "avaliacoes", resultado.avaliacoes(),
                "historico", historico == null ? Map.of() : Map.of(
                        "situacao", historico.situacao.name(),
                        "periodo", historico.periodoCursado == null ? "" : historico.periodoCursado,
                        "cargaHoraria", historico.cargaHoraria == null ? 0 : historico.cargaHoraria));
    }

    private boolean statusAdministrativo(StatusMatriculaDisciplina status) {
        return status == StatusMatriculaDisciplina.ATIVA
                || status == StatusMatriculaDisciplina.MATRICULADO
                || status == StatusMatriculaDisciplina.TRANCADO
                || status == StatusMatriculaDisciplina.CANCELADO;
    }

    private MatriculaDisciplina comResultado(MatriculaDisciplina matricula) {
        HistoricoEscolar historico = historico(matricula);
        if (historico == null || historico.situacao == null) {
            matricula.resultadoAcademico = "EM_ANDAMENTO";
        } else {
            matricula.resultadoAcademico = switch (historico.situacao) {
                case APROVADO -> "APROVADO";
                case REPROVADO_POR_FREQUENCIA -> "REPROVADO_POR_FREQUENCIA";
                case REPROVADO, REPROVADO_POR_NOTA -> "REPROVADO_POR_NOTA";
                default -> "EM_ANDAMENTO";
            };
        }
        return matricula;
    }

    private HistoricoEscolar historico(MatriculaDisciplina matricula) {
        HistoricoEscolar historico = HistoricoEscolar.find("matriculaDisciplina", matricula).firstResult();
        return historico != null ? historico : HistoricoEscolar.find(
                "aluno = ?1 and ofertaDisciplina = ?2", matricula.aluno, matricula.ofertaDisciplina).firstResult();
    }
}
