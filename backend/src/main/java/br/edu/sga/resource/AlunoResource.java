package br.edu.sga.resource;

import br.edu.sga.entity.Aluno;
import jakarta.ws.rs.Path;

@Path("/api/alunos")
public class AlunoResource extends CadastroResource.Crud<Aluno> {
    public AlunoResource() {
        super(Aluno.class);
    }
}
