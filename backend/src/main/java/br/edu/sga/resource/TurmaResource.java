package br.edu.sga.resource;

import br.edu.sga.dto.TurmaOpcaoDTO;
import br.edu.sga.entity.Turma;
import br.edu.sga.enums.StatusTurma;
import br.edu.sga.exception.ApiException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/turmas")
public class TurmaResource extends CadastroResource.Crud<Turma> {
    public TurmaResource() {
        super(Turma.class);
    }

    @GET
    @Path("/opcoes")
    public List<TurmaOpcaoDTO> listarOpcoes(@QueryParam("anoLetivoId") Long anoLetivoId,
                                            @QueryParam("cursoId") Long cursoId,
                                            @QueryParam("moduloId") Long moduloId) {
        List<StatusTurma> disponiveis = List.of(
                StatusTurma.PLANEJADA, StatusTurma.ABERTA, StatusTurma.EM_ANDAMENTO);
        StringBuilder consulta = new StringBuilder("status in :status");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("status", disponiveis);
        if (anoLetivoId != null) {
            consulta.append(" and anoLetivo.id = :anoLetivoId");
            parametros.put("anoLetivoId", anoLetivoId);
        }
        if (cursoId != null) {
            consulta.append(" and (curso.id = :cursoId or (curso is null and disciplina.curso.id = :cursoId))");
            parametros.put("cursoId", cursoId);
        }
        if (moduloId != null) {
            consulta.append(" and disciplina.modulo.id = :moduloId");
            parametros.put("moduloId", moduloId);
        }
        consulta.append(" order by nome");
        return Turma.<Turma>find(consulta.toString(), parametros)
                .list().stream().map(TurmaOpcaoDTO::de).toList();
    }

    @POST
    @Transactional
    @Override
    public Turma criar(@Valid Turma turma) {
        validarTurma(turma, null);
        turma.persist();
        return turma;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Override
    public Turma atualizar(@PathParam("id") Long id, @Valid Turma turma) {
        buscar(id);
        validarTurma(turma, id);
        turma.id = id;
        return getEntityManager().merge(turma);
    }

    private void validarTurma(Turma turma, Long idAtual) {
        if (turma == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Turma obrigatoria");
        }
        if (turma.disciplina == null || turma.disciplina.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Disciplina obrigatoria");
        }
        if (turma.professor == null || turma.professor.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Professor obrigatorio");
        }
        if (turma.horario == null || turma.horario.isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Horario obrigatorio");
        }
        if (turma.sala == null || turma.sala.isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Sala obrigatoria");
        }
        if (turma.quantidadeMaximaAlunos == null || turma.quantidadeMaximaAlunos <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Quantidade maxima de alunos obrigatoria");
        }
        if (turma.status == null) {
            turma.status = StatusTurma.ABERTA;
        }

        List<StatusTurma> statusAtivos = List.of(StatusTurma.PLANEJADA, StatusTurma.ABERTA, StatusTurma.EM_ANDAMENTO);
        String sufixoEdicao = idAtual == null ? "" : " and id <> ?4";
        Object[] parametrosProfessor = idAtual == null
                ? new Object[] { turma.professor.id, turma.horario.trim().toLowerCase(), statusAtivos }
                : new Object[] { turma.professor.id, turma.horario.trim().toLowerCase(), statusAtivos, idAtual };
        long conflitosProfessor = Turma.count(
                "professor.id = ?1 and lower(horario) = ?2 and status in ?3" + sufixoEdicao,
                parametrosProfessor
        );
        if (conflitosProfessor > 0) {
            throw new ApiException(Response.Status.CONFLICT, "O professor ja possui uma turma neste horario.");
        }

        Object[] parametrosSala = idAtual == null
                ? new Object[] { turma.sala.trim().toLowerCase(), turma.horario.trim().toLowerCase(), statusAtivos }
                : new Object[] { turma.sala.trim().toLowerCase(), turma.horario.trim().toLowerCase(), statusAtivos, idAtual };
        long conflitosSala = Turma.count(
                "lower(sala) = ?1 and lower(horario) = ?2 and status in ?3" + sufixoEdicao,
                parametrosSala
        );
        if (conflitosSala > 0) {
            throw new ApiException(Response.Status.CONFLICT, "A sala ja esta ocupada neste horario.");
        }
    }
}
