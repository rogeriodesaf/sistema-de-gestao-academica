package br.edu.sga.resource;

import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.exception.ApiException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/ofertas-disciplinas")
public class OfertaDisciplinaResource extends CadastroResource.Crud<OfertaDisciplina> {
    public OfertaDisciplinaResource() {
        super(OfertaDisciplina.class);
    }

    @POST
    @Transactional
    @Override
    public OfertaDisciplina criar(@Valid OfertaDisciplina oferta) {
        validarOferta(oferta, null);
        oferta.persist();
        return oferta;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Override
    public OfertaDisciplina atualizar(@PathParam("id") Long id, @Valid OfertaDisciplina oferta) {
        buscar(id);
        validarOferta(oferta, id);
        oferta.id = id;
        return getEntityManager().merge(oferta);
    }

    private void validarOferta(OfertaDisciplina oferta, Long idAtual) {
        if (oferta == null || oferta.disciplina == null || oferta.disciplina.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Disciplina obrigatoria");
        }
        if (oferta.professor == null || oferta.professor.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Professor obrigatorio");
        }
        if (oferta.anoLetivo == null || oferta.anoLetivo.id == null || oferta.periodoLetivo == null || oferta.periodoLetivo.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Ano e periodo letivo sao obrigatorios");
        }
        if (oferta.modulo == null || oferta.modulo.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Modulo de oferta obrigatorio");
        }
        if (oferta.turma == null || oferta.turma.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Turma da oferta obrigatoria");
        }
        if (oferta.vagas == null || oferta.vagas <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Vagas obrigatorias");
        }
        if (oferta.horario == null || oferta.horario.isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Horario obrigatorio");
        }
        if (oferta.sala == null || oferta.sala.isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Sala obrigatoria");
        }
        if (oferta.status == null) {
            oferta.status = StatusOfertaDisciplina.ABERTA;
        }
        List<StatusOfertaDisciplina> statusAtivos = List.of(
                StatusOfertaDisciplina.PLANEJADA,
                StatusOfertaDisciplina.ABERTA,
                StatusOfertaDisciplina.EM_ANDAMENTO
        );
        String consultaDuplicidade = "disciplina.id = ?1 and anoLetivo.id = ?2 and periodoLetivo.id = ?3 and lower(horario) = ?4 and lower(sala) = ?5 and status in ?6";
        Object[] parametros = {
                oferta.disciplina.id,
                oferta.anoLetivo.id,
                oferta.periodoLetivo.id,
                oferta.horario.trim().toLowerCase(),
                oferta.sala.trim().toLowerCase(),
                statusAtivos
        };
        if (idAtual != null) {
            consultaDuplicidade += " and id <> ?7";
            parametros = new Object[] {
                    oferta.disciplina.id,
                    oferta.anoLetivo.id,
                    oferta.periodoLetivo.id,
                    oferta.horario.trim().toLowerCase(),
                    oferta.sala.trim().toLowerCase(),
                    statusAtivos,
                    idAtual
            };
        }

        long duplicadas = OfertaDisciplina.count(consultaDuplicidade, parametros);
        if (duplicadas > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Ja existe uma oferta desta disciplina no mesmo periodo, horario e sala");
        }
    }
}
