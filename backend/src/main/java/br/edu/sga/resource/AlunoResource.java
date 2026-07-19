package br.edu.sga.resource;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.Frequencia;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.Matricula;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.Nota;
import br.edu.sga.entity.NotaAvaliacao;
import br.edu.sga.exception.ApiException;
import br.edu.sga.service.AlunoUsuarioService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/alunos")
public class AlunoResource extends CadastroResource.Crud<Aluno> {
    @Inject
    AlunoUsuarioService alunoUsuarioService;

    public AlunoResource() {
        super(Aluno.class);
    }

    @GET
    @Override
    public List<Aluno> listar(@QueryParam("pagina") @DefaultValue("0") int pagina,
                              @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        exigirGestaoAcademica();
        return super.listar(pagina, tamanho);
    }

    @GET
    @Path("/{id}")
    @Override
    public Aluno buscar(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        return super.buscar(id);
    }

    @Override
    @POST
    @Transactional
    public Aluno criar(@Valid Aluno aluno) {
        Aluno criado = super.criar(aluno);
        alunoUsuarioService.vincularUsuarioCompativel(criado);
        return criado;
    }

    @Override
    @PUT
    @Path("/{id}")
    @Transactional
    public Aluno atualizar(@PathParam("id") Long id, @Valid Aluno entrada) {
        Aluno atual = super.buscar(id);
        if (entrada.usuario == null) entrada.usuario = atual.usuario;
        Aluno atualizado = super.atualizar(id, entrada);
        alunoUsuarioService.vincularUsuarioCompativel(atualizado);
        return atualizado;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @Override
    public void excluir(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        Aluno aluno = super.buscar(id);
        if (MatriculaDisciplina.count("aluno", aluno) > 0 || Matricula.count("aluno", aluno) > 0
                || Nota.count("aluno", aluno) > 0 || NotaAvaliacao.count("aluno", aluno) > 0
                || Frequencia.count("aluno", aluno) > 0 || HistoricoEscolar.count("aluno", aluno) > 0) {
            throw new ApiException(Response.Status.CONFLICT,
                    "O aluno possui registros acadêmicos e não pode ser excluído");
        }
        getEntityManager().remove(aluno);
    }
}
