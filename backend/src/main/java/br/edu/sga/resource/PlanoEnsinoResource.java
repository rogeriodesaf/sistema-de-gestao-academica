package br.edu.sga.resource;

import br.edu.sga.entity.PlanoEnsino;
import br.edu.sga.entity.Disciplina;
import br.edu.sga.exception.ApiException;
import br.edu.sga.service.ArquivoPdfService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Path("/api/planos-ensino")
public class PlanoEnsinoResource extends CadastroResource.Crud<PlanoEnsino> {
    @Inject
    ArquivoPdfService arquivoPdfService;

    public PlanoEnsinoResource() {
        super(PlanoEnsino.class);
    }

    @GET
    @Override
    public List<PlanoEnsino> listar(@QueryParam("pagina") @DefaultValue("0") int pagina,
                                    @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        return getEntityManager().createQuery("from PlanoEnsino order by disciplina.nome", PlanoEnsino.class)
                .setFirstResult(Math.max(pagina, 0) * Math.max(tamanho, 1))
                .setMaxResults(Math.max(tamanho, 1)).getResultList();
    }

    @POST
    @Transactional
    @Override
    public PlanoEnsino criar(PlanoEnsino plano) {
        plano.disciplina = disciplinaValida(plano, null);
        plano.ofertaDisciplina = null;
        plano.turma = null;
        plano.dataCadastro = LocalDateTime.now();
        plano.ultimaAtualizacao = LocalDateTime.now();
        return super.criar(plano);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Override
    public PlanoEnsino atualizar(@PathParam("id") Long id, PlanoEnsino entrada) {
        exigirGestaoAcademica();
        PlanoEnsino plano = buscar(id);
        plano.disciplina = disciplinaValida(entrada, id);
        plano.ofertaDisciplina = null;
        plano.turma = null;
        plano.objetivos = entrada.objetivos;
        plano.conteudoProgramatico = entrada.conteudoProgramatico;
        plano.metodologia = entrada.metodologia;
        plano.bibliografiaBasica = entrada.bibliografiaBasica;
        plano.bibliografiaComplementar = entrada.bibliografiaComplementar;
        plano.observacoes = entrada.observacoes;
        plano.ultimaAtualizacao = LocalDateTime.now();
        return plano;
    }

    @POST
    @Path("/{id}/ementa-pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public PlanoEnsino enviarEmenta(@PathParam("id") Long id, @RestForm("arquivo") FileUpload arquivo) {
        exigirGestaoAcademica();
        PlanoEnsino plano = PlanoEnsino.findById(id);
        if (plano == null) throw new NotFoundException();
        var salvo = arquivoPdfService.salvarPdf(arquivo, "ementas-planos-ensino");
        arquivoPdfService.remover(plano.planoPdfCaminho);
        plano.planoPdfCaminho = salvo.caminho();
        plano.planoPdfNome = salvo.nome();
        plano.planoPdfTipo = salvo.tipo();
        plano.planoPdfTamanho = salvo.tamanho();
        plano.ultimaAtualizacao = LocalDateTime.now();
        return plano;
    }

    @GET
    @Path("/{id}/ementa-pdf")
    @Produces("application/pdf")
    public Response abrirEmenta(@PathParam("id") Long id) {
        PlanoEnsino plano = PlanoEnsino.findById(id);
        if (plano == null || plano.planoPdfCaminho == null) throw new NotFoundException();
        File arquivo = new File(plano.planoPdfCaminho);
        if (!arquivo.exists()) throw new NotFoundException();
        return Response.ok(arquivo, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + plano.planoPdfNome + "\"")
                .build();
    }

    @GET
    @Path("/{id}/ementa-pdf/download")
    @Produces("application/pdf")
    public Response baixarEmenta(@PathParam("id") Long id) {
        PlanoEnsino plano = PlanoEnsino.findById(id);
        if (plano == null || plano.planoPdfCaminho == null) throw new NotFoundException();
        File arquivo = new File(plano.planoPdfCaminho);
        if (!arquivo.exists()) throw new NotFoundException();
        return Response.ok(arquivo, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + plano.planoPdfNome + "\"")
                .build();
    }

    @DELETE
    @Path("/{id}/ementa-pdf")
    @Transactional
    public void removerEmenta(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        PlanoEnsino plano = PlanoEnsino.findById(id);
        if (plano == null) throw new NotFoundException();
        arquivoPdfService.remover(plano.planoPdfCaminho);
        plano.planoPdfCaminho = null;
        plano.planoPdfNome = null;
        plano.planoPdfTipo = null;
        plano.planoPdfTamanho = null;
        plano.ultimaAtualizacao = LocalDateTime.now();
    }

    private Disciplina disciplinaValida(PlanoEnsino plano, Long idAtual) {
        if (plano == null || plano.disciplina == null || plano.disciplina.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Disciplina obrigatoria");
        }
        Disciplina disciplina = Disciplina.findById(plano.disciplina.id);
        if (disciplina == null) throw new ApiException(Response.Status.BAD_REQUEST, "Disciplina nao encontrada");
        long existentes = idAtual == null
                ? PlanoEnsino.count("disciplina", disciplina)
                : PlanoEnsino.count("disciplina = ?1 and id <> ?2", disciplina, idAtual);
        if (existentes > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Esta disciplina ja possui Plano de Ensino");
        }
        return disciplina;
    }
}
