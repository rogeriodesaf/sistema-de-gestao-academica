package br.edu.sga.resource;

import br.edu.sga.entity.*;
import br.edu.sga.enums.StatusHistorico;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Map;

@Path("/api/relatorios")
public class RelatorioResource {
    @GET
    @Path("/alunos-por-turma")
    public Object alunosPorTurma(@QueryParam("turmaId") Long turmaId) {
        return Matricula.list("turma.id", turmaId);
    }

    @GET
    @Path("/frequencia-por-disciplina")
    public Object frequenciaPorDisciplina(@QueryParam("disciplinaId") Long disciplinaId) {
        return Frequencia.list("aula.disciplina.id", disciplinaId);
    }

    @GET
    @Path("/notas-por-disciplina")
    public Object notasPorDisciplina(@QueryParam("disciplinaId") Long disciplinaId) {
        return Nota.list("disciplina.id", disciplinaId);
    }

    @GET
    @Path("/historico-aluno")
    public Object historicoAluno(@QueryParam("alunoId") Long alunoId) {
        return HistoricoEscolar.list("aluno.id", alunoId);
    }

    @GET
    @Path("/resultados")
    public Object resultados() {
        return Map.of(
                "aprovados", HistoricoEscolar.count("situacao", StatusHistorico.APROVADO),
                "reprovados", HistoricoEscolar.count("situacao", StatusHistorico.REPROVADO),
                "pendentes", HistoricoEscolar.count("situacao", StatusHistorico.PENDENTE)
        );
    }
}
