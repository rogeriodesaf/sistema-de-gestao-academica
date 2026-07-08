package br.edu.sga.resource;

import br.edu.sga.entity.HistoricoEscolar;
import jakarta.ws.rs.Path;

@Path("/api/historicos")
public class HistoricoResource extends CadastroResource.Crud<HistoricoEscolar> {
    public HistoricoResource() {
        super(HistoricoEscolar.class);
    }
}
