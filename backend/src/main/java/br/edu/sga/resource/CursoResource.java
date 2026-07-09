package br.edu.sga.resource;

import br.edu.sga.entity.Curso;
import br.edu.sga.service.ArquivoPdfService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.File;

@Path("/api/cursos")
public class CursoResource extends CadastroResource.Crud<Curso> {
    @Inject
    ArquivoPdfService arquivoPdfService;

    public CursoResource() {
        super(Curso.class);
    }

    @POST
    @Path("/{id}/grade-pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Curso enviarGrade(@PathParam("id") Long id, @RestForm("arquivo") FileUpload arquivo) {
        Curso curso = Curso.findById(id);
        if (curso == null) throw new NotFoundException();
        arquivoPdfService.remover(curso.gradePdfCaminho);
        var salvo = arquivoPdfService.salvarPdf(arquivo, "grades-curriculares");
        curso.gradePdfCaminho = salvo.caminho();
        curso.gradePdfNome = salvo.nome();
        curso.gradePdfTipo = salvo.tipo();
        curso.gradePdfTamanho = salvo.tamanho();
        return curso;
    }

    @GET
    @Path("/{id}/grade-pdf")
    @Produces("application/pdf")
    public Response baixarGrade(@PathParam("id") Long id) {
        Curso curso = Curso.findById(id);
        if (curso == null || curso.gradePdfCaminho == null) throw new NotFoundException();
        File arquivo = new File(curso.gradePdfCaminho);
        if (!arquivo.exists()) throw new NotFoundException();
        return Response.ok(arquivo, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + curso.gradePdfNome + "\"")
                .build();
    }

    @GET
    @Path("/{id}/grade-pdf/download")
    @Produces("application/pdf")
    public Response baixarGradeComoAnexo(@PathParam("id") Long id) {
        Curso curso = Curso.findById(id);
        if (curso == null || curso.gradePdfCaminho == null) throw new NotFoundException();
        File arquivo = new File(curso.gradePdfCaminho);
        if (!arquivo.exists()) throw new NotFoundException();
        return Response.ok(arquivo, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + curso.gradePdfNome + "\"")
                .build();
    }

    @DELETE
    @Path("/{id}/grade-pdf")
    @Transactional
    public void removerGrade(@PathParam("id") Long id) {
        Curso curso = Curso.findById(id);
        if (curso == null) throw new NotFoundException();
        arquivoPdfService.remover(curso.gradePdfCaminho);
        curso.gradePdfCaminho = null;
        curso.gradePdfNome = null;
        curso.gradePdfTipo = null;
        curso.gradePdfTamanho = null;
    }
}
