package br.edu.sga.resource;

import br.edu.sga.entity.PlanoEnsino;
import jakarta.ws.rs.Path;

@Path("/api/planos-ensino")
public class PlanoEnsinoResource extends CadastroResource.Crud<PlanoEnsino> {
    public PlanoEnsinoResource() {
        super(PlanoEnsino.class);
    }
}
