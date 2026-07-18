package br.edu.sga.service;

import br.edu.sga.entity.Avaliacao;
import br.edu.sga.entity.AulaMinistrada;
import br.edu.sga.entity.Frequencia;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.NotaAvaliacao;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.entity.PlanoEnsino;
import br.edu.sga.entity.Professor;
import br.edu.sga.entity.Usuario;
import br.edu.sga.enums.StatusHistorico;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.enums.ResultadoAcademico;
import br.edu.sga.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class FechamentoDiarioService {
    private static final List<StatusMatriculaDisciplina> STATUS_ATIVOS = List.of(
            StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject FrequenciaAcademicaService frequenciaService;
    @Inject ResultadoAcademicoService resultadoService;
    @Inject IntegralizacaoCursoService integralizacaoCursoService;

    public record OperacaoDTO(String mensagem, String status, List<String> pendencias) {}

    public List<String> pendencias(OfertaDisciplina oferta) {
        List<String> pendencias = new ArrayList<>();
        List<MatriculaDisciplina> matriculas = matriculasAtivas(oferta);
        List<AulaMinistrada> aulas = AulaMinistrada.list(
                "ofertaDisciplina = ?1 order by dataAula, id", oferta);
        List<Avaliacao> avaliacoes = Avaliacao.list(
                "ofertaDisciplina = ?1 order by ordem, id", oferta);

        if (PlanoEnsino.count("ofertaDisciplina = ?1 or (ofertaDisciplina is null and disciplina = ?2)",
                oferta, oferta.disciplina) == 0) {
            pendencias.add("Plano de ensino nao cadastrado para a disciplina.");
        }
        if (matriculas.isEmpty()) pendencias.add("Nao ha alunos ativos matriculados.");
        if (aulas.isEmpty()) pendencias.add("Nao ha aulas registradas.");
        int cargaMinistrada = aulas.stream().mapToInt(aula -> aula.cargaHorariaAula == null
                ? 0 : aula.cargaHorariaAula).sum();
        int cargaPrevista = oferta.cargaHorariaPrevista != null ? oferta.cargaHorariaPrevista
                : oferta.disciplina.cargaHoraria == null ? 0 : oferta.disciplina.cargaHoraria;
        if (cargaPrevista > 0 && cargaMinistrada < cargaPrevista) {
            pendencias.add("Carga horaria ministrada de " + cargaMinistrada
                    + "h e menor que a prevista de " + cargaPrevista + "h.");
        }
        for (AulaMinistrada aula : aulas) {
            long chamadas = matriculas.isEmpty() ? 0
                    : Frequencia.count("aula = ?1 and matriculaDisciplina in ?2", aula, matriculas);
            if (chamadas < matriculas.size()) {
                pendencias.add("A aula de " + DATA.format(aula.dataAula) + " esta sem chamada completa.");
            }
        }

        if (avaliacoes.isEmpty()) pendencias.add("Nao ha avaliacoes cadastradas.");
        for (Avaliacao avaliacao : avaliacoes) {
            List<NotaAvaliacao> notas = matriculas.isEmpty() ? List.of() : NotaAvaliacao.list(
                    "avaliacao = ?1 and matriculaDisciplina in ?2", avaliacao, matriculas);
            long invalidas = notas.stream().filter(nota -> nota.nota == null || nota.nota.signum() < 0
                    || nota.nota.compareTo(avaliacao.notaMaxima) > 0).count();
            long semNota = matriculas.size() - notas.stream()
                    .map(nota -> nota.matriculaDisciplina.id).distinct().count();
            if (semNota > 0) {
                pendencias.add("A avaliacao " + avaliacao.nome + " possui " + semNota + " alunos sem nota.");
            }
            if (invalidas > 0) {
                pendencias.add("A avaliacao " + avaliacao.nome + " possui " + invalidas + " notas invalidas.");
            }
        }
        Map<Long, FrequenciaAcademicaService.ResumoFrequencia> frequencias = frequenciaService
                .resumirOferta(oferta).stream().collect(Collectors.toMap(
                        FrequenciaAcademicaService.ResumoFrequencia::matriculaId, Function.identity()));
        for (MatriculaDisciplina matricula : matriculas) {
            var frequencia = frequencias.get(matricula.id);
            var resultado = resultadoService.calcularPreliminar(matricula, frequencia);
            if (!resultado.completo() || resultado.media() == null) {
                pendencias.add("A media final de " + matricula.aluno.nome + " nao pode ser calculada.");
            }
            if (frequencia == null || frequencia.percentualPresenca() == null) {
                pendencias.add("A frequencia final de " + matricula.aluno.nome + " nao pode ser calculada.");
            }
        }
        return pendencias;
    }

    @Transactional
    public OperacaoDTO encerrar(OfertaDisciplina oferta, Professor professor) {
        if (oferta.status != StatusOfertaDisciplina.EM_ANDAMENTO) {
            throw new ApiException(Response.Status.CONFLICT, "Somente uma oferta em andamento pode ser encerrada");
        }
        List<String> pendencias = pendencias(oferta);
        if (!pendencias.isEmpty()) return new OperacaoDTO(
                "O diario possui pendencias.", oferta.status.name(), pendencias);

        resultadoService.atualizarMedias(oferta);
        frequenciaService.recalcularOferta(oferta);
        oferta.status = StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO;
        oferta.dataEncerramento = LocalDateTime.now();
        oferta.encerradoPor = professor;
        return new OperacaoDTO("Diario encerrado e enviado para homologacao.", oferta.status.name(), List.of());
    }

    @Transactional
    public OperacaoDTO reabrir(OfertaDisciplina oferta, Usuario coordenador, String motivo) {
        if (oferta.status != StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO) {
            throw new ApiException(Response.Status.CONFLICT, "Somente um diario aguardando homologacao pode ser reaberto");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Informe o motivo da reabertura");
        }
        oferta.status = StatusOfertaDisciplina.EM_ANDAMENTO;
        oferta.dataReabertura = LocalDateTime.now();
        oferta.reabertoPor = coordenador;
        oferta.motivoReabertura = motivo.trim();
        return new OperacaoDTO("Diario reaberto para correcao.", oferta.status.name(), List.of());
    }

    @Transactional
    public OperacaoDTO homologar(OfertaDisciplina oferta, Usuario coordenador) {
        if (oferta.status != StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO) {
            throw new ApiException(Response.Status.CONFLICT, "Somente um diario aguardando homologacao pode ser homologado");
        }
        List<String> pendencias = pendencias(oferta);
        if (!pendencias.isEmpty()) return new OperacaoDTO(
                "O diario ainda possui pendencias.", oferta.status.name(), pendencias);

        resultadoService.atualizarMedias(oferta);
        Map<Long, FrequenciaAcademicaService.ResumoFrequencia> frequencias = frequenciaService
                .recalcularOferta(oferta).stream().collect(Collectors.toMap(
                        FrequenciaAcademicaService.ResumoFrequencia::matriculaId, Function.identity()));
        LocalDateTime agora = LocalDateTime.now();
        oferta.status = StatusOfertaDisciplina.CONCLUIDA;
        oferta.dataHomologacao = agora;
        oferta.homologadoPor = coordenador;
        List<String> concluintes = new ArrayList<>();
        for (MatriculaDisciplina matricula : matriculasAtivas(oferta)) {
            var frequencia = frequencias.get(matricula.id);
            var resultado = resultadoService.calcularPreliminar(matricula, frequencia);
            String situacao = resultado.situacao();
            matricula.notaFinal = resultado.media();
            matricula.frequenciaFinal = frequencia.percentualPresenca();
            matricula.dataConsolidacao = agora;
            matricula.resultadoAcademico = ResultadoAcademico.valueOf(situacao);
            consolidarHistorico(matricula, situacao);
            var integralizacao = integralizacaoCursoService.recalcular(matricula.aluno);
            if (integralizacao.concluidoNestaVerificacao()) concluintes.add(matricula.aluno.nome);
        }
        String mensagem = concluintes.isEmpty() ? "Diario homologado e resultados consolidados."
                : "Diario homologado. Requisitos academicos do curso concluidos por: "
                + String.join(", ", concluintes) + ".";
        return new OperacaoDTO(mensagem, oferta.status.name(), List.of());
    }

    private void consolidarHistorico(MatriculaDisciplina matricula, String situacao) {
        OfertaDisciplina oferta = matricula.ofertaDisciplina;
        HistoricoEscolar historico = HistoricoEscolar.find(
                "aluno = ?1 and ofertaDisciplina = ?2", matricula.aluno, oferta).firstResult();
        if (historico == null) {
            historico = new HistoricoEscolar();
            historico.aluno = matricula.aluno;
            historico.ofertaDisciplina = oferta;
            historico.matriculaDisciplina = matricula;
            historico.turma = oferta.turma;
            historico.curso = matricula.curso != null ? matricula.curso : oferta.curso;
            historico.disciplina = oferta.disciplina;
            historico.professorResponsavel = oferta.professor;
            historico.cargaHoraria = oferta.cargaHorariaPrevista != null
                    ? oferta.cargaHorariaPrevista : oferta.disciplina.cargaHoraria;
            historico.periodoCursado = oferta.periodoLetivo == null ? null : oferta.periodoLetivo.nome;
            historico.persist();
        }
        historico.matriculaDisciplina = matricula;
        historico.ofertaDisciplina = oferta;
        historico.notaFinal = matricula.notaFinal;
        historico.frequenciaFinal = matricula.frequenciaFinal;
        historico.disciplinaNome = oferta.disciplina.nome;
        historico.disciplinaCodigo = oferta.disciplina.codigo;
        historico.moduloNome = oferta.modulo == null ? null : oferta.modulo.nome;
        historico.creditos = oferta.disciplina.creditos;
        historico.professorNome = oferta.professor == null ? null : oferta.professor.nome;
        historico.dataHomologacao = oferta.dataHomologacao;
        historico.situacao = switch (situacao) {
            case "APROVADO" -> StatusHistorico.APROVADO;
            case "REPROVADO_POR_FREQUENCIA" -> StatusHistorico.REPROVADO_POR_FREQUENCIA;
            case "REPROVADO_POR_NOTA_E_FREQUENCIA" -> StatusHistorico.REPROVADO_POR_NOTA_E_FREQUENCIA;
            default -> StatusHistorico.REPROVADO_POR_NOTA;
        };
    }

    private List<MatriculaDisciplina> matriculasAtivas(OfertaDisciplina oferta) {
        return MatriculaDisciplina.list(
                "ofertaDisciplina = ?1 and status in ?2 order by aluno.nome", oferta, STATUS_ATIVOS);
    }
}
