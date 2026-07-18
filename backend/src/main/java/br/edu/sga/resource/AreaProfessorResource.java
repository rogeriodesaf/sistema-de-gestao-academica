package br.edu.sga.resource;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.ArquivoProfessor;
import br.edu.sga.entity.Avaliacao;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.Frequencia;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.Nota;
import br.edu.sga.entity.NotaAvaliacao;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.Professor;
import br.edu.sga.enums.Perfil;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusFrequencia;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.enums.TipoVinculoArquivo;
import br.edu.sga.exception.ApiException;
import br.edu.sga.service.AcademicoService;
import br.edu.sga.service.ArquivoPdfService;
import br.edu.sga.service.PermissaoService;
import br.edu.sga.service.FrequenciaAcademicaService;
import br.edu.sga.service.ResultadoAcademicoService;
import br.edu.sga.service.FechamentoDiarioService;
import br.edu.sga.service.ProfessorUsuarioService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/professor")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AreaProfessorResource {
    @Inject AcademicoService academicoService;
    @Inject ArquivoPdfService arquivoPdfService;
    @Inject PermissaoService permissaoService;
    @Inject FrequenciaAcademicaService frequenciaAcademicaService;
    @Inject ResultadoAcademicoService resultadoAcademicoService;
    @Inject FechamentoDiarioService fechamentoDiarioService;
    @Inject ProfessorUsuarioService professorUsuarioService;
    @Inject EntityManager entityManager;
    @Context ContainerRequestContext contexto;

    public record AulaDTO(Long ofertaDisciplinaId, LocalDate dataAula, String conteudoMinistrado, String observacoes, Integer cargaHorariaAula) {}
    public record PresencaAlunoDTO(Long alunoId, boolean presente, String justificativa, String observacao) {}
    public record FrequenciaDTO(Long aulaId, List<PresencaAlunoDTO> presencas) {}
    public record NotaAlunoDTO(Long alunoId, BigDecimal nota1, BigDecimal nota2, BigDecimal trabalho, BigDecimal avaliacaoFinal) {}
    public record NotasDTO(Long ofertaDisciplinaId, List<NotaAlunoDTO> notas) {}
    public record PessoaResumoDTO(Long id, String nome, String email) {}
    public record ModuloResumoDTO(Long id, String nome) {}
    public record PeriodoResumoDTO(Long id, String nome) {}
    public record TurmaResumoDTO(Long id, String nome, String turno) {}
    public record DisciplinaResumoDTO(Long id, String nome, String codigo, Integer cargaHoraria) {}
    public record OfertaProfessorDTO(Long id, TurmaResumoDTO turma, DisciplinaResumoDTO disciplina, ModuloResumoDTO modulo,
                                     PeriodoResumoDTO periodoLetivo, PessoaResumoDTO professor, String horario, String sala,
                                     Integer cargaHorariaPrevista, Integer cargaHorariaMinistrada, String status,
                                     Long alunosMatriculados, String motivoReabertura, LocalDateTime dataEncerramento) {}
    public record AlunoResumoDTO(Long id, String nome, String email, String cpf) {}
    public record MatriculaResumoDTO(Long id, AlunoResumoDTO aluno, String status, BigDecimal notaFinal, BigDecimal frequenciaFinal, String observacoes) {}
    public record AulaResumoDTO(Long id, LocalDate dataAula, String conteudoMinistrado, String observacoes, Integer cargaHorariaAula) {}
    public record AulaProfessorDTO(Long id, LocalDate dataAula, String conteudoMinistrado, String observacoes,
                                   Integer cargaHorariaAula, boolean chamadaPreenchida, long quantidadeArquivos) {}
    public record ChamadaItemDTO(Long matriculaId, String status, String observacao) {}
    public record ChamadaDTO(List<ChamadaItemDTO> presencas) {}
    public record FrequenciaChamadaDTO(Long matriculaId, AlunoResumoDTO aluno, String status,
                                       String observacao, boolean salva) {}
    public record AvaliacaoDTO(String nome, String descricao, Integer ordem, LocalDate data,
                               BigDecimal notaMaxima, BigDecimal peso) {}
    public record AvaliacaoResumoDTO(Long id, String nome, String descricao, Integer ordem, LocalDate data,
                                     BigDecimal notaMaxima, BigDecimal peso, long quantidadeArquivos) {}
    public record NotaAvaliacaoItemDTO(Long matriculaId, BigDecimal nota, String observacao) {}
    public record NotasAvaliacaoDTO(List<NotaAvaliacaoItemDTO> notas) {}
    public record NotaLancamentoDTO(Long matriculaId, AlunoResumoDTO aluno, BigDecimal nota,
                                    String observacao, boolean salva) {}
    public record ResultadoNotaDTO(Long avaliacaoId, String avaliacao, BigDecimal nota) {}
    public record ResultadoAlunoDTO(Long matriculaId, AlunoResumoDTO aluno, List<ResultadoNotaDTO> notas,
                                    BigDecimal media, String situacao,
                                    FrequenciaAcademicaService.ResumoFrequencia frequencia) {}
    public record ArquivoResumoDTO(Long id, String titulo, String nomeOriginal, String tipoVinculo,
                                   Long aulaId, Long avaliacaoId, Long tamanho, LocalDateTime dataEnvio) {}
    public record AulaRefDTO(Long id) {}
    public record FrequenciaResumoDTO(Long id, AulaRefDTO aula, AlunoResumoDTO aluno, boolean presente, String justificativa, String observacao) {}
    public record NotaResumoDTO(Long id, AlunoResumoDTO aluno, BigDecimal nota1, BigDecimal nota2, BigDecimal trabalho,
                                BigDecimal avaliacaoFinal, BigDecimal mediaFinal, String situacao) {}
    public record HistoricoResumoDTO(Long id, AlunoResumoDTO aluno, BigDecimal notaFinal, BigDecimal frequenciaFinal,
                                     String situacao, String periodoCursado) {}
    public record DiarioProfessorDTO(OfertaProfessorDTO oferta, List<MatriculaResumoDTO> matriculas,
                                     List<AulaResumoDTO> aulas, List<FrequenciaResumoDTO> frequencias,
                                     List<NotaResumoDTO> notas, List<HistoricoResumoDTO> historicos) {}

    @GET
    @Path("/ofertas")
    public List<OfertaProfessorDTO> minhasOfertas() {
        Professor professor = professorLogadoObrigatorio();
        List<Object[]> linhas = entityManager.createQuery("""
                select o.id,
                       t.id, t.nome, t.turno,
                       d.id, d.nome, d.codigo, d.cargaHoraria,
                       op.id, op.nome, op.email,
                       mo.id, mo.nome, pe.id, pe.nome,
                       o.horario, o.sala, o.cargaHorariaPrevista, o.cargaHorariaMinistrada, o.status,
                       o.motivoReabertura, o.dataEncerramento,
                       count(m.id)
                from OfertaDisciplina o
                join o.turma t
                join o.disciplina d
                join o.professor op
                left join o.modulo mo
                left join o.periodoLetivo pe
                left join MatriculaDisciplina m on m.ofertaDisciplina = o and m.status in :statusMatricula
                where op.id = :professorId
                group by o.id, t.id, t.nome, t.turno,
                         d.id, d.nome, d.codigo, d.cargaHoraria,
                         op.id, op.nome, op.email,
                         mo.id, mo.nome, pe.id, pe.nome,
                         o.horario, o.sala, o.cargaHorariaPrevista, o.cargaHorariaMinistrada, o.status,
                         o.motivoReabertura, o.dataEncerramento
                order by count(m.id) desc, d.nome
                """, Object[].class)
                .setParameter("professorId", professor.id)
                .setParameter("statusMatricula", List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO))
                .getResultList();
        return linhas.stream().map(this::ofertaResumoProjetado).toList();
    }

    @POST
    @Path("/ofertas/{ofertaId}/encerrar-diario")
    @Transactional
    public FechamentoDiarioService.OperacaoDTO encerrarDiario(@PathParam("ofertaId") Long ofertaId) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        return fechamentoDiarioService.encerrar(oferta, professorLogadoObrigatorio());
    }

    @GET
    @Path("/ofertas/{id}/diario")
    public DiarioProfessorDTO diario(@PathParam("id") Long id) {
        OfertaDisciplina oferta = ofertaPermitida(id);
        List<MatriculaDisciplina> matriculas = matriculasDaOferta(oferta);
        List<AulaMinistrada> aulas = AulaMinistrada.list("ofertaDisciplina = ?1 order by dataAula desc, id desc", oferta);
        List<Frequencia> frequencias = Frequencia.list("aula.ofertaDisciplina = ?1", oferta);
        List<Nota> notas = Nota.list("ofertaDisciplina = ?1", oferta);
        List<HistoricoEscolar> historicos = HistoricoEscolar.list("ofertaDisciplina = ?1", oferta);
        return new DiarioProfessorDTO(
                ofertaResumo(oferta),
                matriculas.stream().map(this::matriculaResumo).toList(),
                aulas.stream().map(this::aulaResumo).toList(),
                frequencias.stream().map(this::frequenciaResumo).toList(),
                notas.stream().map(this::notaResumo).toList(),
                historicos.stream().map(this::historicoResumo).toList()
        );
    }

    @GET
    @Path("/ofertas/{ofertaId}/aulas")
    public List<AulaProfessorDTO> listarAulas(@PathParam("ofertaId") Long ofertaId) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        return AulaMinistrada.<AulaMinistrada>list("ofertaDisciplina = ?1 order by dataAula desc, id desc", oferta)
                .stream().map(this::aulaProfessorResumo).toList();
    }

    @POST
    @Path("/ofertas/{ofertaId}/aulas")
    @Transactional
    public AulaProfessorDTO registrarAula(@PathParam("ofertaId") Long ofertaId, AulaDTO dto) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        exigirEdicaoLiberada(oferta);
        validarAula(dto);
        String conteudo = dto.conteudoMinistrado().trim();
        if (AulaMinistrada.count("ofertaDisciplina = ?1 and dataAula = ?2 and conteudoMinistrado = ?3",
                oferta, dto.dataAula(), conteudo) > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Esta aula ja foi registrada");
        }

        AulaMinistrada aula = novaAula(oferta, dto.dataAula(), conteudo, dto.observacoes(), dto.cargaHorariaAula());
        aula.persist();
        atualizarCargaMinistrada(oferta);
        frequenciaAcademicaService.recalcularOferta(oferta);
        return aulaProfessorResumo(aula);
    }

    @GET
    @Path("/aulas/{aulaId}/frequencias")
    public List<FrequenciaChamadaDTO> buscarChamada(@PathParam("aulaId") Long aulaId) {
        Long ofertaId = ofertaIdDaAula(aulaId);
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        AulaMinistrada aula = entityManager.getReference(AulaMinistrada.class, aulaId);
        List<Object[]> linhas = entityManager.createQuery("""
                select m.id, a.id, a.nome, a.email, a.cpf, f.status, f.observacao, f.id
                from MatriculaDisciplina m join m.aluno a
                left join Frequencia f on f.aula = :aula and f.aluno = a
                where m.ofertaDisciplina = :oferta and m.status not in :statusIgnorados
                order by a.nome
                """, Object[].class)
                .setParameter("aula", aula)
                .setParameter("oferta", oferta)
                .setParameter("statusIgnorados", List.of(
                        StatusMatriculaDisciplina.CANCELADO, StatusMatriculaDisciplina.TRANCADO))
                .getResultList();
        return linhas.stream().map(item -> {
            StatusFrequencia status = item[5] == null ? StatusFrequencia.PRESENTE : (StatusFrequencia) item[5];
            return new FrequenciaChamadaDTO((Long) item[0],
                    new AlunoResumoDTO((Long) item[1], (String) item[2], (String) item[3], (String) item[4]),
                    status.name(), (String) item[6], item[7] != null);
        }).toList();
    }

    @PUT
    @Path("/aulas/{aulaId}/frequencias")
    @Transactional
    public Map<String, Object> salvarChamada(@PathParam("aulaId") Long aulaId, ChamadaDTO dto) {
        Long ofertaId = ofertaIdDaAula(aulaId);
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        AulaMinistrada aula = entityManager.getReference(AulaMinistrada.class, aulaId);
        exigirEdicaoLiberada(oferta);
        if (dto == null || dto.presencas() == null || dto.presencas().isEmpty()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "A chamada deve conter ao menos um aluno");
        }

        Set<Long> matriculasRecebidas = new HashSet<>();
        for (ChamadaItemDTO item : dto.presencas()) {
            if (item == null || item.matriculaId() == null || !matriculasRecebidas.add(item.matriculaId())) {
                throw new ApiException(Response.Status.BAD_REQUEST, "A chamada possui matricula ausente ou duplicada");
            }
            MatriculaDisciplina matricula = MatriculaDisciplina.findById(item.matriculaId());
            if (!matriculaAtivaDaOferta(matricula, oferta)) {
                throw new ApiException(Response.Status.BAD_REQUEST, "Aluno nao matriculado nesta disciplina");
            }
            StatusFrequencia status = statusFrequencia(item.status());
            int atualizadas = entityManager.createQuery("""
                    update Frequencia f set f.matriculaDisciplina = :matricula, f.status = :status,
                        f.presente = :presente, f.justificativa = :justificativa, f.observacao = :observacao
                    where f.aula = :aula and f.aluno = :aluno
                    """).setParameter("matricula", matricula).setParameter("status", status)
                    .setParameter("presente", status == StatusFrequencia.PRESENTE)
                    .setParameter("justificativa", status == StatusFrequencia.JUSTIFICADO ? "JUSTIFICADO" : null)
                    .setParameter("observacao", textoOpcional(item.observacao()))
                    .setParameter("aula", aula).setParameter("aluno", matricula.aluno).executeUpdate();
            if (atualizadas == 0) {
                Frequencia frequencia = new Frequencia();
                frequencia.aula = aula;
                frequencia.aluno = matricula.aluno;
                frequencia.matriculaDisciplina = matricula;
                frequencia.status = status;
                frequencia.presente = status == StatusFrequencia.PRESENTE;
                frequencia.justificativa = status == StatusFrequencia.JUSTIFICADO ? "JUSTIFICADO" : null;
                frequencia.observacao = textoOpcional(item.observacao());
                frequencia.persist();
            }
        }
        frequenciaAcademicaService.recalcularOferta(oferta);
        return Map.of("mensagem", "Aula e chamada salvas com sucesso");
    }

    @GET
    @Path("/ofertas/{ofertaId}/avaliacoes")
    public List<AvaliacaoResumoDTO> listarAvaliacoes(@PathParam("ofertaId") Long ofertaId) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        return Avaliacao.<Avaliacao>list("ofertaDisciplina = ?1 order by ordem, data, id", oferta)
                .stream().map(this::avaliacaoResumo).toList();
    }

    @POST
    @Path("/ofertas/{ofertaId}/avaliacoes")
    @Transactional
    public AvaliacaoResumoDTO criarAvaliacao(@PathParam("ofertaId") Long ofertaId, AvaliacaoDTO dto) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        exigirEdicaoLiberada(oferta);
        validarAvaliacao(dto, oferta, null);
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.ofertaDisciplina = oferta;
        avaliacao.professor = oferta.professor;
        preencherAvaliacao(avaliacao, dto);
        avaliacao.persist();
        return avaliacaoResumo(avaliacao);
    }

    @PUT
    @Path("/avaliacoes/{avaliacaoId}")
    @Transactional
    public AvaliacaoResumoDTO editarAvaliacao(@PathParam("avaliacaoId") Long avaliacaoId, AvaliacaoDTO dto) {
        Avaliacao avaliacao = avaliacaoPermitida(avaliacaoId);
        exigirEdicaoLiberada(avaliacao.ofertaDisciplina);
        validarAvaliacao(dto, avaliacao.ofertaDisciplina, avaliacao.id);
        if (NotaAvaliacao.count("avaliacao = ?1 and nota > ?2", avaliacao, dto.notaMaxima()) > 0) {
            throw new ApiException(Response.Status.CONFLICT, "A nota maxima e menor que uma nota ja lancada");
        }
        preencherAvaliacao(avaliacao, dto);
        return avaliacaoResumo(avaliacao);
    }

    @DELETE
    @Path("/avaliacoes/{avaliacaoId}")
    @Transactional
    public void excluirAvaliacao(@PathParam("avaliacaoId") Long avaliacaoId) {
        Avaliacao avaliacao = avaliacaoPermitida(avaliacaoId);
        exigirEdicaoLiberada(avaliacao.ofertaDisciplina);
        if (NotaAvaliacao.count("avaliacao", avaliacao) > 0 || ArquivoProfessor.count("avaliacao", avaliacao) > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Avaliacao com notas ou arquivos nao pode ser excluida");
        }
        avaliacao.delete();
    }

    @GET
    @Path("/avaliacoes/{avaliacaoId}/notas")
    public List<NotaLancamentoDTO> buscarNotasAvaliacao(@PathParam("avaliacaoId") Long avaliacaoId) {
        Avaliacao avaliacao = avaliacaoPermitida(avaliacaoId);
        return matriculasDaOferta(avaliacao.ofertaDisciplina).stream()
                .map(matricula -> notaLancamento(avaliacao, matricula)).toList();
    }

    @PUT
    @Path("/avaliacoes/{avaliacaoId}/notas")
    @Transactional
    public Map<String, Object> salvarNotasAvaliacao(@PathParam("avaliacaoId") Long avaliacaoId, NotasAvaliacaoDTO dto) {
        Avaliacao avaliacao = avaliacaoPermitida(avaliacaoId);
        exigirEdicaoLiberada(avaliacao.ofertaDisciplina);
        if (dto == null || dto.notas() == null || dto.notas().isEmpty()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Informe ao menos uma nota");
        }
        Set<Long> matriculasRecebidas = new HashSet<>();
        for (NotaAvaliacaoItemDTO item : dto.notas()) {
            if (item == null || item.matriculaId() == null || item.nota() == null
                    || !matriculasRecebidas.add(item.matriculaId())) {
                throw new ApiException(Response.Status.BAD_REQUEST, "A lista possui nota ou matricula invalida");
            }
            if (item.nota().signum() < 0 || item.nota().compareTo(avaliacao.notaMaxima) > 0) {
                throw new ApiException(Response.Status.BAD_REQUEST, "A nota deve estar entre zero e " + avaliacao.notaMaxima);
            }
            MatriculaDisciplina matricula = MatriculaDisciplina.findById(item.matriculaId());
            if (!matriculaAtivaDaOferta(matricula, avaliacao.ofertaDisciplina)) {
                throw new ApiException(Response.Status.BAD_REQUEST, "Aluno nao matriculado nesta disciplina");
            }
            NotaAvaliacao nota = NotaAvaliacao.find("avaliacao = ?1 and aluno = ?2", avaliacao, matricula.aluno).firstResult();
            boolean novaNota = nota == null;
            if (nota == null) {
                nota = new NotaAvaliacao();
                nota.avaliacao = avaliacao;
                nota.aluno = matricula.aluno;
                nota.matriculaDisciplina = matricula;
            }
            nota.nota = item.nota();
            nota.observacao = textoOpcional(item.observacao());
            nota.dataAtualizacao = LocalDateTime.now();
            if (novaNota) nota.persist();
        }
        atualizarMedias(avaliacao.ofertaDisciplina);
        return Map.of("mensagem", "Notas salvas com sucesso");
    }

    @GET
    @Path("/ofertas/{ofertaId}/resultados")
    public List<ResultadoAlunoDTO> resultados(@PathParam("ofertaId") Long ofertaId) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        Map<Long, FrequenciaAcademicaService.ResumoFrequencia> frequencias = frequenciaAcademicaService
                .resumirOferta(oferta).stream().collect(java.util.stream.Collectors.toMap(
                        FrequenciaAcademicaService.ResumoFrequencia::matriculaId, item -> item));
        return matriculasDaOferta(oferta).stream()
                .map(matricula -> resultadoAluno(matricula, frequencias.get(matricula.id))).toList();
    }

    @POST
    @Path("/arquivos")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public ArquivoResumoDTO enviarArquivo(@RestForm("arquivo") FileUpload arquivo,
                                          @RestForm("titulo") String titulo,
                                          @RestForm("ofertaId") Long ofertaId,
                                          @RestForm("aulaId") Long aulaId,
                                          @RestForm("avaliacaoId") Long avaliacaoId) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        exigirEdicaoLiberada(oferta);
        if (titulo == null || titulo.isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Titulo do arquivo obrigatorio");
        }
        AulaMinistrada aula = aulaId == null ? null : aulaPermitida(aulaId);
        Avaliacao avaliacao = avaliacaoId == null ? null : avaliacaoPermitida(avaliacaoId);
        if (aula != null && !oferta.id.equals(aula.ofertaDisciplina.id)
                || avaliacao != null && !oferta.id.equals(avaliacao.ofertaDisciplina.id)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Vinculo do arquivo invalido");
        }
        var salvo = arquivoPdfService.salvarPdf(arquivo, "professor");
        ArquivoProfessor registro = new ArquivoProfessor();
        registro.professor = professorLogadoObrigatorio();
        registro.ofertaDisciplina = oferta;
        registro.aula = aula;
        registro.avaliacao = avaliacao;
        registro.tipoVinculo = avaliacao != null ? TipoVinculoArquivo.AVALIACAO
                : aula != null ? TipoVinculoArquivo.AULA : TipoVinculoArquivo.DISCIPLINA;
        registro.titulo = titulo.trim();
        registro.nomeOriginal = salvo.nome();
        registro.mimeType = salvo.tipo();
        registro.tamanho = salvo.tamanho();
        registro.caminho = salvo.caminho();
        registro.persist();
        return arquivoResumo(registro);
    }

    @GET
    @Path("/ofertas/{ofertaId}/arquivos")
    public List<ArquivoResumoDTO> listarArquivos(@PathParam("ofertaId") Long ofertaId) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        return ArquivoProfessor.<ArquivoProfessor>list("ofertaDisciplina = ?1 order by dataEnvio desc", oferta)
                .stream().map(this::arquivoResumo).toList();
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

    @DELETE
    @Path("/arquivos/{arquivoId}")
    @Transactional
    public void removerArquivo(@PathParam("arquivoId") Long arquivoId) {
        ArquivoProfessor arquivo = arquivoPermitido(arquivoId);
        exigirEdicaoLiberada(arquivo.ofertaDisciplina);
        Professor logado = professorLogadoObrigatorio();
        if (!mesmoProfessor(logado, arquivo.professor)) {
            throw new ApiException(Response.Status.FORBIDDEN, "Somente o responsavel pode remover este arquivo");
        }
        arquivoPdfService.remover(arquivo.caminho);
        arquivo.delete();
    }

    @POST
    @Path("/aulas")
    @Transactional
    public AulaResumoDTO salvarAula(AulaDTO dto) {
        if (dto == null || dto.ofertaDisciplinaId() == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Oferta de disciplina obrigatoria");
        }
        OfertaDisciplina oferta = ofertaPermitida(dto.ofertaDisciplinaId());
        exigirEdicaoLiberada(oferta);
        AulaMinistrada aula = novaAula(oferta, dto.dataAula() == null ? LocalDate.now() : dto.dataAula(),
                dto.conteudoMinistrado(), dto.observacoes(), dto.cargaHorariaAula());
        aula.persist();
        atualizarCargaMinistrada(oferta);
        frequenciaAcademicaService.recalcularOferta(oferta);
        return aulaResumo(aula);
    }

    @POST
    @Path("/frequencias")
    @Transactional
    public Map<String, Object> salvarFrequencias(FrequenciaDTO dto) {
        if (dto == null || dto.aulaId() == null || dto.presencas() == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Aula e lista de presencas sao obrigatorias");
        }
        AulaMinistrada aula = AulaMinistrada.findById(dto.aulaId());
        if (aula == null) throw new NotFoundException();
        ofertaPermitida(aula.ofertaDisciplina.id);
        exigirEdicaoLiberada(aula.ofertaDisciplina);
        for (PresencaAlunoDTO item : dto.presencas()) {
            Aluno aluno = Aluno.findById(item.alunoId());
            if (aluno == null) continue;
            Frequencia frequencia = Frequencia.find("aula = ?1 and aluno = ?2", aula, aluno).firstResult();
            if (frequencia == null) {
                frequencia = new Frequencia();
                frequencia.aula = aula;
                frequencia.aluno = aluno;
            }
            frequencia.presente = item.presente();
            frequencia.justificativa = item.justificativa();
            frequencia.observacao = item.observacao();
            frequencia.status = item.presente() ? StatusFrequencia.PRESENTE
                    : (item.justificativa() == null || item.justificativa().isBlank()
                    ? StatusFrequencia.AUSENTE : StatusFrequencia.JUSTIFICADO);
            frequencia.matriculaDisciplina = MatriculaDisciplina.find(
                    "aluno = ?1 and ofertaDisciplina = ?2", aluno, aula.ofertaDisciplina).firstResult();
            if (frequencia.id == null) frequencia.persist();
        }
        frequenciaAcademicaService.recalcularOferta(aula.ofertaDisciplina);
        return Map.of("mensagem", "Frequencia salva com sucesso");
    }

    @POST
    @Path("/notas")
    @Transactional
    public Map<String, Object> salvarNotas(NotasDTO dto) {
        if (dto == null || dto.ofertaDisciplinaId() == null || dto.notas() == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Oferta e lista de notas sao obrigatorias");
        }
        OfertaDisciplina oferta = ofertaPermitida(dto.ofertaDisciplinaId());
        exigirEdicaoLiberada(oferta);
        for (NotaAlunoDTO item : dto.notas()) {
            Aluno aluno = Aluno.findById(item.alunoId());
            if (aluno == null) continue;
            Nota nota = Nota.find("aluno = ?1 and ofertaDisciplina = ?2", aluno, oferta).firstResult();
            if (nota == null) {
                nota = new Nota();
                nota.aluno = aluno;
                nota.ofertaDisciplina = oferta;
            }
            nota.nota1 = item.nota1();
            nota.nota2 = item.nota2();
            nota.trabalho = item.trabalho();
            nota.avaliacaoFinal = item.avaliacaoFinal();
            academicoService.salvarNota(nota, permissaoService.perfil(contexto), permissaoService.usuarioId(contexto));
        }
        return Map.of("mensagem", "Notas salvas com sucesso");
    }

    private Professor professorLogadoObrigatorio() {
        permissaoService.exigir(contexto, Perfil.PROFESSOR, Perfil.COORDENADOR, Perfil.SECRETARIA);
        return professorUsuarioService.identificarProfessor(permissaoService.usuarioId(contexto));
    }

    private OfertaDisciplina ofertaPermitida(Long id) {
        Professor professor = professorLogadoObrigatorio();
        OfertaDisciplina oferta = OfertaDisciplina.findById(id);
        if (oferta == null) throw new NotFoundException();
        if (!mesmoProfessor(professor, oferta.professor)) {
            throw new ApiException(Response.Status.FORBIDDEN, "Professor nao vinculado a esta disciplina");
        }
        return oferta;
    }

    private boolean mesmoProfessor(Professor logado, Professor vinculado) {
        return logado != null && vinculado != null && logado.id != null && logado.id.equals(vinculado.id);
    }

    private OfertaProfessorDTO ofertaResumo(OfertaDisciplina oferta) {
        return new OfertaProfessorDTO(
                oferta.id,
                turmaResumo(oferta.turma),
                disciplinaResumo(oferta.disciplina),
                oferta.modulo == null ? null : new ModuloResumoDTO(oferta.modulo.id, oferta.modulo.nome),
                oferta.periodoLetivo == null ? null : new PeriodoResumoDTO(oferta.periodoLetivo.id, oferta.periodoLetivo.nome),
                pessoaResumo(oferta.professor),
                oferta.horario,
                oferta.sala,
                oferta.cargaHorariaPrevista,
                oferta.cargaHorariaMinistrada,
                oferta.status == null ? null : oferta.status.name(),
                alunosMatriculados(oferta),
                oferta.motivoReabertura,
                oferta.dataEncerramento
        );
    }

    private OfertaProfessorDTO ofertaResumoProjetado(Object[] linha) {
        PessoaResumoDTO professor = new PessoaResumoDTO((Long) linha[8], (String) linha[9], (String) linha[10]);
        return new OfertaProfessorDTO(
                (Long) linha[0],
                new TurmaResumoDTO((Long) linha[1], (String) linha[2], (String) linha[3]),
                new DisciplinaResumoDTO((Long) linha[4], (String) linha[5], (String) linha[6], (Integer) linha[7]),
                linha[11] == null ? null : new ModuloResumoDTO((Long) linha[11], (String) linha[12]),
                linha[13] == null ? null : new PeriodoResumoDTO((Long) linha[13], (String) linha[14]),
                professor,
                (String) linha[15],
                (String) linha[16],
                (Integer) linha[17],
                (Integer) linha[18],
                linha[19] == null ? null : linha[19].toString(),
                ((Number) linha[22]).longValue(),
                (String) linha[20],
                (LocalDateTime) linha[21]
        );
    }

    private void exigirEdicaoLiberada(OfertaDisciplina oferta) {
        if (oferta.status != StatusOfertaDisciplina.EM_ANDAMENTO) {
            throw new ApiException(Response.Status.CONFLICT,
                    "O diario esta bloqueado para edicao enquanto nao estiver EM_ANDAMENTO");
        }
    }

    private Long alunosMatriculados(OfertaDisciplina oferta) {
        return MatriculaDisciplina.count("ofertaDisciplina = ?1 and status not in ?2", oferta,
                List.of(StatusMatriculaDisciplina.CANCELADO, StatusMatriculaDisciplina.TRANCADO));
    }

    private TurmaResumoDTO turmaResumo(br.edu.sga.entity.Turma turma) {
        if (turma == null) return null;
        return new TurmaResumoDTO(turma.id, turma.nome, turma.turno);
    }

    private DisciplinaResumoDTO disciplinaResumo(br.edu.sga.entity.Disciplina disciplina) {
        if (disciplina == null) return null;
        return new DisciplinaResumoDTO(
                disciplina.id,
                disciplina.nome,
                disciplina.codigo,
                disciplina.cargaHoraria
        );
    }

    private PessoaResumoDTO pessoaResumo(Professor professor) {
        if (professor == null) return null;
        return new PessoaResumoDTO(professor.id, professor.nome, professor.email);
    }

    private AlunoResumoDTO alunoResumo(Aluno aluno) {
        if (aluno == null) return null;
        return new AlunoResumoDTO(aluno.id, aluno.nome, aluno.email, aluno.cpf);
    }

    private MatriculaResumoDTO matriculaResumo(MatriculaDisciplina matricula) {
        return new MatriculaResumoDTO(
                matricula.id,
                alunoResumo(matricula.aluno),
                matricula.status == null ? null : matricula.status.name(),
                matricula.notaFinal,
                matricula.frequenciaFinal,
                matricula.observacoes
        );
    }

    private AulaResumoDTO aulaResumo(AulaMinistrada aula) {
        return new AulaResumoDTO(aula.id, aula.dataAula, aula.conteudoMinistrado, aula.observacoes, aula.cargaHorariaAula);
    }

    private AulaProfessorDTO aulaProfessorResumo(AulaMinistrada aula) {
        long matriculados = alunosMatriculados(aula.ofertaDisciplina);
        long frequencias = Frequencia.count("aula = ?1", aula);
        return new AulaProfessorDTO(aula.id, aula.dataAula, aula.conteudoMinistrado, aula.observacoes,
                aula.cargaHorariaAula, matriculados > 0 && frequencias >= matriculados,
                ArquivoProfessor.count("aula", aula));
    }

    private FrequenciaChamadaDTO frequenciaChamada(AulaMinistrada aula, MatriculaDisciplina matricula) {
        Frequencia frequencia = Frequencia.find("aula = ?1 and aluno = ?2", aula, matricula.aluno).firstResult();
        StatusFrequencia status = frequencia == null || frequencia.status == null
                ? StatusFrequencia.PRESENTE : frequencia.status;
        return new FrequenciaChamadaDTO(matricula.id, alunoResumo(matricula.aluno), status.name(),
                frequencia == null ? null : frequencia.observacao, frequencia != null);
    }

    private AulaMinistrada aulaPermitida(Long aulaId) {
        AulaMinistrada aula = AulaMinistrada.findById(aulaId);
        if (aula == null || aula.ofertaDisciplina == null) throw new NotFoundException();
        ofertaPermitida(aula.ofertaDisciplina.id);
        return aula;
    }

    private Long ofertaIdDaAula(Long aulaId) {
        return entityManager.createQuery(
                "select a.ofertaDisciplina.id from AulaMinistrada a where a.id = :aulaId", Long.class)
                .setParameter("aulaId", aulaId).getResultStream().findFirst().orElseThrow(NotFoundException::new);
    }

    private AulaMinistrada novaAula(OfertaDisciplina oferta, LocalDate data, String conteudo,
                                    String observacoes, Integer cargaHoraria) {
        AulaMinistrada aula = new AulaMinistrada();
        aula.ofertaDisciplina = oferta;
        aula.disciplina = oferta.disciplina;
        aula.turma = oferta.turma;
        aula.professor = oferta.professor;
        aula.dataAula = data;
        aula.conteudoMinistrado = conteudo;
        aula.observacoes = textoOpcional(observacoes);
        aula.cargaHorariaAula = cargaHoraria;
        return aula;
    }

    private void validarAula(AulaDTO dto) {
        if (dto == null || dto.dataAula() == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Data da aula obrigatoria");
        }
        if (dto.conteudoMinistrado() == null || dto.conteudoMinistrado().isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Conteudo ministrado obrigatorio");
        }
        if (dto.cargaHorariaAula() == null || dto.cargaHorariaAula() <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Carga horaria deve ser maior que zero");
        }
    }

    private boolean matriculaAtivaDaOferta(MatriculaDisciplina matricula, OfertaDisciplina oferta) {
        if (matricula == null || matricula.ofertaDisciplina == null || oferta == null
                || !oferta.id.equals(matricula.ofertaDisciplina.id)) return false;
        return matricula.status == StatusMatriculaDisciplina.ATIVA
                || matricula.status == StatusMatriculaDisciplina.MATRICULADO;
    }

    private StatusFrequencia statusFrequencia(String valor) {
        try {
            return StatusFrequencia.valueOf(valor == null ? "" : valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Status de frequencia invalido");
        }
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Avaliacao avaliacaoPermitida(Long avaliacaoId) {
        Avaliacao avaliacao = Avaliacao.findById(avaliacaoId);
        if (avaliacao == null) throw new NotFoundException();
        ofertaPermitida(avaliacao.ofertaDisciplina.id);
        return avaliacao;
    }

    private ArquivoProfessor arquivoPermitido(Long arquivoId) {
        ArquivoProfessor arquivo = ArquivoProfessor.findById(arquivoId);
        if (arquivo == null) throw new NotFoundException();
        ofertaPermitida(arquivo.ofertaDisciplina.id);
        return arquivo;
    }

    private AvaliacaoResumoDTO avaliacaoResumo(Avaliacao avaliacao) {
        return new AvaliacaoResumoDTO(avaliacao.id, avaliacao.nome, avaliacao.descricao, avaliacao.ordem,
                avaliacao.data, avaliacao.notaMaxima, avaliacao.peso,
                ArquivoProfessor.count("avaliacao", avaliacao));
    }

    private ArquivoResumoDTO arquivoResumo(ArquivoProfessor arquivo) {
        return new ArquivoResumoDTO(arquivo.id, arquivo.titulo, arquivo.nomeOriginal,
                arquivo.tipoVinculo.name(), arquivo.aula == null ? null : arquivo.aula.id,
                arquivo.avaliacao == null ? null : arquivo.avaliacao.id, arquivo.tamanho, arquivo.dataEnvio);
    }

    private NotaLancamentoDTO notaLancamento(Avaliacao avaliacao, MatriculaDisciplina matricula) {
        NotaAvaliacao nota = NotaAvaliacao.find("avaliacao = ?1 and aluno = ?2", avaliacao, matricula.aluno).firstResult();
        return new NotaLancamentoDTO(matricula.id, alunoResumo(matricula.aluno),
                nota == null ? null : nota.nota, nota == null ? null : nota.observacao, nota != null);
    }

    private void validarAvaliacao(AvaliacaoDTO dto, OfertaDisciplina oferta, Long idAtual) {
        if (dto == null || dto.nome() == null || dto.nome().isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Nome da avaliacao obrigatorio");
        }
        if (dto.ordem() == null || dto.ordem() <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Ordem da avaliacao deve ser maior que zero");
        }
        if (dto.notaMaxima() == null || dto.notaMaxima().signum() <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Nota maxima deve ser maior que zero");
        }
        if (dto.peso() == null || dto.peso().signum() <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Peso deve ser maior que zero");
        }
        long mesmaOrdem = idAtual == null
                ? Avaliacao.count("ofertaDisciplina = ?1 and ordem = ?2", oferta, dto.ordem())
                : Avaliacao.count("ofertaDisciplina = ?1 and ordem = ?2 and id <> ?3", oferta, dto.ordem(), idAtual);
        long mesmoNome = idAtual == null
                ? Avaliacao.count("ofertaDisciplina = ?1 and lower(nome) = ?2", oferta, dto.nome().trim().toLowerCase())
                : Avaliacao.count("ofertaDisciplina = ?1 and lower(nome) = ?2 and id <> ?3",
                        oferta, dto.nome().trim().toLowerCase(), idAtual);
        if (mesmaOrdem > 0 || mesmoNome > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Ja existe avaliacao com este nome ou ordem");
        }
    }

    private void preencherAvaliacao(Avaliacao avaliacao, AvaliacaoDTO dto) {
        avaliacao.nome = dto.nome().trim();
        avaliacao.descricao = textoOpcional(dto.descricao());
        avaliacao.ordem = dto.ordem();
        avaliacao.data = dto.data();
        avaliacao.notaMaxima = dto.notaMaxima();
        avaliacao.peso = dto.peso();
    }

    private List<MatriculaDisciplina> matriculasDaOferta(OfertaDisciplina oferta) {
        return MatriculaDisciplina.list("ofertaDisciplina = ?1 and status not in ?2 order by aluno.nome", oferta,
                List.of(StatusMatriculaDisciplina.CANCELADO, StatusMatriculaDisciplina.TRANCADO));
    }

    private ResultadoAlunoDTO resultadoAluno(MatriculaDisciplina matricula,
                                              FrequenciaAcademicaService.ResumoFrequencia frequencia) {
        List<Avaliacao> avaliacoes = Avaliacao.list("ofertaDisciplina = ?1 order by ordem, id", matricula.ofertaDisciplina);
        List<ResultadoNotaDTO> notas = avaliacoes.stream().map(avaliacao -> {
            NotaAvaliacao nota = NotaAvaliacao.find("avaliacao = ?1 and aluno = ?2", avaliacao, matricula.aluno).firstResult();
            return new ResultadoNotaDTO(avaliacao.id, avaliacao.nome, nota == null ? null : nota.nota);
        }).toList();
        var resultado = resultadoAcademicoService.calcularPreliminar(matricula, frequencia);
        String situacao = matricula.ofertaDisciplina.status == StatusOfertaDisciplina.CONCLUIDA
                ? resultadoAcademicoService.calcular(matricula, frequencia).situacao()
                : resultado.situacao();
        return new ResultadoAlunoDTO(matricula.id, alunoResumo(matricula.aluno), notas,
                resultado.media(), situacao, frequencia);
    }

    private void atualizarMedias(OfertaDisciplina oferta) {
        resultadoAcademicoService.atualizarMedias(oferta);
    }

    private FrequenciaResumoDTO frequenciaResumo(Frequencia frequencia) {
        return new FrequenciaResumoDTO(
                frequencia.id,
                frequencia.aula == null ? null : new AulaRefDTO(frequencia.aula.id),
                alunoResumo(frequencia.aluno),
                frequencia.presente,
                frequencia.justificativa,
                frequencia.observacao
        );
    }

    private NotaResumoDTO notaResumo(Nota nota) {
        return new NotaResumoDTO(
                nota.id,
                alunoResumo(nota.aluno),
                nota.nota1,
                nota.nota2,
                nota.trabalho,
                nota.avaliacaoFinal,
                nota.mediaFinal,
                nota.situacao == null ? null : nota.situacao.name()
        );
    }

    private HistoricoResumoDTO historicoResumo(HistoricoEscolar historico) {
        return new HistoricoResumoDTO(
                historico.id,
                alunoResumo(historico.aluno),
                historico.notaFinal,
                historico.frequenciaFinal,
                historico.situacao == null ? null : historico.situacao.name(),
                historico.periodoCursado
        );
    }

    private void atualizarCargaMinistrada(OfertaDisciplina oferta) {
        Integer carga = AulaMinistrada.<AulaMinistrada>list("ofertaDisciplina = ?1", oferta).stream()
                .map(aula -> aula.cargaHorariaAula == null ? 0 : aula.cargaHorariaAula)
                .reduce(0, Integer::sum);
        oferta.cargaHorariaMinistrada = carga;
    }
}
