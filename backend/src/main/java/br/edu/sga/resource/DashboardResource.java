package br.edu.sga.resource;

import br.edu.sga.entity.*;
import br.edu.sga.enums.StatusAluno;
import br.edu.sga.enums.StatusNota;
import br.edu.sga.enums.StatusTurma;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Map;

@Path("/api/dashboard")
public class DashboardResource {
    @GET
    public Map<String, Object> geral() {
        return Map.of(
                "alunosAtivos", Aluno.count("status", StatusAluno.ATIVO),
                "professoresAtivos", Professor.count("ativo", true),
                "turmasEmAndamento", Turma.count("status", StatusTurma.EM_ANDAMENTO),
                "disciplinasAtivas", Disciplina.count("ativo", true),
                "matriculas", Matricula.count(),
                "notasPendentes", Nota.count("situacao", StatusNota.PENDENTE),
                "historicosDisponiveis", HistoricoEscolar.count()
        );
    }
}
