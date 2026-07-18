package br.edu.sga.resource;

import br.edu.sga.entity.Aluno;
import br.edu.sga.service.AlunoUsuarioService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api/alunos")
public class AlunoResource extends CadastroResource.Crud<Aluno> {
    @Inject
    AlunoUsuarioService alunoUsuarioService;

    public AlunoResource() {
        super(Aluno.class);
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
        Aluno atual = buscar(id);
        if (entrada.usuario == null) entrada.usuario = atual.usuario;
        Aluno atualizado = super.atualizar(id, entrada);
        alunoUsuarioService.vincularUsuarioCompativel(atualizado);
        return atualizado;
    }
}
