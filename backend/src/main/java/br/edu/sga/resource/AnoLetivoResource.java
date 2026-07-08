package br.edu.sga.resource;

import br.edu.sga.entity.AnoLetivo;
import jakarta.ws.rs.Path;

@Path("/api/anos-letivos")
public class AnoLetivoResource extends CadastroResource.Crud<AnoLetivo> {
    public AnoLetivoResource() {
        super(AnoLetivo.class);
    }
}
