package br.edu.sga.resource;

import br.edu.sga.entity.OfertaDisciplina;
import jakarta.ws.rs.Path;

@Path("/api/ofertas-disciplinas")
public class OfertaDisciplinaResource extends CadastroResource.Crud<OfertaDisciplina> {
    public OfertaDisciplinaResource() {
        super(OfertaDisciplina.class);
    }
}
