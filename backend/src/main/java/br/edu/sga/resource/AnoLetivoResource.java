package br.edu.sga.resource;

import br.edu.sga.entity.AnoLetivo;
import br.edu.sga.entity.Modulo;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.PeriodoLetivo;
import br.edu.sga.entity.Turma;
import br.edu.sga.exception.ApiException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/anos-letivos")
public class AnoLetivoResource extends CadastroResource.Crud<AnoLetivo> {
    public AnoLetivoResource() {
        super(AnoLetivo.class);
    }

    @Override
    @GET
    public List<AnoLetivo> listar(@QueryParam("pagina") @DefaultValue("0") int pagina,
                                  @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        return getEntityManager().createQuery(
                        "from AnoLetivo where legado = false order by ano", AnoLetivo.class)
                .setFirstResult(Math.max(pagina, 0) * Math.max(tamanho, 1))
                .setMaxResults(Math.max(tamanho, 1))
                .getResultList();
    }

    @Override
    @POST
    @Transactional
    public AnoLetivo criar(@Valid AnoLetivo anoLetivo) {
        validar(anoLetivo, null);
        anoLetivo.legado = false;
        return super.criar(anoLetivo);
    }

    @Override
    @PUT
    @Path("/{id}")
    @Transactional
    public AnoLetivo atualizar(@PathParam("id") Long id, @Valid AnoLetivo anoLetivo) {
        validar(anoLetivo, id);
        anoLetivo.legado = false;
        return super.atualizar(id, anoLetivo);
    }

    @Override
    @DELETE
    @Path("/{id}")
    @Transactional
    public void excluir(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        buscar(id);
        boolean possuiVinculos = Modulo.count("anoLetivo.id", id) > 0
                || PeriodoLetivo.count("anoLetivo.id", id) > 0
                || OfertaDisciplina.count("anoLetivo.id", id) > 0
                || Turma.count("anoLetivo.id", id) > 0;
        if (possuiVinculos) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Nao e possivel excluir este Ano Letivo porque existem modulos, periodos, turmas ou ofertas vinculados");
        }
        super.excluir(id);
    }

    private void validar(AnoLetivo anoLetivo, Long idAtual) {
        if (anoLetivo.dataInicio.isAfter(anoLetivo.dataFim)) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "A data de inicio deve ser anterior a data de termino");
        }
        long duplicados = idAtual == null
                ? AnoLetivo.count("ano = ?1 and legado = false", anoLetivo.ano)
                : AnoLetivo.count("ano = ?1 and legado = false and id <> ?2", anoLetivo.ano, idAtual);
        if (duplicados > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Ja existe um Ano Letivo cadastrado para este ano");
        }
    }
}
