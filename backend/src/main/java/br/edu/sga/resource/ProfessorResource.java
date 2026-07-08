package br.edu.sga.resource;

import br.edu.sga.entity.Professor;
import jakarta.ws.rs.Path;

@Path("/api/professores")
public class ProfessorResource extends CadastroResource.Crud<Professor> {
    public ProfessorResource() {
        super(Professor.class);
    }
}
