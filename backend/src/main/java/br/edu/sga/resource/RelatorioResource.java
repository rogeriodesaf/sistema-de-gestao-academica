package br.edu.sga.resource;

import br.edu.sga.entity.*;
import br.edu.sga.enums.StatusHistorico;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/relatorios")
public class RelatorioResource {
    public record DesempenhoOfertaDTO(Long ofertaId, Integer ano, String modulo, String disciplina,
                                      String professor, String status, long matriculados, long aprovados,
                                      long reprovados, long emAndamento, BigDecimal mediaNotas,
                                      BigDecimal frequenciaMedia) {}

    @GET
    @Path("/desempenho-ofertas")
    public Map<String, Object> desempenhoOfertas() {
        List<OfertaDisciplina> ofertas = OfertaDisciplina.find("""
                select o from OfertaDisciplina o
                left join fetch o.anoLetivo
                left join fetch o.modulo
                left join fetch o.disciplina
                left join fetch o.professor
                order by o.anoLetivo.ano desc, o.disciplina.nome
                """).list();
        List<MatriculaDisciplina> matriculas = MatriculaDisciplina.list("ofertaDisciplina is not null");
        List<HistoricoEscolar> historicos = HistoricoEscolar.list("matriculaDisciplina is not null");

        Map<Long, List<MatriculaDisciplina>> matriculasPorOferta = new HashMap<>();
        matriculas.stream()
                .filter(matricula -> matricula.status != br.edu.sga.enums.StatusMatriculaDisciplina.CANCELADO)
                .forEach(matricula -> matriculasPorOferta
                        .computeIfAbsent(matricula.ofertaDisciplina.id, id -> new ArrayList<>())
                        .add(matricula));
        Map<Long, HistoricoEscolar> historicoPorMatricula = new HashMap<>();
        historicos.forEach(historico -> historicoPorMatricula.put(historico.matriculaDisciplina.id, historico));

        List<DesempenhoOfertaDTO> linhas = ofertas.stream().map(oferta -> {
            List<MatriculaDisciplina> matriculasOferta = matriculasPorOferta.getOrDefault(oferta.id, List.of());
            long aprovados = 0;
            long reprovados = 0;
            BigDecimal somaNotas = BigDecimal.ZERO;
            BigDecimal somaFrequencias = BigDecimal.ZERO;
            int notasInformadas = 0;
            int frequenciasInformadas = 0;

            for (MatriculaDisciplina matricula : matriculasOferta) {
                HistoricoEscolar historico = historicoPorMatricula.get(matricula.id);
                if (historico != null && historico.situacao == StatusHistorico.APROVADO) aprovados++;
                if (historico != null && List.of(StatusHistorico.REPROVADO, StatusHistorico.REPROVADO_POR_NOTA,
                        StatusHistorico.REPROVADO_POR_FREQUENCIA,
                        StatusHistorico.REPROVADO_POR_NOTA_E_FREQUENCIA).contains(historico.situacao)) reprovados++;
                BigDecimal nota = historico != null && historico.notaFinal != null
                        ? historico.notaFinal : matricula.notaFinal;
                BigDecimal frequencia = historico != null && historico.frequenciaFinal != null
                        ? historico.frequenciaFinal : matricula.frequenciaFinal;
                if (nota != null) {
                    somaNotas = somaNotas.add(nota);
                    notasInformadas++;
                }
                if (frequencia != null) {
                    somaFrequencias = somaFrequencias.add(frequencia);
                    frequenciasInformadas++;
                }
            }

            long total = matriculasOferta.size();
            return new DesempenhoOfertaDTO(oferta.id,
                    oferta.anoLetivo == null ? null : oferta.anoLetivo.ano,
                    oferta.modulo == null ? "Sem modulo" : oferta.modulo.nome,
                    oferta.disciplina == null ? "Disciplina nao informada" : oferta.disciplina.nome,
                    oferta.professor == null ? "Professor nao informado" : oferta.professor.nome,
                    oferta.status == null ? "" : oferta.status.name(), total, aprovados, reprovados,
                    Math.max(total - aprovados - reprovados, 0), media(somaNotas, notasInformadas),
                    media(somaFrequencias, frequenciasInformadas));
        }).toList();

        return Map.of(
                "resumo", Map.of(
                        "ofertas", linhas.size(),
                        "matriculados", linhas.stream().mapToLong(DesempenhoOfertaDTO::matriculados).sum(),
                        "aprovados", linhas.stream().mapToLong(DesempenhoOfertaDTO::aprovados).sum(),
                        "reprovados", linhas.stream().mapToLong(DesempenhoOfertaDTO::reprovados).sum()),
                "ofertas", linhas);
    }

    private BigDecimal media(BigDecimal soma, int quantidade) {
        return quantidade == 0 ? null : soma.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
    }

    @GET
    @Path("/alunos-por-turma")
    public Object alunosPorTurma(@QueryParam("turmaId") Long turmaId) {
        return MatriculaDisciplina.list("ofertaDisciplina.turma.id", turmaId);
    }

    @GET
    @Path("/alunos-por-disciplina")
    public Object alunosPorDisciplina(@QueryParam("ofertaDisciplinaId") Long ofertaDisciplinaId) {
        return MatriculaDisciplina.list("ofertaDisciplina.id", ofertaDisciplinaId);
    }

    @GET
    @Path("/disciplinas-por-periodo")
    public Object disciplinasPorPeriodo(@QueryParam("periodoLetivoId") Long periodoLetivoId) {
        return OfertaDisciplina.list("periodoLetivo.id", periodoLetivoId);
    }

    @GET
    @Path("/carga-horaria-ministrada")
    public Object cargaHorariaMinistrada(@QueryParam("ofertaDisciplinaId") Long ofertaDisciplinaId) {
        return Map.of(
                "ofertaDisciplinaId", ofertaDisciplinaId,
                "cargaHorariaMinistrada", AulaMinistrada.find("ofertaDisciplina.id", ofertaDisciplinaId).stream()
                        .map(AulaMinistrada.class::cast)
                        .mapToInt(aula -> aula.cargaHorariaAula == null ? 0 : aula.cargaHorariaAula)
                        .sum()
        );
    }

    @GET
    @Path("/frequencia-por-disciplina")
    public Object frequenciaPorDisciplina(@QueryParam("disciplinaId") Long disciplinaId) {
        return Frequencia.list("aula.disciplina.id = ?1 or aula.ofertaDisciplina.disciplina.id = ?1", disciplinaId);
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
                "reprovados", HistoricoEscolar.count("situacao in ?1", java.util.List.of(
                        StatusHistorico.REPROVADO, StatusHistorico.REPROVADO_POR_NOTA,
                        StatusHistorico.REPROVADO_POR_FREQUENCIA,
                        StatusHistorico.REPROVADO_POR_NOTA_E_FREQUENCIA)),
                "pendentes", HistoricoEscolar.count("situacao", StatusHistorico.PENDENTE)
        );
    }
}
