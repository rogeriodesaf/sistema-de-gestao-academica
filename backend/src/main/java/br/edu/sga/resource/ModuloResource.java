package br.edu.sga.resource;

import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.Modulo;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.List;

@Path("/api/modulos")
public class ModuloResource extends CadastroResource.Crud<Modulo> {
    public ModuloResource() {
        super(Modulo.class);
    }

    public record DisciplinasModuloDTO(List<Long> disciplinasIds) {}

    @PUT
    @Path("/{id}/disciplinas")
    @Transactional
    public Modulo vincularDisciplinas(@PathParam("id") Long id, DisciplinasModuloDTO dto) {
        Modulo modulo = Modulo.findById(id);
        if (modulo == null) throw new jakarta.ws.rs.NotFoundException();
        List<Long> ids = dto == null || dto.disciplinasIds() == null ? List.of() : dto.disciplinasIds();
        Disciplina.update("modulo = null where modulo = ?1", modulo);
        if (!ids.isEmpty()) {
            Disciplina.update("moduloOriginal = ?1 where moduloOriginal is null and id in ?2", modulo, ids);
            Disciplina.update("modulo = ?1 where id in ?2", modulo, ids);
        }
        return modulo;
    }
}
