package br.edu.sga.resource;

import br.edu.sga.entity.Curso;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.File;

@Path("/api/publico/cursos")
public class CursoPublicoResource {
    @GET
    @Path("/{id}/grade-pdf")
    @Produces("application/pdf")
    public Response abrirGrade(@PathParam("id") Long id) {
        Curso curso = Curso.findById(id);
        if (curso == null || curso.gradePdfCaminho == null) throw new NotFoundException();
        File arquivo = new File(curso.gradePdfCaminho);
        if (!arquivo.exists()) throw new NotFoundException();
        return Response.ok(arquivo, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + curso.gradePdfNome + "\"")
                .build();
    }
}
