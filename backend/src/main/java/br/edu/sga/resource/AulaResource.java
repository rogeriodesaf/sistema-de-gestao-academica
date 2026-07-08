package br.edu.sga.resource;

import br.edu.sga.entity.AulaMinistrada;
import jakarta.ws.rs.Path;

@Path("/api/aulas")
public class AulaResource extends CadastroResource.Crud<AulaMinistrada> {
    public AulaResource() {
        super(AulaMinistrada.class);
    }
}
