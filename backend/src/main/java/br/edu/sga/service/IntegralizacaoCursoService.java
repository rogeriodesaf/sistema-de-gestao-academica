package br.edu.sga.service;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.Curso;
import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.Matricula;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.enums.StatusHistorico;
import br.edu.sga.enums.StatusMatricula;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.enums.TipoComponenteCurricular;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class IntegralizacaoCursoService {
    private static final List<StatusMatriculaDisciplina> APROVADAS = List.of(
            StatusMatriculaDisciplina.CONCLUIDA, StatusMatriculaDisciplina.CONCLUIDO);

    public record IntegralizacaoDTO(int cargaHorariaTotal, int cargaHorariaCumprida,
                                    int cargaHorariaRestante, int creditosTotais,
                                    int creditosCumpridos, int creditosRestantes,
                                    long disciplinasObrigatorias, long disciplinasConcluidas,
                                    List<String> disciplinasPendentes, BigDecimal percentual,
                                    String situacaoCurso, LocalDate dataConclusao,
                                    boolean concluidoNestaVerificacao) {}

    public IntegralizacaoDTO consultar(Aluno aluno) {
        return calcular(aluno, false);
    }

    @Transactional
    public IntegralizacaoDTO recalcular(Aluno aluno) {
        return calcular(aluno, true);
    }

    @Transactional
    public void recalcularCurso(Curso curso) {
        if (curso == null || curso.id == null) return;
        Aluno.<Aluno>list("curso", curso).forEach(this::recalcular);
    }

    private IntegralizacaoDTO calcular(Aluno aluno, boolean persistir) {
        Curso curso = aluno == null ? null : aluno.curso;
        if (curso == null) {
            return new IntegralizacaoDTO(0, 0, 0, 0, 0, 0,
                    0, 0, List.of(), BigDecimal.ZERO, "SEM_CURSO", null, false);
        }

        List<Disciplina> matriz = Disciplina.list("curso = ?1 and ativo = true order by nome", curso);
        List<Disciplina> obrigatorias = matriz.stream()
                .filter(item -> tipo(item) == TipoComponenteCurricular.OBRIGATORIA).toList();
        List<Disciplina> complementares = matriz.stream()
                .filter(item -> tipo(item) == TipoComponenteCurricular.COMPLEMENTAR).toList();
        List<Disciplina> optativas = matriz.stream()
                .filter(item -> tipo(item) == TipoComponenteCurricular.OPTATIVA).toList();
        Map<Long, Disciplina> aprovadas = new LinkedHashMap<>();
        MatriculaDisciplina.<MatriculaDisciplina>list("aluno = ?1 and status in ?2", aluno, APROVADAS)
                .forEach(item -> adicionarSeDaMatriz(aprovadas, matriz,
                        item.ofertaDisciplina == null ? null : item.ofertaDisciplina.disciplina));
        HistoricoEscolar.<HistoricoEscolar>list("aluno = ?1 and situacao = ?2", aluno, StatusHistorico.APROVADO)
                .stream().filter(item -> item.ofertaDisciplina == null
                        || item.ofertaDisciplina.status == StatusOfertaDisciplina.CONCLUIDA)
                .forEach(item -> adicionarSeDaMatriz(aprovadas, matriz, item.disciplina));

        int creditosObrigatorios = obrigatorias.stream().mapToInt(item -> valor(item.creditos)).sum();
        int creditosComplementares = complementares.stream().mapToInt(item -> valor(item.creditos)).sum();
        int creditosMatriz = matriz.stream().mapToInt(item -> valor(item.creditos)).sum();
        int cargaMatriz = matriz.stream().mapToInt(item -> valor(item.cargaHoraria)).sum();
        int cargaTotal = curso.cargaHorariaTotal == null ? cargaMatriz : curso.cargaHorariaTotal;
        int creditosTotais = curso.creditosTotais == null ? creditosMatriz : curso.creditosTotais;
        int creditosOptativosExigidos = Math.max(creditosTotais - creditosObrigatorios - creditosComplementares, 0);

        Map<Long, Disciplina> aprovadasValidas = new LinkedHashMap<>();
        obrigatorias.stream().filter(item -> aprovadas.containsKey(item.id))
                .forEach(item -> aprovadasValidas.put(item.id, item));
        complementares.stream().filter(item -> aprovadas.containsKey(item.id))
                .forEach(item -> aprovadasValidas.put(item.id, item));
        int creditosOptativosCumpridos = 0;
        for (Disciplina optativa : optativas) {
            if (!aprovadas.containsKey(optativa.id) || creditosOptativosCumpridos >= creditosOptativosExigidos) continue;
            aprovadasValidas.put(optativa.id, optativa);
            creditosOptativosCumpridos += valor(optativa.creditos);
        }

        int cargaCumprida = aprovadasValidas.values().stream().mapToInt(item -> valor(item.cargaHoraria)).sum();
        int creditosCumpridos = aprovadasValidas.values().stream().mapToInt(item -> valor(item.creditos)).sum();
        List<String> pendentes = new ArrayList<>();
        obrigatorias.stream().filter(item -> !aprovadas.containsKey(item.id))
                .map(this::identificacao).forEach(pendentes::add);
        complementares.stream().filter(item -> !aprovadas.containsKey(item.id))
                .map(this::identificacao).forEach(pendentes::add);
        if (creditosOptativosCumpridos < creditosOptativosExigidos) {
            pendentes.add("Disciplinas optativas: "
                    + (creditosOptativosExigidos - creditosOptativosCumpridos) + " créditos pendentes");
        }
        boolean requisitosConcluidos = !matriz.isEmpty() && pendentes.isEmpty()
                && cargaCumprida >= cargaTotal && creditosCumpridos >= creditosTotais;

        Matricula matriculaCurso = matriculaCurso(aluno, curso);
        StatusMatricula statusAnterior = matriculaCurso == null ? null : matriculaCurso.status;
        boolean bloqueada = statusAnterior == StatusMatricula.CANCELADA || statusAnterior == StatusMatricula.TRANCADA;
        boolean concluidoAgora = false;
        if (persistir) {
            if (matriculaCurso == null) {
                matriculaCurso = new Matricula();
                matriculaCurso.aluno = aluno;
                matriculaCurso.curso = curso;
                matriculaCurso.status = StatusMatricula.EM_ANDAMENTO;
                matriculaCurso.persist();
            }
            if (!bloqueada && requisitosConcluidos) {
                concluidoAgora = matriculaCurso.status != StatusMatricula.CONCLUIDO;
                matriculaCurso.status = StatusMatricula.CONCLUIDO;
                if (matriculaCurso.dataConclusao == null) matriculaCurso.dataConclusao = LocalDate.now();
            } else if (!bloqueada && matriculaCurso.status == StatusMatricula.CONCLUIDO && !requisitosConcluidos) {
                matriculaCurso.status = StatusMatricula.EM_ANDAMENTO;
                matriculaCurso.dataConclusao = null;
            } else if (!bloqueada && matriculaCurso.status == StatusMatricula.ATIVA) {
                matriculaCurso.status = StatusMatricula.EM_ANDAMENTO;
            }
        }

        String situacao = matriculaCurso == null || matriculaCurso.status == StatusMatricula.ATIVA
                ? StatusMatricula.EM_ANDAMENTO.name() : matriculaCurso.status.name();
        LocalDate dataConclusao = matriculaCurso == null ? null : matriculaCurso.dataConclusao;
        double percentualCarga = proporcao(cargaCumprida, cargaTotal);
        double percentualCreditos = proporcao(creditosCumpridos, creditosTotais);
        long componentesExigidos = obrigatorias.size() + complementares.size();
        long componentesConcluidos = obrigatorias.stream().filter(item -> aprovadas.containsKey(item.id)).count()
                + complementares.stream().filter(item -> aprovadas.containsKey(item.id)).count();
        double percentualDisciplinas = proporcao(componentesConcluidos, componentesExigidos);
        BigDecimal percentual = BigDecimal.valueOf(Math.min(percentualCarga,
                        Math.min(percentualCreditos, percentualDisciplinas)))
                .setScale(2, RoundingMode.HALF_UP);
        return new IntegralizacaoDTO(cargaTotal, cargaCumprida, Math.max(cargaTotal - cargaCumprida, 0),
                creditosTotais, creditosCumpridos, Math.max(creditosTotais - creditosCumpridos, 0),
                componentesExigidos, componentesConcluidos, pendentes, percentual, situacao,
                dataConclusao, concluidoAgora);
    }

    private Matricula matriculaCurso(Aluno aluno, Curso curso) {
        return Matricula.find("aluno = ?1 and curso = ?2 and turma is null and disciplina is null", aluno, curso)
                .firstResult();
    }

    private void adicionarSeDaMatriz(Map<Long, Disciplina> aprovadas, List<Disciplina> matriz,
                                     Disciplina disciplina) {
        if (disciplina == null || disciplina.id == null) return;
        boolean pertence = matriz.stream().anyMatch(item -> item.id.equals(disciplina.id));
        if (pertence) aprovadas.putIfAbsent(disciplina.id, disciplina);
    }

    private int valor(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private TipoComponenteCurricular tipo(Disciplina disciplina) {
        return disciplina.tipoComponente == null
                ? TipoComponenteCurricular.OBRIGATORIA : disciplina.tipoComponente;
    }

    private String identificacao(Disciplina disciplina) {
        return disciplina.codigo == null ? disciplina.nome : disciplina.codigo + " - " + disciplina.nome;
    }

    private double proporcao(long cumprido, long exigido) {
        return exigido == 0 ? 0 : Math.min(cumprido * 100.0 / exigido, 100.0);
    }
}
