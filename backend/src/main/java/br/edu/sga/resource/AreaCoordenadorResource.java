package br.edu.sga.resource;

import br.edu.sga.entity.Avaliacao;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.ArquivoProfessor;
import br.edu.sga.entity.Frequencia;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.NotaAvaliacao;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.Perfil;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.enums.StatusFrequencia;
import br.edu.sga.service.FechamentoDiarioService;
import br.edu.sga.service.FrequenciaAcademicaService;
import br.edu.sga.service.PermissaoService;
import br.edu.sga.service.ResultadoAcademicoService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/api/coordenador")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AreaCoordenadorResource {
    private static final List<StatusMatriculaDisciplina> STATUS_ATIVOS = List.of(
            StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO);

    @Inject PermissaoService permissaoService;
    @Inject FechamentoDiarioService fechamentoService;
    @Inject FrequenciaAcademicaService frequenciaService;
    @Inject ResultadoAcademicoService resultadoService;
    @Context ContainerRequestContext contexto;

    public record DiarioPendenteDTO(Long ofertaId, String disciplina, String turma, String professor,
                                    String periodo, String modulo, long alunos, LocalDateTime encerradoEm,
                                    String status) {}
    public record AulaDTO(Long id, LocalDate data, String conteudo, Integer cargaHoraria,
                          boolean chamadaPreenchida) {}
    public record AvaliacaoDTO(Long id, String nome, BigDecimal notaMaxima, BigDecimal peso,
                               long notasLancadas, long alunos) {}
    public record NotaAlunoDTO(String avaliacao, BigDecimal nota) {}
    public record ResultadoDTO(Long matriculaId, String aluno, BigDecimal media, BigDecimal frequencia,
                               String situacaoPreliminar, List<NotaAlunoDTO> notas) {}
    public record DiarioDTO(DiarioPendenteDTO oferta, List<AulaDTO> aulas,
                            List<AvaliacaoDTO> avaliacoes, List<ResultadoDTO> resultados,
                            List<String> pendencias) {}
    public record ReaberturaDTO(String motivo) {}
    public record AulaConsultaDTO(Long id, LocalDate data, Long anoLetivoId, Integer anoLetivo,
                                  Long moduloId, String modulo, Long ofertaId, String oferta,
                                  Long professorId, String professor, Long disciplinaId, String disciplina,
                                  String conteudo, Integer cargaHoraria, long alunos, long presencas,
                                  long faltas, String situacaoDiario) {}
    public record AlunoChamadaDTO(String aluno, String situacao, String observacao) {}
    public record ArquivoAulaDTO(Long id, String titulo, String nome) {}
    public record AulaDetalheDTO(AulaConsultaDTO aula, String observacoes, boolean chamadaRegistrada,
                                 List<AlunoChamadaDTO> presentes, List<AlunoChamadaDTO> ausentes,
                                 List<ArquivoAulaDTO> arquivos) {}

    @GET
    @Path("/aulas")
    public List<AulaConsultaDTO> aulas() {
        exigirCoordenador();
        return AulaMinistrada.<AulaMinistrada>list("order by dataAula desc, id desc")
                .stream().map(this::resumoAula).toList();
    }

    @GET
    @Path("/aulas/{aulaId}")
    public AulaDetalheDTO aula(@PathParam("aulaId") Long aulaId) {
        exigirCoordenador();
        AulaMinistrada aula = AulaMinistrada.findById(aulaId);
        if (aula == null) throw new NotFoundException();
        List<Frequencia> frequencias = Frequencia.list("aula = ?1 order by aluno.nome", aula);
        List<AlunoChamadaDTO> presentes = frequencias.stream()
                .filter(this::presente).map(this::alunoChamada).toList();
        List<AlunoChamadaDTO> ausentes = frequencias.stream()
                .filter(frequencia -> !presente(frequencia)).map(this::alunoChamada).toList();
        long alunos = alunosAtivos(aula.ofertaDisciplina);
        List<ArquivoAulaDTO> arquivos = ArquivoProfessor.<ArquivoProfessor>list(
                        "aula = ?1 order by dataEnvio desc", aula).stream()
                .map(arquivo -> new ArquivoAulaDTO(arquivo.id, arquivo.titulo, arquivo.nomeOriginal)).toList();
        return new AulaDetalheDTO(resumoAula(aula), aula.observacoes,
                alunos > 0 && frequencias.size() >= alunos, presentes, ausentes, arquivos);
    }

    @GET
    @Path("/arquivos/{arquivoId}")
    @Produces("application/pdf")
    public Response arquivo(@PathParam("arquivoId") Long arquivoId) {
        exigirCoordenador();
        ArquivoProfessor arquivo = ArquivoProfessor.findById(arquivoId);
        if (arquivo == null || arquivo.aula == null) throw new NotFoundException();
        File pdf = new File(arquivo.caminho);
        if (!pdf.exists()) throw new NotFoundException();
        return Response.ok(pdf, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + arquivo.nomeOriginal + "\"")
                .build();
    }

    @GET
    @Path("/diarios-pendentes")
    public List<DiarioPendenteDTO> pendentes() {
        exigirCoordenador();
        return OfertaDisciplina.<OfertaDisciplina>list(
                "status = ?1 order by dataEncerramento", StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO)
                .stream().map(this::resumo).toList();
    }

    @GET
    @Path("/ofertas/{ofertaId}/diario")
    public DiarioDTO diario(@PathParam("ofertaId") Long ofertaId) {
        exigirCoordenador();
        OfertaDisciplina oferta = ofertaPendente(ofertaId);
        List<MatriculaDisciplina> matriculas = matriculasAtivas(oferta);
        long alunos = matriculas.size();
        List<AulaDTO> aulas = AulaMinistrada.<AulaMinistrada>list(
                "ofertaDisciplina = ?1 order by dataAula, id", oferta).stream()
                .map(aula -> new AulaDTO(aula.id, aula.dataAula, aula.conteudoMinistrado,
                        aula.cargaHorariaAula,
                        Frequencia.count("aula = ?1 and matriculaDisciplina in ?2", aula, matriculas) >= alunos))
                .toList();
        List<Avaliacao> avaliacoesEntidades = Avaliacao.list(
                "ofertaDisciplina = ?1 order by ordem, id", oferta);
        List<AvaliacaoDTO> avaliacoes = avaliacoesEntidades.stream()
                .map(avaliacao -> new AvaliacaoDTO(avaliacao.id, avaliacao.nome, avaliacao.notaMaxima,
                        avaliacao.peso, NotaAvaliacao.count(
                        "avaliacao = ?1 and matriculaDisciplina in ?2", avaliacao, matriculas), alunos))
                .toList();
        Map<Long, FrequenciaAcademicaService.ResumoFrequencia> frequencias = frequenciaService
                .resumirOferta(oferta).stream().collect(Collectors.toMap(
                        FrequenciaAcademicaService.ResumoFrequencia::matriculaId, Function.identity()));
        List<ResultadoDTO> resultados = matriculas.stream().map(matricula -> {
            var frequencia = frequencias.get(matricula.id);
            var resultado = resultadoService.calcularPreliminar(matricula, frequencia);
            List<NotaAlunoDTO> notas = avaliacoesEntidades.stream().map(avaliacao -> {
                NotaAvaliacao nota = NotaAvaliacao.find(
                        "avaliacao = ?1 and matriculaDisciplina = ?2", avaliacao, matricula).firstResult();
                return new NotaAlunoDTO(avaliacao.nome, nota == null ? null : nota.nota);
            }).toList();
            return new ResultadoDTO(matricula.id, matricula.aluno.nome, resultado.media(),
                    frequencia == null ? null : frequencia.percentualPresenca(), resultado.situacao(), notas);
        }).toList();
        return new DiarioDTO(resumo(oferta), aulas, avaliacoes, resultados, fechamentoService.pendencias(oferta));
    }

    @POST
    @Path("/ofertas/{ofertaId}/homologar")
    @Transactional
    public FechamentoDiarioService.OperacaoDTO homologar(@PathParam("ofertaId") Long ofertaId) {
        return fechamentoService.homologar(ofertaPendente(ofertaId), exigirCoordenador());
    }

    @POST
    @Path("/ofertas/{ofertaId}/reabrir")
    @Transactional
    public FechamentoDiarioService.OperacaoDTO reabrir(@PathParam("ofertaId") Long ofertaId,
                                                        ReaberturaDTO dto) {
        return fechamentoService.reabrir(ofertaPendente(ofertaId), exigirCoordenador(),
                dto == null ? null : dto.motivo());
    }

    private Usuario exigirCoordenador() {
        permissaoService.exigir(contexto, Perfil.COORDENADOR);
        Usuario usuario = Usuario.findById(permissaoService.usuarioId(contexto));
        if (usuario == null) throw new NotFoundException();
        return usuario;
    }

    private OfertaDisciplina ofertaPendente(Long id) {
        exigirCoordenador();
        OfertaDisciplina oferta = OfertaDisciplina.findById(id);
        if (oferta == null || oferta.status != StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO) {
            throw new NotFoundException();
        }
        return oferta;
    }

    private DiarioPendenteDTO resumo(OfertaDisciplina oferta) {
        return new DiarioPendenteDTO(oferta.id, oferta.disciplina.nome,
                oferta.turma == null ? null : oferta.turma.nome,
                oferta.professor == null ? null : oferta.professor.nome,
                oferta.periodoLetivo == null ? null : oferta.periodoLetivo.nome,
                oferta.modulo == null ? null : oferta.modulo.nome,
                MatriculaDisciplina.count("ofertaDisciplina = ?1 and status in ?2", oferta, STATUS_ATIVOS),
                oferta.dataEncerramento, oferta.status.name());
    }

    private List<MatriculaDisciplina> matriculasAtivas(OfertaDisciplina oferta) {
        return MatriculaDisciplina.list(
                "ofertaDisciplina = ?1 and status in ?2 order by aluno.nome", oferta, STATUS_ATIVOS);
    }

    private AulaConsultaDTO resumoAula(AulaMinistrada aula) {
        OfertaDisciplina oferta = aula.ofertaDisciplina;
        var disciplina = oferta != null && oferta.disciplina != null ? oferta.disciplina : aula.disciplina;
        var professor = oferta != null && oferta.professor != null ? oferta.professor : aula.professor;
        long alunos = alunosAtivos(oferta);
        List<Frequencia> frequencias = Frequencia.list("aula", aula);
        long presencas = frequencias.stream().filter(this::presente).count();
        return new AulaConsultaDTO(aula.id, aula.dataAula,
                oferta == null || oferta.anoLetivo == null ? null : oferta.anoLetivo.id,
                oferta == null || oferta.anoLetivo == null ? null : oferta.anoLetivo.ano,
                oferta == null || oferta.modulo == null ? null : oferta.modulo.id,
                oferta == null || oferta.modulo == null ? null : oferta.modulo.nome,
                oferta == null ? null : oferta.id,
                oferta == null ? null : oferta.turma == null ? disciplina.nome : oferta.turma.nome,
                professor == null ? null : professor.id, professor == null ? null : professor.nome,
                disciplina == null ? null : disciplina.id, disciplina == null ? null : disciplina.nome,
                aula.conteudoMinistrado, aula.cargaHorariaAula, alunos, presencas,
                frequencias.size() - presencas,
                oferta == null || oferta.status == null ? "SEM_OFERTA" : oferta.status.name());
    }

    private long alunosAtivos(OfertaDisciplina oferta) {
        return oferta == null ? 0 : MatriculaDisciplina.count(
                "ofertaDisciplina = ?1 and status in ?2", oferta, STATUS_ATIVOS);
    }

    private boolean presente(Frequencia frequencia) {
        return frequencia.status == StatusFrequencia.PRESENTE || frequencia.presente;
    }

    private AlunoChamadaDTO alunoChamada(Frequencia frequencia) {
        String nome = frequencia.matriculaDisciplina != null && frequencia.matriculaDisciplina.aluno != null
                ? frequencia.matriculaDisciplina.aluno.nome
                : frequencia.aluno == null ? "Aluno nao identificado" : frequencia.aluno.nome;
        String situacao = frequencia.status == null
                ? presente(frequencia) ? "PRESENTE" : "AUSENTE" : frequencia.status.name();
        return new AlunoChamadaDTO(nome, situacao,
                frequencia.observacao == null ? frequencia.justificativa : frequencia.observacao);
    }
}
