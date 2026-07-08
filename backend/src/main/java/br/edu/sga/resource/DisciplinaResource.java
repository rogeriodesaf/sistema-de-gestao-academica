package br.edu.sga.resource;

import br.edu.sga.entity.Disciplina;
import jakarta.ws.rs.Path;

@Path("/api/disciplinas")
public class DisciplinaResource extends CadastroResource.Crud<Disciplina> {
    public DisciplinaResource() {
        super(Disciplina.class);
    }
}
