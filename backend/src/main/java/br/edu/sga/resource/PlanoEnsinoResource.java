package br.edu.sga.resource;

import br.edu.sga.entity.PlanoEnsino;
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
import java.time.LocalDateTime;

@Path("/api/planos-ensino")
public class PlanoEnsinoResource extends CadastroResource.Crud<PlanoEnsino> {
    @Inject
    ArquivoPdfService arquivoPdfService;

    public PlanoEnsinoResource() {
        super(PlanoEnsino.class);
    }

    @POST
    @Path("/{id}/plano-pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public PlanoEnsino enviarPlano(@PathParam("id") Long id, @RestForm("arquivo") FileUpload arquivo) {
        PlanoEnsino plano = PlanoEnsino.findById(id);
        if (plano == null) throw new NotFoundException();
        arquivoPdfService.remover(plano.planoPdfCaminho);
        var salvo = arquivoPdfService.salvarPdf(arquivo, "planos-ensino");
        plano.planoPdfCaminho = salvo.caminho();
        plano.planoPdfNome = salvo.nome();
        plano.planoPdfTipo = salvo.tipo();
        plano.planoPdfTamanho = salvo.tamanho();
        plano.ultimaAtualizacao = LocalDateTime.now();
        return plano;
    }

    @GET
    @Path("/{id}/plano-pdf")
    @Produces("application/pdf")
    public Response baixarPlano(@PathParam("id") Long id) {
        PlanoEnsino plano = PlanoEnsino.findById(id);
        if (plano == null || plano.planoPdfCaminho == null) throw new NotFoundException();
        File arquivo = new File(plano.planoPdfCaminho);
        if (!arquivo.exists()) throw new NotFoundException();
        return Response.ok(arquivo, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + plano.planoPdfNome + "\"")
                .build();
    }

    @GET
    @Path("/{id}/plano-pdf/download")
    @Produces("application/pdf")
    public Response baixarPlanoComoAnexo(@PathParam("id") Long id) {
        PlanoEnsino plano = PlanoEnsino.findById(id);
        if (plano == null || plano.planoPdfCaminho == null) throw new NotFoundException();
        File arquivo = new File(plano.planoPdfCaminho);
        if (!arquivo.exists()) throw new NotFoundException();
        return Response.ok(arquivo, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + plano.planoPdfNome + "\"")
                .build();
    }

    @DELETE
    @Path("/{id}/plano-pdf")
    @Transactional
    public void removerPlano(@PathParam("id") Long id) {
        PlanoEnsino plano = PlanoEnsino.findById(id);
        if (plano == null) throw new NotFoundException();
        arquivoPdfService.remover(plano.planoPdfCaminho);
        plano.planoPdfCaminho = null;
        plano.planoPdfNome = null;
        plano.planoPdfTipo = null;
        plano.planoPdfTamanho = null;
        plano.ultimaAtualizacao = LocalDateTime.now();
    }
}
