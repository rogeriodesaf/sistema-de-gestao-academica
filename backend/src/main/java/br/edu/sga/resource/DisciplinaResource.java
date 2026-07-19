package br.edu.sga.resource;

import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.Matricula;
import br.edu.sga.entity.Nota;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.PlanoEnsino;
import br.edu.sga.entity.Turma;
import br.edu.sga.entity.VinculoProfessorDisciplinaTurma;
import br.edu.sga.exception.ApiException;
import br.edu.sga.service.ArquivoPdfService;
import br.edu.sga.service.IntegralizacaoCursoService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.File;

@Path("/api/disciplinas")
public class DisciplinaResource extends CadastroResource.Crud<Disciplina> {
    @Inject
    ArquivoPdfService arquivoPdfService;
    @Inject
    IntegralizacaoCursoService integralizacaoCursoService;

    public DisciplinaResource() {
        super(Disciplina.class);
    }

    @POST
    @Transactional
    @Override
    public Disciplina criar(@Valid Disciplina disciplina) {
        Disciplina salva = super.criar(disciplina);
        integralizacaoCursoService.recalcularCurso(salva.curso);
        return salva;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Override
    public Disciplina atualizar(@PathParam("id") Long id, @Valid Disciplina disciplina) {
        var cursoAnterior = buscar(id).curso;
        Disciplina salva = super.atualizar(id, disciplina);
        integralizacaoCursoService.recalcularCurso(cursoAnterior);
        if (salva.curso != null && (cursoAnterior == null || !salva.curso.id.equals(cursoAnterior.id))) {
            integralizacaoCursoService.recalcularCurso(salva.curso);
        }
        return salva;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @Override
    public void excluir(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        Disciplina disciplina = buscar(id);
        if (disciplina.curso != null || disciplina.modulo != null || disciplina.moduloOriginal != null
                || OfertaDisciplina.count("disciplina", disciplina) > 0 || PlanoEnsino.count("disciplina", disciplina) > 0
                || HistoricoEscolar.count("disciplina", disciplina) > 0 || Matricula.count("disciplina", disciplina) > 0
                || Nota.count("disciplina", disciplina) > 0 || AulaMinistrada.count("disciplina", disciplina) > 0
                || Turma.count("disciplina", disciplina) > 0
                || VinculoProfessorDisciplinaTurma.count("disciplina", disciplina) > 0) {
            throw new ApiException(Response.Status.CONFLICT,
                    "A disciplina possui vínculos acadêmicos e não pode ser excluída; utilize a inativação");
        }
        var curso = disciplina.curso;
        getEntityManager().remove(disciplina);
        integralizacaoCursoService.recalcularCurso(curso);
    }

    @POST
    @Path("/{id}/ementa-pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Disciplina enviarEmenta(@PathParam("id") Long id, @RestForm("arquivo") FileUpload arquivo) {
        exigirGestaoAcademica();
        Disciplina disciplina = Disciplina.findById(id);
        if (disciplina == null) throw new NotFoundException();
        arquivoPdfService.remover(disciplina.ementaPdfCaminho);
        var salvo = arquivoPdfService.salvarPdf(arquivo, "ementas");
        disciplina.ementaPdfCaminho = salvo.caminho();
        disciplina.ementaPdfNome = salvo.nome();
        disciplina.ementaPdfTipo = salvo.tipo();
        disciplina.ementaPdfTamanho = salvo.tamanho();
        return disciplina;
    }

    @GET
    @Path("/{id}/ementa-pdf")
    @Produces("application/pdf")
    public Response baixarEmenta(@PathParam("id") Long id) {
        Disciplina disciplina = Disciplina.findById(id);
        if (disciplina == null || disciplina.ementaPdfCaminho == null) throw new NotFoundException();
        File arquivo = new File(disciplina.ementaPdfCaminho);
        if (!arquivo.exists()) throw new NotFoundException();
        return Response.ok(arquivo, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + disciplina.ementaPdfNome + "\"")
                .build();
    }

    @GET
    @Path("/{id}/ementa-pdf/download")
    @Produces("application/pdf")
    public Response baixarEmentaComoAnexo(@PathParam("id") Long id) {
        Disciplina disciplina = Disciplina.findById(id);
        if (disciplina == null || disciplina.ementaPdfCaminho == null) throw new NotFoundException();
        File arquivo = new File(disciplina.ementaPdfCaminho);
        if (!arquivo.exists()) throw new NotFoundException();
        return Response.ok(arquivo, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + disciplina.ementaPdfNome + "\"")
                .build();
    }

    @DELETE
    @Path("/{id}/ementa-pdf")
    @Transactional
    public void removerEmenta(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        Disciplina disciplina = Disciplina.findById(id);
        if (disciplina == null) throw new NotFoundException();
        arquivoPdfService.remover(disciplina.ementaPdfCaminho);
        disciplina.ementaPdfCaminho = null;
        disciplina.ementaPdfNome = null;
        disciplina.ementaPdfTipo = null;
        disciplina.ementaPdfTamanho = null;
    }
}
