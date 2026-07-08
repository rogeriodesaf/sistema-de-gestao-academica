package br.edu.sga.resource;

import br.edu.sga.entity.*;
import br.edu.sga.enums.StatusAluno;
import br.edu.sga.enums.StatusAnoLetivo;
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
        dados.put("professoresAtivos", Professor.count("ativo", true));
        dados.put("turmasEmAndamento", Turma.count("status", StatusTurma.EM_ANDAMENTO));
        dados.put("disciplinasAtivas", Disciplina.count("ativo", true));
        dados.put("matriculas", Matricula.count());
        dados.put("anoLetivoAtual", AnoLetivo.count("status", StatusAnoLetivo.EM_ANDAMENTO));
        dados.put("periodoAtual", PeriodoLetivo.count("status", StatusPeriodoLetivo.EM_ANDAMENTO));
        dados.put("turmasAtivas", Turma.count("status in ?1", java.util.List.of(StatusTurma.ABERTA, StatusTurma.EM_ANDAMENTO)));
        dados.put("disciplinasAbertas", OfertaDisciplina.count("status", StatusOfertaDisciplina.ABERTA));
        dados.put("disciplinasEncerradas", OfertaDisciplina.count("status", StatusOfertaDisciplina.ENCERRADA));
        dados.put("ofertasAbertas", OfertaDisciplina.count("status", StatusOfertaDisciplina.ABERTA));
        dados.put("matriculasEmDisciplinas", matriculasAtivasEmOfertas);
        dados.put("vagasDisponiveis", Math.max(vagasOfertadas - matriculasAtivasEmOfertas, 0));
        dados.put("notasPendentes", Nota.count("situacao", StatusNota.PENDENTE));
        dados.put("historicosDisponiveis", HistoricoEscolar.count());
        return dados;
    }
}
