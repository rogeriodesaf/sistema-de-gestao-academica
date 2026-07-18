package br.edu.sga.resource;

import br.edu.sga.enums.StatusHistorico;
import br.edu.sga.exception.ApiException;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

@Path("/api/historicos")
public class HistoricoResource {
    @Inject EntityManager entityManager;

    public record HistoricoConsultaDTO(Long id, String aluno, String curso, String disciplina,
                                       String codigo, String modulo, String periodoCursado,
                                       Integer cargaHoraria, Integer creditos, BigDecimal notaFinal,
                                       BigDecimal frequenciaFinal, String situacao, String professor,
                                       LocalDateTime dataHomologacao) {}

    @GET
    public List<HistoricoConsultaDTO> listar() {
        return entityManager.createQuery("""
                select h.id, a.nome, c.nome, coalesce(h.disciplinaNome, d.nome),
                       coalesce(h.disciplinaCodigo, d.codigo), coalesce(h.moduloNome, m.nome), h.periodoCursado,
                       h.cargaHoraria, coalesce(h.creditos, d.creditos), h.notaFinal, h.frequenciaFinal,
                       h.situacao, coalesce(h.professorNome, p.nome), h.dataHomologacao
                from HistoricoEscolar h
                left join h.aluno a left join h.curso c left join h.disciplina d
                left join h.ofertaDisciplina o left join o.modulo m left join h.professorResponsavel p
                order by a.nome, h.periodoCursado, d.nome
                """, Object[].class).getResultList().stream().map(this::dto).toList();
    }

    @POST
    public void criar() {
        somenteLeitura();
    }

    @PUT
    @Path("/{id}")
    public void atualizar(@PathParam("id") Long id) {
        somenteLeitura();
    }

    @DELETE
    @Path("/{id}")
    public void excluir(@PathParam("id") Long id) {
        somenteLeitura();
    }

    private HistoricoConsultaDTO dto(Object[] item) {
        StatusHistorico situacao = (StatusHistorico) item[11];
        return new HistoricoConsultaDTO((Long) item[0], (String) item[1], (String) item[2],
                (String) item[3], (String) item[4], (String) item[5], (String) item[6],
                (Integer) item[7], (Integer) item[8], (BigDecimal) item[9], (BigDecimal) item[10],
                situacao == null ? null : situacao.name(), (String) item[12], (LocalDateTime) item[13]);
    }

    private void somenteLeitura() {
        throw new ApiException(Response.Status.METHOD_NOT_ALLOWED,
                "O historico escolar e alimentado exclusivamente pela homologacao");
    }
}
