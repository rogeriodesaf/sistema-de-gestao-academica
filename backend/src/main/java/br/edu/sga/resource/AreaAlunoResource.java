package br.edu.sga.resource;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.ArquivoProfessor;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.Frequencia;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.enums.Perfil;
import br.edu.sga.enums.ResultadoAcademico;
import br.edu.sga.enums.StatusFrequencia;
import br.edu.sga.enums.StatusMatriculaDisciplina;
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
import jakarta.ws.rs.NotFoundException;
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
                                String modulo, String periodo, Integer cargaHoraria, String horario,
                                String sala, String status, BigDecimal media, BigDecimal frequencia,
                                String situacao) {}
    public record FrequenciaDTO(long aulasMinistradas, long presencas, long faltas, long justificadas,
                                BigDecimal percentualPresenca, BigDecimal percentualFaltas,
                                BigDecimal frequenciaMinima, String situacao) {}
    public record AulaDTO(LocalDate data, String conteudo, Integer cargaHoraria,
                          String status, String observacao) {}
    public record ArquivoDTO(Long id, String titulo, String nome, String vinculo,
                             String referencia, Long tamanho, java.time.LocalDateTime enviadoEm) {}
    public record HistoricoItemDTO(String periodo, String modulo, String disciplina, String codigo,
                                   Integer cargaHoraria, Integer creditos, BigDecimal nota,
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
    @Path("/me")
    public PerfilDTO perfil() {
        return perfilDto(alunoLogado());
    }

    @GET
    @Path("/disciplinas")
    public List<DisciplinaDTO> disciplinas() {
        Aluno aluno = alunoLogado();
        return matriculas(aluno).stream().map(item -> disciplinaDto(item, false)).toList();
    }

    @GET
    @Path("/disciplinas/{ofertaId}")
    public DisciplinaDTO disciplina(@PathParam("ofertaId") Long ofertaId) {
        return disciplinaDto(matriculaPermitida(ofertaId), true);
    }

    @GET
    @Path("/disciplinas/{ofertaId}/frequencia")
    public FrequenciaDTO frequencia(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        var resumo = resumoFrequencia(matricula);
        if (resumo == null) return new FrequenciaDTO(0, 0, 0, 0, null, null,
                frequenciaMinima, "SEM_CHAMADAS");
        return new FrequenciaDTO(resumo.aulasMinistradas(), resumo.presencas(), resumo.faltas(),
                resumo.faltasJustificadas(), resumo.percentualPresenca(), resumo.percentualFaltas(),
                frequenciaMinima, resumo.situacao());
    }

    @GET
    @Path("/disciplinas/{ofertaId}/aulas")
    public List<AulaDTO> aulas(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        List<AulaMinistrada> aulas = AulaMinistrada.list(
                "ofertaDisciplina = ?1 order by dataAula desc, id desc", matricula.ofertaDisciplina);
        return aulas.stream().map(aula -> {
            Frequencia registro = Frequencia.find("aula = ?1 and aluno = ?2", aula, matricula.aluno).firstResult();
            String status = registro == null ? "NAO_REGISTRADA" : statusFrequencia(registro);
            return new AulaDTO(aula.dataAula, aula.conteudoMinistrado, aula.cargaHorariaAula,
                    status, registro == null ? null : registro.observacao);
        }).toList();
    }

    @GET
    @Path("/disciplinas/{ofertaId}/avaliacoes")
    public ResultadoAcademicoService.Resultado avaliacoes(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        return resultadoService.calcular(matricula, resumoFrequencia(matricula));
    }

    @GET
    @Path("/disciplinas/{ofertaId}/arquivos")
    public List<ArquivoDTO> arquivos(@PathParam("ofertaId") Long ofertaId) {
        MatriculaDisciplina matricula = matriculaPermitida(ofertaId);
        return ArquivoProfessor.<ArquivoProfessor>list(
                "ofertaDisciplina = ?1 order by dataEnvio desc", matricula.ofertaDisciplina)
                .stream().map(this::arquivoDto).toList();
    }

    @GET
    @Path("/arquivos/{arquivoId}")
    @Produces("application/pdf")
    public Response abrirArquivo(@PathParam("arquivoId") Long arquivoId) {
        ArquivoProfessor arquivo = arquivoPermitido(arquivoId);
        File pdf = new File(arquivo.caminho);
        if (!pdf.exists()) throw new NotFoundException();
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
        if (!pdf.exists()) throw new NotFoundException();
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
                "Disciplina nao pertence ao aluno autenticado");
        return matricula;
    }

    private ArquivoProfessor arquivoPermitido(Long arquivoId) {
        ArquivoProfessor arquivo = ArquivoProfessor.findById(arquivoId);
        if (arquivo == null) throw new NotFoundException();
        matriculaPermitida(arquivo.ofertaDisciplina.id);
        return arquivo;
    }

    private PerfilDTO perfilDto(Aluno aluno) {
        MatriculaDisciplina atual = matriculas(aluno).stream()
                .filter(item -> item.status == StatusMatriculaDisciplina.ATIVA
                        || item.status == StatusMatriculaDisciplina.MATRICULADO).findFirst().orElse(null);
        return new PerfilDTO(aluno.nome, String.format("SGA-%06d", aluno.id), aluno.email,
                aluno.curso == null ? null : aluno.curso.nome,
                atual == null || atual.ofertaDisciplina.modulo == null ? null : atual.ofertaDisciplina.modulo.nome,
                atual == null || atual.periodoLetivo == null ? null : atual.periodoLetivo.nome,
                aluno.status == null ? null : aluno.status.name(), integralizacaoCursoService.consultar(aluno));
    }

    private DisciplinaDTO disciplinaDto(MatriculaDisciplina matricula, boolean atualizar) {
        OfertaDisciplina oferta = matricula.ofertaDisciplina;
        var resumo = atualizar ? resumoFrequencia(matricula) : null;
        var resultado = atualizar ? resultadoService.calcular(matricula, resumo) : null;
        return new DisciplinaDTO(oferta.id, oferta.disciplina.nome, oferta.disciplina.codigo,
                oferta.turma == null ? null : oferta.turma.nome,
                oferta.professor == null ? null : oferta.professor.nome,
                oferta.modulo == null ? null : oferta.modulo.nome,
                oferta.periodoLetivo == null ? null : oferta.periodoLetivo.nome,
                oferta.cargaHorariaPrevista == null ? oferta.disciplina.cargaHoraria : oferta.cargaHorariaPrevista,
                oferta.horario, oferta.sala,
                oferta.status == null ? "EM_ANDAMENTO" : oferta.status.name(),
                resultado == null ? matricula.notaFinal : resultado.media(),
                resumo == null ? matricula.frequenciaFinal : resumo.percentualPresenca(),
                resultado == null ? situacaoMatricula(matricula) : resultado.situacao());
    }

    private FrequenciaAcademicaService.ResumoFrequencia resumoFrequencia(MatriculaDisciplina matricula) {
        return frequenciaService.resumirOferta(matricula.ofertaDisciplina).stream()
                .filter(item -> item.matriculaId().equals(matricula.id)).findFirst().orElse(null);
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

    private HistoricoDTO montarHistorico(Aluno aluno) {
        Map<Long, HistoricoItemDTO> itens = new LinkedHashMap<>();
        for (MatriculaDisciplina matricula : matriculas(aluno)) {
            OfertaDisciplina oferta = matricula.ofertaDisciplina;
            String periodo = periodo(oferta, matricula);
            itens.put(oferta.disciplina.id, new HistoricoItemDTO(periodo,
                    oferta.modulo == null ? null : oferta.modulo.nome, oferta.disciplina.nome,
                    oferta.disciplina.codigo, oferta.disciplina.cargaHoraria, oferta.disciplina.creditos,
                    matricula.notaFinal, matricula.frequenciaFinal, situacaoMatricula(matricula)));
        }
        for (HistoricoEscolar historico : HistoricoEscolar.<HistoricoEscolar>list("aluno", aluno)) {
            if (historico.disciplina == null) continue;
            HistoricoItemDTO matricula = itens.get(historico.disciplina.id);
            itens.put(historico.disciplina.id, new HistoricoItemDTO(historico.periodoCursado,
                    historico.moduloNome != null ? historico.moduloNome
                            : historico.ofertaDisciplina == null || historico.ofertaDisciplina.modulo == null ? null
                            : historico.ofertaDisciplina.modulo.nome,
                    historico.disciplinaNome != null ? historico.disciplinaNome : historico.disciplina.nome,
                    historico.disciplinaCodigo != null ? historico.disciplinaCodigo : historico.disciplina.codigo,
                    historico.cargaHoraria,
                    historico.creditos != null ? historico.creditos : historico.disciplina.creditos,
                    historico.notaFinal == null && matricula != null ? matricula.nota() : historico.notaFinal,
                    historico.frequenciaFinal == null && matricula != null ? matricula.frequencia() : historico.frequenciaFinal,
                    historico.situacao == null ? null : historico.situacao.name()));
        }
        List<HistoricoItemDTO> componentes = new ArrayList<>(itens.values());
        componentes.sort(Comparator.comparing(HistoricoItemDTO::periodo,
                Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(HistoricoItemDTO::disciplina));
        Map<String, List<HistoricoItemDTO>> porPeriodo = new LinkedHashMap<>();
        componentes.forEach(item -> porPeriodo.computeIfAbsent(
                item.periodo() == null ? "Sem periodo" : item.periodo(), chave -> new ArrayList<>()).add(item));
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

    private String periodo(OfertaDisciplina oferta, MatriculaDisciplina matricula) {
        if (oferta.anoLetivo != null && oferta.periodoLetivo != null) return oferta.anoLetivo.ano + " - " + oferta.periodoLetivo.nome;
        return matricula.periodoLetivo == null ? null : matricula.periodoLetivo.nome;
    }

    private String situacaoMatricula(MatriculaDisciplina matricula) {
        if (matricula.resultadoAcademico != null
                && matricula.resultadoAcademico != ResultadoAcademico.EM_ANDAMENTO) {
            return matricula.resultadoAcademico.name();
        }
        return switch (matricula.status) {
            case CONCLUIDA, CONCLUIDO -> "APROVADO";
            case REPROVADO_POR_NOTA -> "REPROVADO_POR_NOTA";
            case REPROVADO_POR_FREQUENCIA -> "REPROVADO_POR_FREQUENCIA";
            case REPROVADA -> "REPROVADO";
            case TRANCADO -> "TRANCADO";
            default -> "EM_ANDAMENTO";
        };
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
