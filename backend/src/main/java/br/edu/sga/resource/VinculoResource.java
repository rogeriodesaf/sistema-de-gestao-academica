package br.edu.sga.resource;

import br.edu.sga.entity.VinculoProfessorDisciplinaTurma;
import jakarta.ws.rs.Path;

@Path("/api/vinculos")
public class VinculoResource extends CadastroResource.Crud<VinculoProfessorDisciplinaTurma> {
    public VinculoResource() {
        super(VinculoProfessorDisciplinaTurma.class);
    }
}
