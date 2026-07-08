package br.edu.sga.resource;

import br.edu.sga.entity.*;
import br.edu.sga.enums.StatusAluno;
import br.edu.sga.enums.StatusAnoLetivo;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.enums.StatusPeriodoLetivo;
import br.edu.sga.enums.StatusNota;
import br.edu.sga.enums.StatusTurma;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/dashboard")
public class DashboardResource {
    @GET
    public Map<String, Object> geral() {
        long vagasOfertadas = OfertaDisciplina.findAll().stream()
                .map(OfertaDisciplina.class::cast)
                .mapToLong(oferta -> oferta.vagas == null ? 0 : oferta.vagas)
                .sum();
        long matriculasAtivasEmOfertas = MatriculaDisciplina.count("status", br.edu.sga.enums.StatusMatriculaDisciplina.MATRICULADO);
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("alunosAtivos", Aluno.count("status", StatusAluno.ATIVO));
        dados.put("professores", Professor.count("ativo", true));
        dados.put("cursos", Curso.count("ativo", true));
        dados.put("modulos", Modulo.count("ativo", true));
        dados.put("disciplinas", Disciplina.count("ativo", true));
        dados.put("turmas", Turma.count());
        dados.put("matriculas", Matricula.count());
        dados.put("anoLetivoAtual", AnoLetivo.count("status", StatusAnoLetivo.EM_ANDAMENTO));
        dados.put("periodoAtual", PeriodoLetivo.count("status", StatusPeriodoLetivo.EM_ANDAMENTO));
        dados.put("turmasAtivas", Turma.count("status in ?1", java.util.List.of(StatusTurma.ABERTA, StatusTurma.EM_ANDAMENTO)));
        dados.put("disciplinasAbertas", OfertaDisciplina.count("status", StatusOfertaDisciplina.ABERTA));
        dados.put("disciplinasEncerradas", OfertaDisciplina.count("status", StatusOfertaDisciplina.ENCERRADA));
        dados.put("ofertasAbertas", OfertaDisciplina.count("status", StatusOfertaDisciplina.ABERTA));
        dados.put("matriculasEmDisciplinas", matriculasAtivasEmOfertas);
        dados.put("matriculasAtivasPeriodoAtual", MatriculaDisciplina.count("status in ?1", java.util.List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO)));
        dados.put("vagasDisponiveis", Math.max(vagasOfertadas - matriculasAtivasEmOfertas, 0));
        dados.put("notasPendentes", Nota.count("situacao", StatusNota.PENDENTE));
        dados.put("historicosDisponiveis", HistoricoEscolar.count());
        dados.put("alunosPorCurso", Aluno.find("curso is not null").stream()
                .map(Aluno.class::cast)
                .collect(java.util.stream.Collectors.groupingBy(aluno -> aluno.curso.nome, LinkedHashMap::new, java.util.stream.Collectors.counting())));
        dados.put("matriculasPorPeriodo", MatriculaDisciplina.find("periodoLetivo is not null").stream()
                .map(MatriculaDisciplina.class::cast)
                .collect(java.util.stream.Collectors.groupingBy(matricula -> matricula.periodoLetivo.nome, LinkedHashMap::new, java.util.stream.Collectors.counting())));
        dados.put("disciplinasPorModulo", Disciplina.find("modulo is not null").stream()
                .map(Disciplina.class::cast)
                .collect(java.util.stream.Collectors.groupingBy(disciplina -> disciplina.modulo.nome, LinkedHashMap::new, java.util.stream.Collectors.counting())));
        dados.put("situacaoMatriculas", MatriculaDisciplina.findAll().stream()
                .map(MatriculaDisciplina.class::cast)
                .collect(java.util.stream.Collectors.groupingBy(matricula -> matricula.status.name(), LinkedHashMap::new, java.util.stream.Collectors.counting())));
        return dados;
    }
}
