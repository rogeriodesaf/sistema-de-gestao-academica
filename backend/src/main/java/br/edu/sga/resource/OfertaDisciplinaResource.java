package br.edu.sga.resource;

import br.edu.sga.entity.AnoLetivo;
import br.edu.sga.entity.Curso;
import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.Modulo;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.PeriodoLetivo;
import br.edu.sga.entity.Professor;
import br.edu.sga.entity.Turma;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.exception.ApiException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
    public OfertaDisciplinaResource() {
        super(OfertaDisciplina.class);
    }

    @GET
    @Override
    public List<OfertaDisciplina> listar(@QueryParam("pagina") @DefaultValue("0") int pagina,
                                         @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        return super.listar(pagina, tamanho).stream().map(this::resumir).toList();
    }

    @POST
    @Transactional
    @Override
    public OfertaDisciplina criar(@Valid OfertaDisciplina oferta) {
        validarOferta(oferta, null);
        oferta.persist();
        return resumir(oferta);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Override
    public OfertaDisciplina atualizar(@PathParam("id") Long id, @Valid OfertaDisciplina oferta) {
        OfertaDisciplina existente = buscar(id);
        validarAlteracaoStatus(existente, oferta);
        validarOferta(oferta, id);
        oferta.id = id;
        OfertaDisciplina atualizada = getEntityManager().merge(oferta);
        return resumir(atualizada);
    }

    private OfertaDisciplina resumir(OfertaDisciplina origem) {
        OfertaDisciplina resumo = new OfertaDisciplina();
        resumo.id = origem.id;
        resumo.turma = turmaResumida(origem.turma);
        resumo.anoLetivo = anoLetivoResumido(origem.anoLetivo);
        resumo.periodoLetivo = periodoLetivoResumido(origem.periodoLetivo);
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
                StatusOfertaDisciplina.EM_ANDAMENTO,
                StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO
        );
        String consultaDuplicidade = "disciplina.id = ?1 and anoLetivo.id = ?2 and modulo.id = ?3 and lower(horario) = ?4 and lower(sala) = ?5 and status in ?6";
        Object[] parametros = {
                oferta.disciplina.id,
                oferta.anoLetivo.id,
                oferta.modulo.id,
                oferta.horario.trim().toLowerCase(),
                oferta.sala.trim().toLowerCase(),
                statusAtivos
        };
        if (idAtual != null) {
            consultaDuplicidade += " and id <> ?7";
            parametros = new Object[] {
                    oferta.disciplina.id,
                    oferta.anoLetivo.id,
                    oferta.modulo.id,
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
