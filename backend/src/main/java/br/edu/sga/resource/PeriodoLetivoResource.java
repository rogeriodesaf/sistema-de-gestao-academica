package br.edu.sga.resource;

import br.edu.sga.entity.PeriodoLetivo;
import jakarta.ws.rs.Path;

@Path("/api/periodos-letivos")
public class PeriodoLetivoResource extends CadastroResource.Crud<PeriodoLetivo> {
    public PeriodoLetivoResource() {
        super(PeriodoLetivo.class);
    }
}
