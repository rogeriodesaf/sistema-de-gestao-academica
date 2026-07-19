package br.edu.sga.resource;

import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.ResultadoAcademico;
import br.edu.sga.enums.Perfil;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.service.AcademicoService;
import br.edu.sga.service.FrequenciaAcademicaService;
import br.edu.sga.service.ResultadoAcademicoService;
import br.edu.sga.service.PermissaoService;
import br.edu.sga.exception.ApiException;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import java.util.LinkedHashMap;
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
    @Inject
    PermissaoService permissaoService;
    @Context
    ContainerRequestContext contexto;

    @GET
    public List<MatriculaDisciplina> listar() {
        exigirGestao();
        return MatriculaDisciplina.<MatriculaDisciplina>list("order by dataMatricula desc, id desc")
                .stream().map(this::comResultado).toList();
    }

    @GET
    @Path("/{id}")
    public MatriculaDisciplina buscar(@PathParam("id") Long id) {
        exigirGestao();
        MatriculaDisciplina matricula = MatriculaDisciplina.findById(id);
        if (matricula == null) throw new NotFoundException();
        return comResultado(matricula);
    }

    @POST
    public MatriculaDisciplina criar(@Valid MatriculaDisciplina matricula) {
        exigirGestao();
        matricula.notaFinal = null;
        matricula.frequenciaFinal = null;
        matricula.dataConsolidacao = null;
        matricula.status = StatusMatriculaDisciplina.ATIVA;
        matricula.resultadoAcademico = ResultadoAcademico.EM_ANDAMENTO;
        return academicoService.matricularEmDisciplina(matricula);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public MatriculaDisciplina atualizar(@PathParam("id") Long id, @Valid MatriculaDisciplina entrada) {
        exigirGestao();
        MatriculaDisciplina atual = buscar(id);
        if (atual.dataConsolidacao != null) {
            throw new ApiException(Response.Status.CONFLICT, "Matricula consolidada nao pode ser alterada");
        }
        exigirOfertaEditavel(atual);
        if (statusAdministrativo(entrada.status)) atual.status = entrada.status;
        if (entrada.dataMatricula != null) atual.dataMatricula = entrada.dataMatricula;
        atual.observacoes = entrada.observacoes;
        return comResultado(atual);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void excluir(@PathParam("id") Long id) {
        exigirGestao();
        MatriculaDisciplina matricula = buscar(id);
        if (matricula.dataConsolidacao != null) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Matricula consolidada nao pode ser cancelada ou excluida");
        }
        exigirOfertaEditavel(matricula);
        matricula.status = StatusMatriculaDisciplina.CANCELADO;
    }

    @GET
    @Path("/{id}/desempenho")
    public Map<String, Object> desempenho(@PathParam("id") Long id) {
        exigirGestao();
        MatriculaDisciplina matricula = buscar(id);
        var frequencia = frequenciaService.resumirOferta(matricula.ofertaDisciplina).stream()
                .filter(item -> item.matriculaId().equals(matricula.id)).findFirst().orElse(null);
        var resultado = resultadoService.calcularPreliminar(matricula, frequencia);
        HistoricoEscolar historico = historico(matricula);
        var oferta = matricula.ofertaDisciplina;
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("oferta", Map.of(
                "disciplina", oferta.disciplina.nome,
                "turma", oferta.turma == null ? "" : oferta.turma.nome,
                "horario", oferta.horario == null ? "" : oferta.horario));
        dados.put("professor", oferta.professor == null ? "" : oferta.professor.nome);
        dados.put("mediaFinal", resultado.media() == null ? "" : resultado.media());
        dados.put("frequenciaFinal", frequencia == null || frequencia.percentualPresenca() == null
                ? "" : frequencia.percentualPresenca());
        dados.put("resultadoAcademico", matricula.resultadoAcademico.name());
        dados.put("resultadoPreliminar", resultado.situacao());
        dados.put("statusDiario", oferta.status.name());
        dados.put("statusHomologacao", oferta.dataHomologacao == null ? "NAO_HOMOLOGADO" : "HOMOLOGADO");
        dados.put("avaliacoes", resultado.avaliacoes());
        dados.put("historico", historico == null ? Map.of() : Map.of(
                "situacao", historico.situacao == null ? "" : historico.situacao.name(),
                "periodo", historico.periodoCursado == null ? "" : historico.periodoCursado,
                "cargaHoraria", historico.cargaHoraria == null ? 0 : historico.cargaHoraria));
        return dados;
    }

    private boolean statusAdministrativo(StatusMatriculaDisciplina status) {
        return status == StatusMatriculaDisciplina.ATIVA
                || status == StatusMatriculaDisciplina.TRANCADO
                || status == StatusMatriculaDisciplina.CANCELADO;
    }

    private void exigirOfertaEditavel(MatriculaDisciplina matricula) {
        if (!List.of(StatusOfertaDisciplina.PLANEJADA, StatusOfertaDisciplina.ABERTA,
                StatusOfertaDisciplina.EM_ANDAMENTO).contains(matricula.ofertaDisciplina.status)) {
            throw new ApiException(Response.Status.CONFLICT,
                    "A matrícula não pode ser alterada após o encerramento do diário");
        }
    }

    private MatriculaDisciplina comResultado(MatriculaDisciplina matricula) {
        if (matricula.resultadoAcademico == null) matricula.resultadoAcademico = ResultadoAcademico.EM_ANDAMENTO;
        return matricula;
    }

    private HistoricoEscolar historico(MatriculaDisciplina matricula) {
        HistoricoEscolar historico = HistoricoEscolar.find("matriculaDisciplina", matricula).firstResult();
        return historico != null ? historico : HistoricoEscolar.find(
                "aluno = ?1 and ofertaDisciplina = ?2", matricula.aluno, matricula.ofertaDisciplina).firstResult();
    }

    private void exigirGestao() {
        permissaoService.exigir(contexto, Perfil.COORDENADOR, Perfil.SECRETARIA);
    }
}
