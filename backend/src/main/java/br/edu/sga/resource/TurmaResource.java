package br.edu.sga.resource;

import br.edu.sga.dto.TurmaOpcaoDTO;
import br.edu.sga.entity.AnoLetivo;
import br.edu.sga.entity.Curso;
import br.edu.sga.entity.PeriodoLetivo;
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
                                            @QueryParam("periodoLetivoId") Long periodoLetivoId) {
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
            consulta.append(" and (curso.id = :cursoId or curso is null)");
            parametros.put("cursoId", cursoId);
        }
        if (periodoLetivoId != null) {
            consulta.append(" and periodoLetivo.id = :periodoLetivoId");
            parametros.put("periodoLetivoId", periodoLetivoId);
        }
        consulta.append(" order by nome");
        return Turma.<Turma>find(consulta.toString(), parametros)
                .list().stream().map(TurmaOpcaoDTO::de).toList();
    }

    @POST
    @Transactional
    @Override
    public Turma criar(@Valid Turma turma) {
        exigirGestaoAcademica();
        validarTurma(turma, null);
        turma.disciplina = null;
        turma.professor = null;
        turma.horario = null;
        turma.sala = null;
        turma.persist();
        return turma;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Override
    public Turma atualizar(@PathParam("id") Long id, @Valid Turma turma) {
        exigirGestaoAcademica();
        Turma existente = buscar(id);
        validarTurma(turma, id);
        turma.disciplina = existente.disciplina;
        turma.professor = existente.professor;
        turma.horario = existente.horario;
        turma.sala = existente.sala;
        turma.id = id;
        return getEntityManager().merge(turma);
    }

    private void validarTurma(Turma turma, Long idAtual) {
        if (turma == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Turma obrigatoria");
        }
        if (turma.quantidadeMaximaAlunos == null || turma.quantidadeMaximaAlunos <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Quantidade maxima de alunos obrigatoria");
        }
        if (turma.anoLetivo == null || turma.anoLetivo.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Ano letivo obrigatorio");
        }
        AnoLetivo anoLetivo = AnoLetivo.findById(turma.anoLetivo.id);
        Curso curso = turma.curso == null || turma.curso.id == null ? null : Curso.findById(turma.curso.id);
        PeriodoLetivo periodo = turma.periodoLetivo == null || turma.periodoLetivo.id == null
                ? null : PeriodoLetivo.findById(turma.periodoLetivo.id);
        if (anoLetivo == null || turma.curso != null && turma.curso.id != null && curso == null
                || turma.periodoLetivo != null && turma.periodoLetivo.id != null && periodo == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Ano, periodo ou curso da turma nao encontrado");
        }
        if (periodo != null && periodo.anoLetivo != null && !anoLetivo.id.equals(periodo.anoLetivo.id)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "O periodo letivo nao pertence ao ano informado");
        }
        turma.anoLetivo = anoLetivo;
        turma.curso = curso;
        turma.periodoLetivo = periodo;
        if (turma.status == null) {
            turma.status = StatusTurma.ABERTA;
        }
    }
}
