package br.edu.sga.resource;

import br.edu.sga.entity.Professor;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.Avaliacao;
import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.Turma;
import br.edu.sga.entity.VinculoProfessorDisciplinaTurma;
import br.edu.sga.exception.ApiException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/professores")
public class ProfessorResource extends CadastroResource.Crud<Professor> {
    public ProfessorResource() {
        super(Professor.class);
    }

    @GET
    @Override
    public List<Professor> listar(@QueryParam("pagina") @DefaultValue("0") int pagina,
                                  @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        exigirGestaoAcademica();
        return super.listar(pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    @Override
    public Professor buscar(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        return super.buscar(id);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @Override
    public void excluir(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        Professor professor = super.buscar(id);
        if (OfertaDisciplina.count("professor", professor) > 0 || AulaMinistrada.count("professor", professor) > 0
                || Avaliacao.count("professor", professor) > 0
                || HistoricoEscolar.count("professorResponsavel", professor) > 0
                || Disciplina.count("professorResponsavel", professor) > 0 || Turma.count("professor", professor) > 0
                || VinculoProfessorDisciplinaTurma.count("professor", professor) > 0) {
            throw new ApiException(Response.Status.CONFLICT,
                    "O professor possui vínculos acadêmicos e não pode ser excluído");
        }
        getEntityManager().remove(professor);
    }
}
