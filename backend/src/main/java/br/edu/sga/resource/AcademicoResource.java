package br.edu.sga.resource;

import br.edu.sga.entity.*;
import br.edu.sga.enums.Perfil;
import br.edu.sga.service.AcademicoService;
import br.edu.sga.service.PermissaoService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AcademicoResource {
    @Inject AcademicoService academicoService;
    @Inject PermissaoService permissaoService;
    @Context ContainerRequestContext contexto;

    @POST
    @Path("/matriculas")
    public Matricula matricular(@Valid Matricula matricula) {
        permissaoService.exigir(contexto, Perfil.COORDENADOR, Perfil.SECRETARIA);
        return academicoService.matricular(matricula);
    }

    @GET
    @Path("/matriculas")
    public Object matriculas() {
        permissaoService.exigir(contexto, Perfil.COORDENADOR, Perfil.SECRETARIA);
        return Matricula.listAll();
    }

    @POST
    @Path("/notas")
    public Nota salvarNota(@Valid Nota nota) {
        permissaoService.exigir(contexto, Perfil.COORDENADOR, Perfil.PROFESSOR);
        return academicoService.salvarNota(nota, permissaoService.perfil(contexto), permissaoService.usuarioId(contexto));
    }

    @GET
    @Path("/notas")
    public Object notas() {
        permissaoService.exigir(contexto, Perfil.COORDENADOR, Perfil.SECRETARIA);
        return Nota.listAll();
    }

    @POST
    @Path("/frequencias")
    public Frequencia salvarFrequencia(@Valid Frequencia frequencia) {
        permissaoService.exigir(contexto, Perfil.COORDENADOR, Perfil.PROFESSOR);
        return academicoService.salvarFrequencia(frequencia, permissaoService.perfil(contexto), permissaoService.usuarioId(contexto));
    }

    @GET
    @Path("/frequencias")
    public Object frequencias() {
        permissaoService.exigir(contexto, Perfil.COORDENADOR, Perfil.SECRETARIA);
        return Frequencia.listAll();
    }

    @GET
    @Path("/encerramentos/{turmaId}/{disciplinaId}/pendencias")
    public Object pendencias(@PathParam("turmaId") Long turmaId, @PathParam("disciplinaId") Long disciplinaId) {
        permissaoService.exigir(contexto, Perfil.COORDENADOR);
        return Map.of("pendencias", academicoService.pendenciasEncerramento(turmaId, disciplinaId));
    }

    @POST
    @Path("/encerramentos/{turmaId}/{disciplinaId}")
    public Object encerrar(@PathParam("turmaId") Long turmaId, @PathParam("disciplinaId") Long disciplinaId) {
        permissaoService.exigir(contexto, Perfil.COORDENADOR);
        academicoService.encerrarDisciplina(turmaId, disciplinaId);
        return Map.of("mensagem", "Disciplina encerrada com sucesso");
    }
}
