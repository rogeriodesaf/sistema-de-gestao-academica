package br.edu.sga.resource;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.ArquivoProfessor;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.Frequencia;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.PlanoEnsino;
import br.edu.sga.enums.Perfil;
import br.edu.sga.enums.ResultadoAcademico;
import br.edu.sga.enums.StatusFrequencia;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.exception.ApiException;
import br.edu.sga.service.FrequenciaAcademicaService;
import br.edu.sga.service.HistoricoPdfService;
import br.edu.sga.service.IntegralizacaoCursoService;
import br.edu.sga.service.AlunoUsuarioService;
import br.edu.sga.service.PermissaoService;
import br.edu.sga.service.ResultadoAcademicoService;
import br.edu.sga.service.ValidacaoHistoricoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/aluno")
@Produces(MediaType.APPLICATION_JSON)
public class AreaAlunoResource {
    @Inject PermissaoService permissaoService;
    @Inject AlunoUsuarioService alunoUsuarioService;
    @Inject FrequenciaAcademicaService frequenciaService;
    @Inject ResultadoAcademicoService resultadoService;
    @Inject HistoricoPdfService pdfService;
    @Inject ValidacaoHistoricoService validacaoService;
    @Inject IntegralizacaoCursoService integralizacaoCursoService;
    @ConfigProperty(name = "sga.academico.frequencia-minima") BigDecimal frequenciaMinima;
    @Context ContainerRequestContext contexto;

    public record PerfilDTO(String nome, String matricula, String email, String curso,
                            String moduloAtual, String periodoAtual, String status,
                            IntegralizacaoCursoService.IntegralizacaoDTO integralizacao) {}
    public record DisciplinaDTO(Long ofertaId, String nome, String codigo, String turma, String professor,
                                String modulo, String periodo, Integer cargaHoraria, Integer creditos,
                                String horario, String sala, String situacaoMatricula,
                                String resultadoAcademico, String statusOferta, String statusDiario,
                                BigDecimal media, BigDecimal frequencia, boolean resultadoDefinitivo) {}
    public record FrequenciaDTO(long aulasMinistradas, long presencas, long faltas, long justificadas,
                                BigDecimal percentualPresenca, BigDecimal percentualFaltas,
                                BigDecimal frequenciaMinima, String situacao) {}
    public record AulaDTO(LocalDate data, String conteudo, Integer cargaHoraria,
                          String status, String observacoes) {}
    public record ArquivoDTO(Long id, String titulo, String nome, String vinculo,
                             String referencia, Long tamanho, java.time.LocalDateTime enviadoEm) {}
    public record PlanoEnsinoDTO(Long id, String objetivos, String ementa, String conteudoProgramatico,
                                 String metodologia, String criteriosAvaliacao, String bibliografiaBasica,
                                 String bibliografiaComplementar, String arquivoNome, Long arquivoTamanho,
                                 java.time.LocalDateTime atualizadoEm) {}
    public record DetalheDisciplinaDTO(DisciplinaDTO disciplina, FrequenciaDTO frequencia,
                                       List<AulaDTO> aulas, ResultadoAcademicoService.Resultado resultado,
                                       PlanoEnsinoDTO plano, List<ArquivoDTO> arquivos) {}
    public record PortalAlunoDTO(PerfilDTO perfil, List<DisciplinaDTO> disciplinas) {}
    public record HistoricoItemDTO(String periodo, String modulo, String disciplina, String codigo,
                                   Integer cargaHoraria, Integer creditos, String professor, BigDecimal nota,
                                   BigDecimal frequencia, String situacao) {}
    public record ProgressoDTO(long disciplinasPrevistas, long disciplinasConcluidas,
                               int cargaHorariaPrevista, int cargaHorariaConcluida,
                               int cargaHorariaRestante, int creditosPrevistos,
                               int creditosConcluidos, int creditosRestantes,
                               BigDecimal percentualCurso, BigDecimal coeficiente,
                               List<String> disciplinasPendentes, String situacaoCurso,
                               LocalDate dataConclusao) {}
    public record HistoricoDTO(PerfilDTO aluno, List<HistoricoItemDTO> componentes,
                               Map<String, List<HistoricoItemDTO>> porPeriodo, ProgressoDTO progresso) {}

    @GET
    @Path("/portal")
    public PortalAlunoDTO portal() {
        Aluno aluno = alunoLogado();
        List<MatriculaDisciplina> matriculas = matriculas(aluno);
        return new PortalAlunoDTO(perfilDto(aluno, matriculas),
                matriculas.stream().map(item -> disciplinaDto(item, null, null)).toList());
    }

    @GET
    @Path("/me")
    public PerfilDTO perfil() {
        return perfilDto(alunoLogado());
    }

    @GET
    @Path("/disciplinas")
    public List<DisciplinaDTO> disciplinas() {
        Aluno aluno = alunoLogado();
        return matriculas(aluno).stream().map(item -> disciplinaDto(item, null, null)).toList();
    }

    @GET
    @Path("/disciplinas/{ofertaId}")
    public DisciplinaDTO disciplina(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        var resumo = resumoFrequencia(matricula);
        return disciplinaDto(matricula, resultadoService.calcularPreliminar(matricula, resumo), resumo);
    }

    @GET
    @Path("/disciplinas/{ofertaId}/detalhes")
    public DetalheDisciplinaDTO detalhes(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        var resumo = resumoFrequencia(matricula);
        var resultado = resultadoService.calcularPreliminar(matricula, resumo);
        return new DetalheDisciplinaDTO(
                disciplinaDto(matricula, resultado, resumo), frequenciaDto(resumo), aulasDto(matricula),
                resultado, planoDto(matricula.ofertaDisciplina), arquivosDto(matricula.ofertaDisciplina));
    }

    @GET
    @Path("/disciplinas/{ofertaId}/frequencia")
    public FrequenciaDTO frequencia(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        var resumo = resumoFrequencia(matricula);
        return frequenciaDto(resumo);
    }

    @GET
    @Path("/disciplinas/{ofertaId}/aulas")
    public List<AulaDTO> aulas(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        return aulasDto(matricula);
    }

    @GET
    @Path("/disciplinas/{ofertaId}/avaliacoes")
    public ResultadoAcademicoService.Resultado avaliacoes(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        return resultadoService.calcularPreliminar(matricula, resumoFrequencia(matricula));
    }

    @GET
    @Path("/disciplinas/{ofertaId}/arquivos")
    public List<ArquivoDTO> arquivos(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        return arquivosDto(matricula.ofertaDisciplina);
    }

    @GET
    @Path("/disciplinas/{ofertaId}/plano")
    public PlanoEnsinoDTO plano(@PathParam("ofertaId") Long ofertaId) {
        return planoDto(matriculaPermitida(ofertaId).ofertaDisciplina);
    }

    @GET
    @Path("/disciplinas/{ofertaId}/plano/pdf")
    @Produces("application/pdf")
    public Response abrirPlano(@PathParam("ofertaId") Long ofertaId) {
        return respostaPlano(ofertaId, false);
    }

    @GET
    @Path("/disciplinas/{ofertaId}/plano/pdf/download")
    @Produces("application/pdf")
    public Response baixarPlano(@PathParam("ofertaId") Long ofertaId) {
        return respostaPlano(ofertaId, true);
    }

    @GET
    @Path("/arquivos/{arquivoId}")
    @Produces("application/pdf")
    public Response abrirArquivo(@PathParam("arquivoId") Long arquivoId) {
        ArquivoProfessor arquivo = arquivoPermitido(arquivoId);
        File pdf = new File(arquivo.caminho);
        if (!pdf.exists()) throw new ApiException(Response.Status.NOT_FOUND, "Arquivo não encontrado");
        return Response.ok(pdf, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + arquivo.nomeOriginal + "\"")
                .build();
    }

    @GET
    @Path("/arquivos/{arquivoId}/download")
    @Produces("application/pdf")
    public Response baixarArquivo(@PathParam("arquivoId") Long arquivoId) {
        ArquivoProfessor arquivo = arquivoPermitido(arquivoId);
        File pdf = new File(arquivo.caminho);
        if (!pdf.exists()) throw new ApiException(Response.Status.NOT_FOUND, "Arquivo não encontrado");
        return Response.ok(pdf, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.nomeOriginal + "\"")
                .build();
    }

    @GET
    @Path("/historico")
    public HistoricoDTO historico() {
        return montarHistorico(alunoLogado());
    }

    @GET
    @Path("/historico/pdf")
    @Produces("application/pdf")
    public Response historicoPdf() {
        Aluno aluno = alunoLogado();
        HistoricoDTO historico = montarHistorico(aluno);
        Instant emissao = Instant.now();
        String codigo = validacaoService.emitir(aluno.id, emissao);
        List<String> linhas = linhasPdf(historico, codigo, emissao);
        return Response.ok(pdfService.gerar(linhas), "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historico-escolar.pdf\"")
                .build();
    }

    private Aluno alunoLogado() {
        permissaoService.exigir(contexto, Perfil.ALUNO);
        return alunoUsuarioService.identificarAluno(permissaoService.usuarioId(contexto));
    }

    private List<MatriculaDisciplina> matriculas(Aluno aluno) {
        return MatriculaDisciplina.list(
                "aluno = ?1 and status <> ?2 order by ofertaDisciplina.dataInicio desc, ofertaDisciplina.id desc",
                aluno, StatusMatriculaDisciplina.CANCELADO);
    }

    private MatriculaDisciplina matriculaPermitida(Long ofertaId) {
        MatriculaDisciplina matricula = MatriculaDisciplina.find(
                "aluno = ?1 and ofertaDisciplina.id = ?2 and status <> ?3",
                alunoLogado(), ofertaId, StatusMatriculaDisciplina.CANCELADO).firstResult();
        if (matricula == null) throw new ApiException(Response.Status.FORBIDDEN,
                "Esta disciplina não pertence ao aluno autenticado");
        return matricula;
    }

    private ArquivoProfessor arquivoPermitido(Long arquivoId) {
        ArquivoProfessor arquivo = ArquivoProfessor.findById(arquivoId);
        if (arquivo == null || arquivo.ofertaDisciplina == null) {
            throw new ApiException(Response.Status.NOT_FOUND, "Arquivo não encontrado");
        }
        matriculaPermitida(arquivo.ofertaDisciplina.id);
        return arquivo;
    }

    private PerfilDTO perfilDto(Aluno aluno) {
        return perfilDto(aluno, matriculas(aluno));
    }

    private PerfilDTO perfilDto(Aluno aluno, List<MatriculaDisciplina> matriculas) {
        MatriculaDisciplina atual = matriculas.stream()
                .filter(item -> item.status == StatusMatriculaDisciplina.ATIVA
                        || item.status == StatusMatriculaDisciplina.MATRICULADO).findFirst().orElse(null);
        return new PerfilDTO(aluno.nome, String.format("SGA-%06d", aluno.id), aluno.email,
                aluno.curso == null ? "Aluno avulso" : aluno.curso.nome,
                atual == null || atual.ofertaDisciplina.modulo == null ? null : atual.ofertaDisciplina.modulo.nome,
                atual == null || atual.ofertaDisciplina.periodoLetivo == null
                        ? null : atual.ofertaDisciplina.periodoLetivo.nome,
                aluno.status == null ? null : aluno.status.name(), integralizacaoCursoService.consultar(aluno));
    }

    private DisciplinaDTO disciplinaDto(MatriculaDisciplina matricula,
                                        ResultadoAcademicoService.Resultado resultado,
                                        FrequenciaAcademicaService.ResumoFrequencia resumo) {
        OfertaDisciplina oferta = matricula.ofertaDisciplina;
        ResultadoAcademico resultadoPersistido = matricula.resultadoAcademico == null
                ? ResultadoAcademico.EM_ANDAMENTO : matricula.resultadoAcademico;
        String statusOferta = oferta.status == null ? "EM_ANDAMENTO" : oferta.status.name();
        boolean definitivo = resultado != null ? resultado.definitivo()
                : matricula.dataConsolidacao != null && resultadoPersistido != ResultadoAcademico.EM_ANDAMENTO;
        return new DisciplinaDTO(oferta.id, oferta.disciplina.nome, oferta.disciplina.codigo,
                oferta.turma == null ? null : oferta.turma.nome,
                oferta.professor == null ? null : oferta.professor.nome,
                oferta.modulo == null ? null : oferta.modulo.nome,
                oferta.periodoLetivo == null ? null : oferta.periodoLetivo.nome,
                oferta.cargaHorariaPrevista == null ? oferta.disciplina.cargaHoraria : oferta.cargaHorariaPrevista,
                oferta.disciplina.creditos, oferta.horario, oferta.sala,
                matricula.status == null ? null : matricula.status.name(),
                resultado == null ? resultadoPersistido.name() : resultado.situacao(), statusOferta,
                statusDiario(oferta),
                resultado == null ? matricula.notaFinal : resultado.media(),
                resumo == null ? matricula.frequenciaFinal : resumo.percentualPresenca(),
                definitivo);
    }

    private FrequenciaDTO frequenciaDto(FrequenciaAcademicaService.ResumoFrequencia resumo) {
        if (resumo == null) return new FrequenciaDTO(0, 0, 0, 0, null, null,
                frequenciaMinima, "SEM_CHAMADAS");
        return new FrequenciaDTO(resumo.aulasMinistradas(), resumo.presencas(), resumo.faltas(),
                resumo.faltasJustificadas(), resumo.percentualPresenca(), resumo.percentualFaltas(),
                frequenciaMinima, resumo.situacao());
    }

    private String statusDiario(OfertaDisciplina oferta) {
        if (oferta.dataHomologacao != null || oferta.status == StatusOfertaDisciplina.CONCLUIDA) {
            return "HOMOLOGADO";
        }
        if (oferta.dataEncerramento != null || oferta.status == StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO
                || oferta.status == StatusOfertaDisciplina.ENCERRADA) return "ENCERRADO";
        return "EM_ANDAMENTO";
    }

    private FrequenciaAcademicaService.ResumoFrequencia resumoFrequencia(MatriculaDisciplina matricula) {
        return frequenciaService.resumirOferta(matricula.ofertaDisciplina).stream()
                .filter(item -> item.matriculaId().equals(matricula.id)).findFirst().orElse(null);
    }

    private List<AulaDTO> aulasDto(MatriculaDisciplina matricula) {
        List<AulaMinistrada> aulas = AulaMinistrada.list(
                "ofertaDisciplina = ?1 order by dataAula desc, id desc", matricula.ofertaDisciplina);
        Map<Long, Frequencia> frequenciasPorAula = new HashMap<>();
        Frequencia.<Frequencia>list("aula.ofertaDisciplina = ?1 and (matriculaDisciplina = ?2 or "
                        + "(matriculaDisciplina is null and aluno = ?3))",
                matricula.ofertaDisciplina, matricula, matricula.aluno)
                .forEach(registro -> frequenciasPorAula.put(registro.aula.id, registro));
        return aulas.stream().map(aula -> {
            Frequencia registro = frequenciasPorAula.get(aula.id);
            return new AulaDTO(aula.dataAula, aula.conteudoMinistrado, aula.cargaHorariaAula,
                    registro == null ? "NAO_REGISTRADA" : statusFrequencia(registro), aula.observacoes);
        }).toList();
    }

    private String statusFrequencia(Frequencia registro) {
        if (registro.status != null) return registro.status.name();
        if (registro.presente) return StatusFrequencia.PRESENTE.name();
        return registro.justificativa != null && !registro.justificativa.isBlank()
                ? StatusFrequencia.JUSTIFICADO.name() : StatusFrequencia.AUSENTE.name();
    }

    private ArquivoDTO arquivoDto(ArquivoProfessor arquivo) {
        String referencia = arquivo.avaliacao != null ? arquivo.avaliacao.nome
                : arquivo.aula != null && arquivo.aula.dataAula != null ? arquivo.aula.dataAula.toString()
                : arquivo.ofertaDisciplina.disciplina.nome;
        return new ArquivoDTO(arquivo.id, arquivo.titulo, arquivo.nomeOriginal,
                arquivo.tipoVinculo.name(), referencia, arquivo.tamanho, arquivo.dataEnvio);
    }

    private List<ArquivoDTO> arquivosDto(OfertaDisciplina oferta) {
        return ArquivoProfessor.<ArquivoProfessor>list(
                "ofertaDisciplina = ?1 order by dataEnvio desc", oferta)
                .stream().map(this::arquivoDto).toList();
    }

    private PlanoEnsino planoDaOferta(OfertaDisciplina oferta) {
        return PlanoEnsino.find("ofertaDisciplina", oferta).firstResult();
    }

    private PlanoEnsinoDTO planoDto(OfertaDisciplina oferta) {
        PlanoEnsino plano = planoDaOferta(oferta);
        if (plano == null) return null;
        String ementa = plano.ementa == null || plano.ementa.isBlank()
                ? plano.disciplina.ementaResumo : plano.ementa;
        return new PlanoEnsinoDTO(plano.id, plano.objetivos, ementa, plano.conteudoProgramatico,
                plano.metodologia, plano.criteriosAvaliacao, plano.bibliografiaBasica,
                plano.bibliografiaComplementar, plano.planoPdfNome, plano.planoPdfTamanho,
                plano.ultimaAtualizacao);
    }

    private Response respostaPlano(Long ofertaId, boolean download) {
        OfertaDisciplina oferta = matriculaPermitida(ofertaId).ofertaDisciplina;
        PlanoEnsino plano = planoDaOferta(oferta);
        if (plano == null || plano.planoPdfCaminho == null || plano.planoPdfCaminho.isBlank()) {
            throw new ApiException(Response.Status.NOT_FOUND, "Plano de ensino em PDF ainda não disponibilizado");
        }
        File arquivo = new File(plano.planoPdfCaminho);
        if (!arquivo.exists()) {
            throw new ApiException(Response.Status.NOT_FOUND, "Arquivo do plano de ensino não encontrado");
        }
        String disposicao = download ? "attachment" : "inline";
        return Response.ok(arquivo, plano.planoPdfTipo == null ? "application/pdf" : plano.planoPdfTipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposicao + "; filename=\"" + plano.planoPdfNome + "\"").build();
    }

    private HistoricoDTO montarHistorico(Aluno aluno) {
        List<HistoricoItemDTO> componentes = HistoricoEscolar.<HistoricoEscolar>list("aluno", aluno).stream()
                .filter(historico -> historico.disciplina != null || historico.disciplinaNome != null)
                .map(historico -> new HistoricoItemDTO(historico.periodoCursado,
                        historico.moduloNome != null ? historico.moduloNome
                                : historico.ofertaDisciplina == null || historico.ofertaDisciplina.modulo == null
                                ? null : historico.ofertaDisciplina.modulo.nome,
                        historico.disciplinaNome != null ? historico.disciplinaNome
                                : historico.disciplina == null ? "Disciplina não informada" : historico.disciplina.nome,
                        historico.disciplinaCodigo != null ? historico.disciplinaCodigo
                                : historico.disciplina == null ? null : historico.disciplina.codigo,
                        historico.cargaHoraria,
                        historico.creditos != null ? historico.creditos
                                : historico.disciplina == null ? null : historico.disciplina.creditos,
                        historico.professorNome != null ? historico.professorNome
                                : historico.professorResponsavel == null ? null : historico.professorResponsavel.nome,
                        historico.notaFinal, historico.frequenciaFinal,
                        historico.situacao == null ? null : historico.situacao.name()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        componentes.sort(Comparator.comparing(HistoricoItemDTO::periodo,
                Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(HistoricoItemDTO::disciplina));
        Map<String, List<HistoricoItemDTO>> porPeriodo = new LinkedHashMap<>();
        componentes.forEach(item -> porPeriodo.computeIfAbsent(
                item.periodo() == null ? "Sem período" : item.periodo(), chave -> new ArrayList<>()).add(item));
        return new HistoricoDTO(perfilDto(aluno), componentes, porPeriodo, progresso(aluno, componentes));
    }

    private ProgressoDTO progresso(Aluno aluno, List<HistoricoItemDTO> itens) {
        var integralizacao = integralizacaoCursoService.consultar(aluno);
        BigDecimal soma = BigDecimal.ZERO;
        int pesos = 0;
        for (HistoricoItemDTO item : itens) {
            if (item.nota() == null || !List.of("APROVADO", "REPROVADO", "REPROVADO_POR_NOTA",
                    "REPROVADO_POR_FREQUENCIA", "REPROVADO_POR_NOTA_E_FREQUENCIA")
                    .contains(item.situacao())) continue;
            int peso = item.creditos() == null || item.creditos() <= 0 ? 1 : item.creditos();
            soma = soma.add(item.nota().multiply(BigDecimal.valueOf(peso)));
            pesos += peso;
        }
        BigDecimal coeficiente = pesos == 0 ? null : soma.divide(BigDecimal.valueOf(pesos), 2, RoundingMode.HALF_UP);
        return new ProgressoDTO(integralizacao.disciplinasObrigatorias(),
                integralizacao.disciplinasConcluidas(), integralizacao.cargaHorariaTotal(),
                integralizacao.cargaHorariaCumprida(), integralizacao.cargaHorariaRestante(),
                integralizacao.creditosTotais(), integralizacao.creditosCumpridos(),
                integralizacao.creditosRestantes(), integralizacao.percentual(), coeficiente,
                integralizacao.disciplinasPendentes(), integralizacao.situacaoCurso(),
                integralizacao.dataConclusao());
    }

    private List<String> linhasPdf(HistoricoDTO historico, String codigo, Instant emissao) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<String> linhas = new ArrayList<>();
        linhas.add("SGA - SISTEMA DE GESTÃO ACADÊMICA");
        linhas.add("Aluno: " + historico.aluno().nome());
        linhas.add("Matrícula: " + historico.aluno().matricula() + "   Curso: " + historico.aluno().curso());
        linhas.add("Situação acadêmica: " + historico.aluno().status());
        linhas.add(" ");
        linhas.add("PERÍODO | DISCIPLINA | CH | NOTA | FREQUÊNCIA | SITUAÇÃO");
        for (HistoricoItemDTO item : historico.componentes()) {
            linhas.add(valor(item.periodo()) + " | " + valor(item.disciplina()) + " | " + valor(item.cargaHoraria())
                    + " | " + valor(item.nota()) + " | " + valor(item.frequencia()) + "% | " + valor(item.situacao()));
        }
        linhas.add(" ");
        linhas.add("Carga horária concluída: " + historico.progresso().cargaHorariaConcluida()
                + " de " + historico.progresso().cargaHorariaPrevista());
        linhas.add("Carga horária a cumprir: " + historico.progresso().cargaHorariaRestante());
        linhas.add("Créditos concluídos: " + historico.progresso().creditosConcluidos()
                + " de " + historico.progresso().creditosPrevistos());
        linhas.add("Créditos restantes: " + historico.progresso().creditosRestantes());
        linhas.add("Situação do curso: " + historico.progresso().situacaoCurso());
        linhas.add("Data de conclusão: " + valor(historico.progresso().dataConclusao()));
        linhas.add("Coeficiente de rendimento: " + valor(historico.progresso().coeficiente()));
        linhas.add("Emitido em: " + formato.format(emissao.atZone(ZoneId.of("America/Fortaleza"))));
        linhas.add("Código de validação:");
        linhas.add(codigo.substring(0, codigo.length() / 2));
        linhas.add(codigo.substring(codigo.length() / 2));
        linhas.add("Validação pública: GET /api/publico/historicos/{codigo}");
        return linhas;
    }

    private String valor(Object valor) { return valor == null ? "-" : valor.toString(); }
}
