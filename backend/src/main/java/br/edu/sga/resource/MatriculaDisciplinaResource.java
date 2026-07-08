package br.edu.sga.resource;

import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.service.AcademicoService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/matriculas-disciplinas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MatriculaDisciplinaResource {
    @Inject
    AcademicoService academicoService;

    @GET
    public List<MatriculaDisciplina> listar() {
        return MatriculaDisciplina.listAll();
    }

    @GET
    @Path("/{id}")
    public MatriculaDisciplina buscar(@PathParam("id") Long id) {
        MatriculaDisciplina matricula = MatriculaDisciplina.findById(id);
        if (matricula == null) throw new NotFoundException();
        return matricula;
    }

    @POST
    public MatriculaDisciplina criar(@Valid MatriculaDisciplina matricula) {
        return academicoService.matricularEmDisciplina(matricula);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public MatriculaDisciplina atualizar(@PathParam("id") Long id, @Valid MatriculaDisciplina entrada) {
        MatriculaDisciplina atual = buscar(id);
        atual.status = entrada.status;
        atual.notaFinal = entrada.notaFinal;
        atual.frequenciaFinal = entrada.frequenciaFinal;
        atual.observacoes = entrada.observacoes;
        return atual;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void excluir(@PathParam("id") Long id) {
        MatriculaDisciplina.deleteById(id);
    }
}
