package br.edu.sga.resource;

import br.edu.sga.entity.Disciplina;
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

@Path("/api/disciplinas")
public class DisciplinaResource extends CadastroResource.Crud<Disciplina> {
    @Inject
    ArquivoPdfService arquivoPdfService;

    public DisciplinaResource() {
        super(Disciplina.class);
    }

    @POST
    @Path("/{id}/ementa-pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Disciplina enviarEmenta(@PathParam("id") Long id, @RestForm("arquivo") FileUpload arquivo) {
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

    @DELETE
    @Path("/{id}/ementa-pdf")
    @Transactional
    public void removerEmenta(@PathParam("id") Long id) {
        Disciplina disciplina = Disciplina.findById(id);
        if (disciplina == null) throw new NotFoundException();
        arquivoPdfService.remover(disciplina.ementaPdfCaminho);
        disciplina.ementaPdfCaminho = null;
        disciplina.ementaPdfNome = null;
        disciplina.ementaPdfTipo = null;
        disciplina.ementaPdfTamanho = null;
    }
}
