package br.edu.sga.resource;

import br.edu.sga.dto.TurmaOpcaoDTO;
import br.edu.sga.dto.AlunosTurmaDTO;
import br.edu.sga.dto.AlunosTurmaDTO.AlunoDTO;
import br.edu.sga.dto.AlunosTurmaDTO.DisciplinaDTO;
import br.edu.sga.entity.AnoLetivo;
import br.edu.sga.entity.Curso;
import br.edu.sga.entity.PeriodoLetivo;
import br.edu.sga.entity.Turma;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.Matricula;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.Nota;
import br.edu.sga.entity.PlanoEnsino;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.enums.StatusTurma;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.exception.ApiException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @GET
    @Path("/{id}/alunos")
    public AlunosTurmaDTO listarAlunos(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        buscar(id);
        List<StatusMatriculaDisciplina> vigentes = List.of(
                StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO);
        List<MatriculaDisciplina> matriculas = getEntityManager().createQuery("""
                select md from MatriculaDisciplina md
                join fetch md.aluno aluno
                left join fetch aluno.curso
                left join fetch md.curso
                join fetch md.ofertaDisciplina oferta
                join fetch oferta.disciplina
                left join fetch oferta.curso
                where oferta.turma.id = :turmaId and md.status in :status
                order by aluno.nome, oferta.disciplina.nome, md.id
                """, MatriculaDisciplina.class)
                .setParameter("turmaId", id)
                .setParameter("status", vigentes)
                .getResultList();

        Map<Long, List<MatriculaDisciplina>> porAluno = matriculas.stream().collect(Collectors.groupingBy(
                matricula -> matricula.aluno.id, LinkedHashMap::new, Collectors.toList()));
        List<AlunoDTO> alunos = porAluno.values().stream().map(vinculosAluno -> {
            MatriculaDisciplina primeiro = vinculosAluno.getFirst();
            Map<Long, List<MatriculaDisciplina>> porOferta = vinculosAluno.stream().collect(Collectors.groupingBy(
                    matricula -> matricula.ofertaDisciplina.id, LinkedHashMap::new, Collectors.toList()));
            List<DisciplinaDTO> disciplinas = porOferta.values().stream().map(vinculosOferta -> {
                MatriculaDisciplina matricula = vinculosOferta.getFirst();
                OfertaDisciplina oferta = matricula.ofertaDisciplina;
                return new DisciplinaDTO(
                        matricula.id,
                        oferta.id,
                        oferta.disciplina.id,
                        oferta.disciplina.nome,
                        oferta.horario,
                        matricula.status,
                        matricula.resultadoAcademico,
                        matricula.dataMatricula,
                        vinculosOferta.size());
            }).toList();
            String curso = primeiro.aluno.curso != null ? primeiro.aluno.curso.nome
                    : primeiro.curso != null ? primeiro.curso.nome
                    : primeiro.ofertaDisciplina.curso != null ? primeiro.ofertaDisciplina.curso.nome
                    : "Disciplina avulsa";
            return new AlunoDTO(
                    primeiro.aluno.id,
                    primeiro.aluno.nome,
                    curso,
                    disciplinas.size(),
                    disciplinas);
        }).toList();
        return new AlunosTurmaDTO(alunos.size(), matriculas.size(), alunos);
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
        turma.status = StatusTurma.ABERTA;
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
        if (turma.status == null) turma.status = existente.status;
        validarTurma(turma, id);
        validarTransicaoStatus(existente, turma.status);
        turma.disciplina = existente.disciplina;
        turma.professor = existente.professor;
        turma.horario = existente.horario;
        turma.sala = existente.sala;
        turma.id = id;
        return getEntityManager().merge(turma);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @Override
    public void excluir(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        Turma turma = buscar(id);
        if (OfertaDisciplina.count("turma", turma) > 0 || Matricula.count("turma", turma) > 0
                || AulaMinistrada.count("turma", turma) > 0 || HistoricoEscolar.count("turma", turma) > 0
                || Nota.count("turma", turma) > 0 || PlanoEnsino.count("turma", turma) > 0) {
            throw new ApiException(Response.Status.CONFLICT,
                    "A turma possui ofertas ou registros acadêmicos vinculados");
        }
        getEntityManager().remove(turma);
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

    private void validarTransicaoStatus(Turma existente, StatusTurma destino) {
        if (destino == existente.status) return;
        if (existente.status == StatusTurma.ENCERRADA || existente.status == StatusTurma.CANCELADA) {
            throw new ApiException(Response.Status.CONFLICT, "Uma turma encerrada ou cancelada não pode ter sua situação alterada");
        }
        if (destino == StatusTurma.ENCERRADA) {
            if (existente.status != StatusTurma.EM_ANDAMENTO) {
                throw new ApiException(Response.Status.CONFLICT,
                        "A turma deve estar em andamento antes de ser encerrada");
            }
            validarEncerramento(existente);
            return;
        }
        boolean transicaoValida = existente.status == StatusTurma.PLANEJADA && destino == StatusTurma.ABERTA
                || existente.status == StatusTurma.ABERTA && destino == StatusTurma.EM_ANDAMENTO
                || destino == StatusTurma.CANCELADA;
        if (!transicaoValida) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Transição de situação da turma não permitida");
        }
    }

    private void validarEncerramento(Turma turma) {
        List<OfertaDisciplina> ofertas = OfertaDisciplina.list("turma", turma);
        if (ofertas.isEmpty()) {
            throw new ApiException(Response.Status.CONFLICT,
                    "A turma não pode ser encerrada sem ofertas concluídas");
        }
        boolean ofertaPendente = ofertas.stream().anyMatch(oferta ->
                oferta.status != StatusOfertaDisciplina.CONCLUIDA
                        || oferta.dataEncerramento == null
                        || oferta.dataHomologacao == null);
        if (ofertaPendente) {
            throw new ApiException(Response.Status.CONFLICT,
                    "A turma possui ofertas, diários ou homologações pendentes");
        }
    }
}
