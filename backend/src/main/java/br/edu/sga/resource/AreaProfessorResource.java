package br.edu.sga.resource;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.ArquivoProfessor;
import br.edu.sga.entity.Avaliacao;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.Frequencia;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.NotaAvaliacao;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.Professor;
import br.edu.sga.enums.Perfil;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusFrequencia;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.enums.TipoVinculoArquivo;
import br.edu.sga.exception.ApiException;
import br.edu.sga.service.ArquivoPdfService;
import br.edu.sga.service.PermissaoService;
import br.edu.sga.service.FrequenciaAcademicaService;
import br.edu.sga.service.ResultadoAcademicoService;
import br.edu.sga.service.FechamentoDiarioService;
import br.edu.sga.service.ProfessorUsuarioService;
import br.edu.sga.service.IntegridadeAcademicaService;
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
import java.util.HashMap;
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
    @Inject ArquivoPdfService arquivoPdfService;
    @Inject PermissaoService permissaoService;
    @Inject FrequenciaAcademicaService frequenciaAcademicaService;
    @Inject ResultadoAcademicoService resultadoAcademicoService;
    @Inject FechamentoDiarioService fechamentoDiarioService;
    @Inject ProfessorUsuarioService professorUsuarioService;
    @Inject IntegridadeAcademicaService integridadeAcademicaService;
    @Inject EntityManager entityManager;
    @Context ContainerRequestContext contexto;

    public record AulaDTO(LocalDate dataAula, String conteudoMinistrado, String observacoes, Integer cargaHorariaAula) {}
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
    public record AulaProfessorDTO(Long id, LocalDate dataAula, String conteudoMinistrado, String observacoes,
                                   Integer cargaHorariaAula, boolean chamadaPreenchida, boolean possuiChamada,
                                   long quantidadeArquivos) {}
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
    public record DiarioProfessorDTO(OfertaProfessorDTO oferta, List<MatriculaResumoDTO> matriculas,
                                     List<AulaProfessorDTO> aulas) {}

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
                left join o.turma t
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
        return new DiarioProfessorDTO(
                ofertaResumo(oferta),
                matriculas.stream().map(this::matriculaResumo).toList(),
                aulasDaOferta(oferta, matriculas.size())
        );
    }

    @GET
    @Path("/ofertas/{ofertaId}/aulas")
    public List<AulaProfessorDTO> listarAulas(@PathParam("ofertaId") Long ofertaId) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        return aulasDaOferta(oferta, alunosMatriculados(oferta));
    }

    @POST
    @Path("/ofertas/{ofertaId}/aulas")
    @Transactional
    public AulaProfessorDTO registrarAula(@PathParam("ofertaId") Long ofertaId, AulaDTO dto) {
        OfertaDisciplina oferta = ofertaPermitida(ofertaId);
        exigirEdicaoLiberada(oferta);
        validarAula(dto);
        integridadeAcademicaService.validarDataNaOferta(dto.dataAula(), oferta, "da aula");
        String conteudo = dto.conteudoMinistrado().trim();
        if (AulaMinistrada.count("ofertaDisciplina = ?1 and dataAula = ?2 and conteudoMinistrado = ?3",
                oferta, dto.dataAula(), conteudo) > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Esta aula ja foi registrada");
        }

        AulaMinistrada aula = novaAula(oferta, dto.dataAula(), conteudo, dto.observacoes(), dto.cargaHorariaAula());
        aula.persist();
        atualizarCargaMinistrada(oferta);
        frequenciaAcademicaService.recalcularOferta(oferta);
        return new AulaProfessorDTO(aula.id, aula.dataAula, aula.conteudoMinistrado, aula.observacoes,
                aula.cargaHorariaAula, false, false, 0);
    }

    @PUT
    @Path("/aulas/{aulaId}")
    @Transactional
    public AulaProfessorDTO editarAula(@PathParam("aulaId") Long aulaId, AulaDTO dto) {
        AulaMinistrada aula = aulaPermitida(aulaId);
        OfertaDisciplina oferta = aula.ofertaDisciplina;
        exigirEdicaoLiberada(oferta);
        validarAula(dto);
        integridadeAcademicaService.validarDataNaOferta(dto.dataAula(), oferta, "da aula");
        String conteudo = dto.conteudoMinistrado().trim();
        if (AulaMinistrada.count(
                "ofertaDisciplina = ?1 and dataAula = ?2 and conteudoMinistrado = ?3 and id <> ?4",
                oferta, dto.dataAula(), conteudo, aula.id) > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Esta aula ja foi registrada");
        }
        aula.dataAula = dto.dataAula();
        aula.conteudoMinistrado = conteudo;
        aula.observacoes = textoOpcional(dto.observacoes());
        aula.cargaHorariaAula = dto.cargaHorariaAula();
        atualizarCargaMinistrada(oferta);
        frequenciaAcademicaService.recalcularOferta(oferta);
        long frequencias = Frequencia.count("aula", aula);
        long arquivos = ArquivoProfessor.count("aula", aula);
        long matriculados = alunosMatriculados(oferta);
        return new AulaProfessorDTO(aula.id, aula.dataAula, aula.conteudoMinistrado, aula.observacoes,
                aula.cargaHorariaAula, matriculados > 0 && frequencias >= matriculados,
                frequencias > 0, arquivos);
    }

    @DELETE
    @Path("/aulas/{aulaId}")
    @Transactional
    public void excluirAula(@PathParam("aulaId") Long aulaId) {
        AulaMinistrada aula = aulaPermitida(aulaId);
        OfertaDisciplina oferta = aula.ofertaDisciplina;
        exigirEdicaoLiberada(oferta);
        if (Frequencia.count("aula", aula) > 0) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Aula com chamada registrada nao pode ser excluida");
        }
        if (ArquivoProfessor.count("aula", aula) > 0) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Remova os PDFs vinculados antes de excluir a aula");
        }
        aula.delete();
        atualizarCargaMinistrada(oferta);
        frequenciaAcademicaService.recalcularOferta(oferta);
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
                left join Frequencia f on f.aula = :aula and f.matriculaDisciplina = m
                where m.ofertaDisciplina = :oferta and m.status in :statusAtivos
                order by a.nome
                """, Object[].class)
                .setParameter("aula", aula)
                .setParameter("oferta", oferta)
                .setParameter("statusAtivos", statusMatriculasAtivas())
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

        List<MatriculaDisciplina> matriculasAtivas = matriculasDaOferta(oferta);
        Map<Long, MatriculaDisciplina> matriculasPorId = new HashMap<>();
        matriculasAtivas.forEach(matricula -> matriculasPorId.put(matricula.id, matricula));
        Map<Long, Frequencia> frequenciasPorMatricula = new HashMap<>();
        Map<Long, Frequencia> frequenciasPorAluno = new HashMap<>();
        Frequencia.<Frequencia>list("aula", aula).forEach(frequencia -> {
            if (frequencia.matriculaDisciplina != null) {
                frequenciasPorMatricula.put(frequencia.matriculaDisciplina.id, frequencia);
            }
            if (frequencia.aluno != null) frequenciasPorAluno.put(frequencia.aluno.id, frequencia);
        });
        Set<Long> matriculasRecebidas = new HashSet<>();
        for (ChamadaItemDTO item : dto.presencas()) {
            if (item == null || item.matriculaId() == null || !matriculasRecebidas.add(item.matriculaId())) {
                throw new ApiException(Response.Status.BAD_REQUEST, "A chamada possui matrícula ausente ou duplicada");
            }
            MatriculaDisciplina matricula = matriculasPorId.get(item.matriculaId());
            if (matricula == null) {
                throw new ApiException(Response.Status.BAD_REQUEST, "Aluno não possui matrícula ativa nesta oferta");
            }
            StatusFrequencia status = statusFrequencia(item.status());
            Frequencia frequencia = frequenciasPorMatricula.get(matricula.id);
            if (frequencia == null) frequencia = frequenciasPorAluno.get(matricula.aluno.id);
            if (frequencia == null) {
                frequencia = new Frequencia();
                frequencia.aula = aula;
                frequencia.aluno = matricula.aluno;
                frequencia.persist();
            }
            frequencia.matriculaDisciplina = matricula;
            frequencia.status = status;
            frequencia.presente = status == StatusFrequencia.PRESENTE;
            frequencia.justificativa = status == StatusFrequencia.JUSTIFICADO ? "JUSTIFICADO" : null;
            frequencia.observacao = textoOpcional(item.observacao());
        }
        frequenciaAcademicaService.recalcularOferta(oferta);
        return Map.of("mensagem", "Chamada salva com sucesso");
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
            throw new ApiException(Response.Status.CONFLICT, "A nota máxima é menor que uma nota já lançada");
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
            throw new ApiException(Response.Status.CONFLICT, "Avaliação com notas ou arquivos não pode ser excluída");
        }
        avaliacao.delete();
    }

    @GET
    @Path("/avaliacoes/{avaliacaoId}/notas")
    public List<NotaLancamentoDTO> buscarNotasAvaliacao(@PathParam("avaliacaoId") Long avaliacaoId) {
        Avaliacao avaliacao = avaliacaoPermitida(avaliacaoId);
        return entityManager.createQuery("""
                select m.id, a.id, a.nome, a.email, a.cpf, n.nota, n.observacao, n.id
                from MatriculaDisciplina m join m.aluno a
                left join NotaAvaliacao n on n.avaliacao = :avaliacao and n.matriculaDisciplina = m
                where m.ofertaDisciplina = :oferta and m.status in :statusAtivos
                order by a.nome
                """, Object[].class)
                .setParameter("avaliacao", avaliacao)
                .setParameter("oferta", avaliacao.ofertaDisciplina)
                .setParameter("statusAtivos", statusMatriculasAtivas())
                .getResultList().stream().map(item -> new NotaLancamentoDTO(
                        (Long) item[0], new AlunoResumoDTO((Long) item[1], (String) item[2],
                        (String) item[3], (String) item[4]), (BigDecimal) item[5],
                        (String) item[6], item[7] != null)).toList();
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
        List<MatriculaDisciplina> matriculasAtivas = matriculasDaOferta(avaliacao.ofertaDisciplina);
        Map<Long, MatriculaDisciplina> matriculasPorId = new HashMap<>();
        matriculasAtivas.forEach(matricula -> matriculasPorId.put(matricula.id, matricula));
        Map<Long, NotaAvaliacao> notasPorMatricula = new HashMap<>();
        Map<Long, NotaAvaliacao> notasPorAluno = new HashMap<>();
        NotaAvaliacao.<NotaAvaliacao>list("avaliacao", avaliacao).forEach(nota -> {
            if (nota.matriculaDisciplina != null) notasPorMatricula.put(nota.matriculaDisciplina.id, nota);
            if (nota.aluno != null) notasPorAluno.put(nota.aluno.id, nota);
        });
        Set<Long> matriculasRecebidas = new HashSet<>();
        for (NotaAvaliacaoItemDTO item : dto.notas()) {
            if (item == null || item.matriculaId() == null || item.nota() == null
                    || !matriculasRecebidas.add(item.matriculaId())) {
                throw new ApiException(Response.Status.BAD_REQUEST, "A lista possui nota ou matrícula inválida");
            }
            if (item.nota().signum() < 0 || item.nota().compareTo(avaliacao.notaMaxima) > 0) {
                throw new ApiException(Response.Status.BAD_REQUEST, "A nota deve estar entre zero e " + avaliacao.notaMaxima);
            }
            MatriculaDisciplina matricula = matriculasPorId.get(item.matriculaId());
            if (matricula == null) {
                throw new ApiException(Response.Status.BAD_REQUEST, "Aluno não possui matrícula ativa nesta oferta");
            }
            NotaAvaliacao nota = notasPorMatricula.get(matricula.id);
            if (nota == null) nota = notasPorAluno.get(matricula.aluno.id);
            boolean novaNota = nota == null;
            if (nota == null) {
                nota = new NotaAvaliacao();
                nota.avaliacao = avaliacao;
                nota.aluno = matricula.aluno;
            }
            nota.matriculaDisciplina = matricula;
            nota.nota = item.nota();
            nota.observacao = textoOpcional(item.observacao());
            nota.dataAtualizacao = LocalDateTime.now();
            if (novaNota) nota.persist();
        }
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

    private Professor professorLogadoObrigatorio() {
        permissaoService.exigir(contexto, Perfil.PROFESSOR);
        return professorUsuarioService.identificarProfessor(permissaoService.usuarioId(contexto));
    }

    private OfertaDisciplina ofertaPermitida(Long id) {
        Professor professor = professorLogadoObrigatorio();
        OfertaDisciplina oferta = OfertaDisciplina.findById(id);
        if (oferta == null) throw new NotFoundException();
        if (!mesmoProfessor(professor, oferta.professor)) {
            throw new ApiException(Response.Status.FORBIDDEN, "Professor não vinculado a esta oferta");
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
                linha[1] == null ? null : new TurmaResumoDTO((Long) linha[1], (String) linha[2], (String) linha[3]),
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
                    "O diário está bloqueado para edição enquanto não estiver em andamento");
        }
    }

    private Long alunosMatriculados(OfertaDisciplina oferta) {
        return MatriculaDisciplina.count("ofertaDisciplina = ?1 and status in ?2", oferta,
                List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO));
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

    private List<AulaProfessorDTO> aulasDaOferta(OfertaDisciplina oferta, long matriculados) {
        return entityManager.createQuery("""
                select a.id, a.dataAula, a.conteudoMinistrado, a.observacoes, a.cargaHorariaAula,
                       count(distinct f.id), count(distinct ar.id)
                from AulaMinistrada a
                left join Frequencia f on f.aula = a and f.matriculaDisciplina.status in :statusAtivos
                left join ArquivoProfessor ar on ar.aula = a
                where a.ofertaDisciplina = :oferta
                group by a.id, a.dataAula, a.conteudoMinistrado, a.observacoes, a.cargaHorariaAula
                order by a.dataAula desc, a.id desc
                """, Object[].class)
                .setParameter("oferta", oferta)
                .setParameter("statusAtivos", statusMatriculasAtivas())
                .getResultList().stream().map(item -> {
                    long frequencias = ((Number) item[5]).longValue();
                    return new AulaProfessorDTO((Long) item[0], (LocalDate) item[1], (String) item[2],
                            (String) item[3], (Integer) item[4], matriculados > 0 && frequencias >= matriculados,
                            frequencias > 0, ((Number) item[6]).longValue());
                }).toList();
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
            throw new ApiException(Response.Status.BAD_REQUEST, "Data da aula obrigatória");
        }
        if (dto.conteudoMinistrado() == null || dto.conteudoMinistrado().isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Conteúdo ministrado obrigatório");
        }
        if (dto.cargaHorariaAula() == null || dto.cargaHorariaAula() <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Carga horária deve ser maior que zero");
        }
    }

    private StatusFrequencia statusFrequencia(String valor) {
        try {
            return StatusFrequencia.valueOf(valor == null ? "" : valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Situação de frequência inválida");
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
                avaliacao.data, avaliacao.notaMaxima, avaliacao.peso, 0);
    }

    private ArquivoResumoDTO arquivoResumo(ArquivoProfessor arquivo) {
        return new ArquivoResumoDTO(arquivo.id, arquivo.titulo, arquivo.nomeOriginal,
                arquivo.tipoVinculo.name(), arquivo.aula == null ? null : arquivo.aula.id,
                arquivo.avaliacao == null ? null : arquivo.avaliacao.id, arquivo.tamanho, arquivo.dataEnvio);
    }

    private void validarAvaliacao(AvaliacaoDTO dto, OfertaDisciplina oferta, Long idAtual) {
        if (dto == null || dto.nome() == null || dto.nome().isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Nome da avaliação obrigatório");
        }
        if (dto.ordem() == null || dto.ordem() <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Ordem da avaliação deve ser maior que zero");
        }
        if (dto.notaMaxima() == null || dto.notaMaxima().signum() <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Nota máxima deve ser maior que zero");
        }
        if (dto.peso() == null || dto.peso().signum() <= 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Peso deve ser maior que zero");
        }
        integridadeAcademicaService.validarDataNaOferta(dto.data(), oferta, "da avaliação");
        long mesmaOrdem = idAtual == null
                ? Avaliacao.count("ofertaDisciplina = ?1 and ordem = ?2", oferta, dto.ordem())
                : Avaliacao.count("ofertaDisciplina = ?1 and ordem = ?2 and id <> ?3", oferta, dto.ordem(), idAtual);
        long mesmoNome = idAtual == null
                ? Avaliacao.count("ofertaDisciplina = ?1 and lower(nome) = ?2", oferta, dto.nome().trim().toLowerCase())
                : Avaliacao.count("ofertaDisciplina = ?1 and lower(nome) = ?2 and id <> ?3",
                        oferta, dto.nome().trim().toLowerCase(), idAtual);
        if (mesmaOrdem > 0 || mesmoNome > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Já existe avaliação com este nome ou ordem");
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
        return MatriculaDisciplina.list("ofertaDisciplina = ?1 and status in ?2 order by aluno.nome", oferta,
                statusMatriculasAtivas());
    }

    private List<StatusMatriculaDisciplina> statusMatriculasAtivas() {
        return List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO);
    }

    private ResultadoAlunoDTO resultadoAluno(MatriculaDisciplina matricula,
                                              FrequenciaAcademicaService.ResumoFrequencia frequencia) {
        var resultado = resultadoAcademicoService.calcularPreliminar(matricula, frequencia);
        List<ResultadoNotaDTO> notas = resultado.avaliacoes().stream()
                .map(item -> new ResultadoNotaDTO(item.avaliacaoId(), item.nome(), item.nota())).toList();
        return new ResultadoAlunoDTO(matricula.id, alunoResumo(matricula.aluno), notas,
                resultado.media(), resultado.situacao(), frequencia);
    }

    private void atualizarCargaMinistrada(OfertaDisciplina oferta) {
        Long carga = entityManager.createQuery("""
                select coalesce(sum(a.cargaHorariaAula), 0)
                from AulaMinistrada a where a.ofertaDisciplina = :oferta
                """, Long.class).setParameter("oferta", oferta).getSingleResult();
        oferta.cargaHorariaMinistrada = carga.intValue();
    }
}
