package br.edu.sga.resource;

import br.edu.sga.entity.AnoLetivo;
import br.edu.sga.entity.ArquivoProfessor;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.Avaliacao;
import br.edu.sga.entity.Curso;
import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.Modulo;
import br.edu.sga.entity.Nota;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.PeriodoLetivo;
import br.edu.sga.entity.PlanoEnsino;
import br.edu.sga.entity.Professor;
import br.edu.sga.entity.Turma;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.enums.StatusTurma;
import br.edu.sga.exception.ApiException;
import br.edu.sga.service.IntegridadeAcademicaService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/ofertas-disciplinas")
public class OfertaDisciplinaResource extends CadastroResource.Crud<OfertaDisciplina> {
    @Inject IntegridadeAcademicaService integridadeService;

    public OfertaDisciplinaResource() {
        super(OfertaDisciplina.class);
    }

    @GET
    @Override
    public List<OfertaDisciplina> listar(@QueryParam("pagina") @DefaultValue("0") int pagina,
                                         @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        exigirGestaoAcademica();
        return super.listar(pagina, tamanho).stream().map(this::resumir).toList();
    }

    @GET
    @Path("/{id}")
    @Override
    public OfertaDisciplina buscar(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        return resumir(super.buscar(id));
    }

    @POST
    @Transactional
    @Override
    public OfertaDisciplina criar(@Valid OfertaDisciplina oferta) {
        exigirGestaoAcademica();
        validarOferta(oferta, null);
        oferta.persist();
        return resumir(oferta);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Override
    public OfertaDisciplina atualizar(@PathParam("id") Long id, @Valid OfertaDisciplina oferta) {
        exigirGestaoAcademica();
        OfertaDisciplina existente = super.buscar(id);
        validarAlteracaoStatus(existente, oferta);
        validarOferta(oferta, id);
        if (oferta.status == StatusOfertaDisciplina.EM_ANDAMENTO
                && oferta.turma.status == StatusTurma.ABERTA) {
            oferta.turma.status = StatusTurma.EM_ANDAMENTO;
        }
        oferta.id = id;
        OfertaDisciplina atualizada = getEntityManager().merge(oferta);
        return resumir(atualizada);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @Override
    public void excluir(@PathParam("id") Long id) {
        exigirGestaoAcademica();
        OfertaDisciplina oferta = super.buscar(id);
        if (possuiRegistrosAcademicos(oferta)) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Esta oferta possui matriculas ou registros academicos e nao pode ser excluida. Cancele a oferta para preservar o historico.");
        }
        if (!List.of(StatusOfertaDisciplina.PLANEJADA, StatusOfertaDisciplina.ABERTA,
                StatusOfertaDisciplina.CANCELADA).contains(oferta.status)) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Somente ofertas planejadas, abertas ou canceladas e sem registros academicos podem ser excluidas.");
        }
        getEntityManager().remove(oferta);
    }

    private boolean possuiRegistrosAcademicos(OfertaDisciplina oferta) {
        return MatriculaDisciplina.count("ofertaDisciplina", oferta) > 0
                || AulaMinistrada.count("ofertaDisciplina", oferta) > 0
                || Avaliacao.count("ofertaDisciplina", oferta) > 0
                || ArquivoProfessor.count("ofertaDisciplina", oferta) > 0
                || HistoricoEscolar.count("ofertaDisciplina", oferta) > 0
                || Nota.count("ofertaDisciplina", oferta) > 0
                || PlanoEnsino.count("ofertaDisciplina", oferta) > 0;
    }

    private OfertaDisciplina resumir(OfertaDisciplina origem) {
        OfertaDisciplina resumo = new OfertaDisciplina();
        resumo.id = origem.id;
        resumo.turma = turmaResumida(origem.turma);
        resumo.anoLetivo = anoLetivoResumido(origem.anoLetivo);
        PeriodoLetivo periodo = origem.periodoLetivo != null
                ? origem.periodoLetivo
                : origem.turma == null ? null : origem.turma.periodoLetivo;
        resumo.periodoLetivo = periodoLetivoResumido(periodo);
        resumo.curso = cursoResumido(origem.curso);
        resumo.modulo = moduloResumido(origem.modulo);
        resumo.disciplina = disciplinaResumida(origem.disciplina);
        resumo.professor = professorResumido(origem.professor);
        resumo.vagas = origem.vagas;
        resumo.horario = origem.horario;
        resumo.sala = origem.sala;
        resumo.cargaHorariaPrevista = origem.cargaHorariaPrevista;
        resumo.cargaHorariaMinistrada = origem.cargaHorariaMinistrada;
        resumo.dataInicio = origem.dataInicio;
        resumo.dataFim = origem.dataFim;
        resumo.status = origem.status;
        resumo.dataEncerramento = origem.dataEncerramento;
        resumo.dataHomologacao = origem.dataHomologacao;
        resumo.dataReabertura = origem.dataReabertura;
        resumo.motivoReabertura = origem.motivoReabertura;
        return resumo;
    }

    private Turma turmaResumida(Turma origem) {
        if (origem == null) return null;
        Turma resumo = new Turma();
        resumo.id = origem.id;
        resumo.nome = origem.nome;
        return resumo;
    }

    private AnoLetivo anoLetivoResumido(AnoLetivo origem) {
        if (origem == null) return null;
        AnoLetivo resumo = new AnoLetivo();
        resumo.id = origem.id;
        resumo.ano = origem.ano;
        return resumo;
    }

    private PeriodoLetivo periodoLetivoResumido(PeriodoLetivo origem) {
        if (origem == null) return null;
        PeriodoLetivo resumo = new PeriodoLetivo();
        resumo.id = origem.id;
        resumo.nome = origem.nome;
        return resumo;
    }

    private Curso cursoResumido(Curso origem) {
        if (origem == null) return null;
        Curso resumo = new Curso();
        resumo.id = origem.id;
        resumo.nome = origem.nome;
        return resumo;
    }

    private Modulo moduloResumido(Modulo origem) {
        if (origem == null) return null;
        Modulo resumo = new Modulo();
        resumo.id = origem.id;
        resumo.nome = origem.nome;
        return resumo;
    }

    private Disciplina disciplinaResumida(Disciplina origem) {
        if (origem == null) return null;
        Disciplina resumo = new Disciplina();
        resumo.id = origem.id;
        resumo.nome = origem.nome;
        resumo.codigo = origem.codigo;
        resumo.modulo = moduloResumido(origem.modulo);
        resumo.moduloOriginal = moduloResumido(origem.moduloOriginal);
        return resumo;
    }

    private Professor professorResumido(Professor origem) {
        if (origem == null) return null;
        Professor resumo = new Professor();
        resumo.id = origem.id;
        resumo.nome = origem.nome;
        resumo.email = origem.email;
        return resumo;
    }

    private void validarAlteracaoStatus(OfertaDisciplina existente, OfertaDisciplina recebida) {
        if (List.of(StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO,
                StatusOfertaDisciplina.CONCLUIDA, StatusOfertaDisciplina.ENCERRADA)
                .contains(existente.status)) {
            throw new ApiException(Response.Status.CONFLICT,
                    "A oferta está bloqueada. Use o fluxo de reabertura para realizar correções");
        }
        if (recebida.status == null || existente.status == recebida.status) return;
        List<StatusOfertaDisciplina> controlados = List.of(
                StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO,
                StatusOfertaDisciplina.CONCLUIDA);
        if (controlados.contains(existente.status) || controlados.contains(recebida.status)) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Use o fluxo de encerramento e homologacao para alterar este status");
        }
    }

    private void validarOferta(OfertaDisciplina oferta, Long idAtual) {
        if (oferta == null || oferta.disciplina == null || oferta.disciplina.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Disciplina obrigatoria");
        }
        if (oferta.professor == null || oferta.professor.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Professor obrigatorio");
        }
        if (oferta.anoLetivo == null || oferta.anoLetivo.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Ano letivo obrigatorio");
        }
        if (oferta.modulo == null || oferta.modulo.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Modulo de oferta obrigatorio");
        }
        if (oferta.turma == null || oferta.turma.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Turma da oferta obrigatoria");
        }
        Turma turma = Turma.findById(oferta.turma.id);
        AnoLetivo anoLetivo = AnoLetivo.findById(oferta.anoLetivo.id);
        if ((oferta.periodoLetivo == null || oferta.periodoLetivo.id == null)
                && turma != null && turma.periodoLetivo != null) {
            oferta.periodoLetivo = turma.periodoLetivo;
        }
        if (oferta.periodoLetivo == null || oferta.periodoLetivo.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Periodo letivo obrigatorio");
        }
        PeriodoLetivo periodoLetivo = oferta.periodoLetivo == null || oferta.periodoLetivo.id == null
                ? null : PeriodoLetivo.findById(oferta.periodoLetivo.id);
        Curso curso = oferta.curso == null || oferta.curso.id == null ? null : Curso.findById(oferta.curso.id);
        Modulo modulo = Modulo.findById(oferta.modulo.id);
        Disciplina disciplina = Disciplina.findById(oferta.disciplina.id);
        Professor professor = Professor.findById(oferta.professor.id);
        if (turma == null || anoLetivo == null || modulo == null || disciplina == null || professor == null
                || oferta.periodoLetivo != null && oferta.periodoLetivo.id != null && periodoLetivo == null
                || oferta.curso != null && oferta.curso.id != null && curso == null) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "Turma, ano letivo, curso, modulo, disciplina ou professor nao encontrado");
        }
        oferta.turma = turma;
        oferta.anoLetivo = anoLetivo;
        oferta.periodoLetivo = periodoLetivo;
        oferta.curso = curso;
        oferta.modulo = modulo;
        oferta.disciplina = disciplina;
        oferta.professor = professor;
        if (turma.anoLetivo != null && !turma.anoLetivo.id.equals(anoLetivo.id)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "A turma nao pertence ao ano letivo da oferta");
        }
        if (turma.periodoLetivo != null && (periodoLetivo == null
                || !turma.periodoLetivo.id.equals(periodoLetivo.id))) {
            throw new ApiException(Response.Status.BAD_REQUEST, "A turma nao pertence ao periodo letivo da oferta");
        }
        if (turma.curso != null && (curso == null || !turma.curso.id.equals(curso.id))) {
            throw new ApiException(Response.Status.BAD_REQUEST, "A turma nao pertence ao curso de referencia da oferta");
        }
        if (oferta.vagas == null || oferta.vagas <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Vagas obrigatorias");
        }
        if (idAtual != null && MatriculaDisciplina.count(
                "ofertaDisciplina.id = ?1 and status in ?2", idAtual,
                List.of(br.edu.sga.enums.StatusMatriculaDisciplina.ATIVA,
                        br.edu.sga.enums.StatusMatriculaDisciplina.MATRICULADO)) > oferta.vagas) {
            throw new ApiException(Response.Status.CONFLICT,
                    "A quantidade de vagas não pode ser menor que o número de alunos matriculados");
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
        integridadeService.validarDatasOferta(oferta);
        integridadeService.validarConflitosOferta(oferta, idAtual);
    }
}
