package br.edu.sga.resource;

import br.edu.sga.entity.Modulo;
import jakarta.ws.rs.Path;

@Path("/api/modulos")
public class ModuloResource extends CadastroResource.Crud<Modulo> {
    public ModuloResource() {
        super(Modulo.class);
    }
}
