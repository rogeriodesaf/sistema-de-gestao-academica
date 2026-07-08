package br.edu.sga.resource;

import br.edu.sga.entity.Curso;
import jakarta.ws.rs.Path;

@Path("/api/cursos")
public class CursoResource extends CadastroResource.Crud<Curso> {
    public CursoResource() {
        super(Curso.class);
    }
}
