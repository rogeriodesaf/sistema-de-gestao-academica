package br.edu.sga.resource;

import br.edu.sga.entity.Turma;
import jakarta.ws.rs.Path;

@Path("/api/turmas")
public class TurmaResource extends CadastroResource.Crud<Turma> {
    public TurmaResource() {
        super(Turma.class);
    }
}
