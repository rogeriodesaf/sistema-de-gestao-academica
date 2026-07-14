package br.edu.sga.resource;

import br.edu.sga.entity.AulaMinistrada;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api/aulas")
public class AulaResource extends CadastroResource.Crud<AulaMinistrada> {
    public AulaResource() {
        super(AulaMinistrada.class);
    }

    @Override
    @POST
    public AulaMinistrada criar(AulaMinistrada aula) {
        throw new NotAllowedException("GET");
    }

    @Override
    @PUT
    @Path("/{id}")
    public AulaMinistrada atualizar(@PathParam("id") Long id, AulaMinistrada aula) {
        throw new NotAllowedException("GET");
    }

    @Override
    @DELETE
    @Path("/{id}")
    public void excluir(@PathParam("id") Long id) {
        throw new NotAllowedException("GET");
    }
}
