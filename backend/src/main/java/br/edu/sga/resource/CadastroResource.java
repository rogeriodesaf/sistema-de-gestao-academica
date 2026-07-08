package br.edu.sga.resource;

import br.edu.sga.entity.*;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CadastroResource {
    public static abstract class Crud<T extends PanacheEntity> {
        private final Class<T> tipo;
        @PersistenceContext
        EntityManager entityManager;

        protected Crud(Class<T> tipo) { this.tipo = tipo; }

        @GET
        public List<T> listar(@QueryParam("pagina") @DefaultValue("0") int pagina, @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
            return entityManager.createQuery("from " + tipo.getSimpleName(), tipo)
                    .setFirstResult(Math.max(pagina, 0) * Math.max(tamanho, 1))
                    .setMaxResults(Math.max(tamanho, 1))
                    .getResultList();
        }

        @GET
        @Path("/{id}")
        public T buscar(@PathParam("id") Long id) {
            T entidade = entityManager.find(tipo, id);
            if (entidade == null) throw new NotFoundException();
            return entidade;
        }

        @POST
        @Transactional
        public T criar(@Valid T entidade) {
            entityManager.persist(entidade);
            return entidade;
        }

        @PUT
        @Path("/{id}")
        @Transactional
        public T atualizar(@PathParam("id") Long id, @Valid T entrada) {
            buscar(id);
            entrada.id = id;
            return entityManager.merge(entrada);
        }

        @DELETE
        @Path("/{id}")
        @Transactional
        public void excluir(@PathParam("id") Long id) {
            entityManager.remove(buscar(id));
        }
    }
}
