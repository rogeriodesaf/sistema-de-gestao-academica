package br.edu.sga.resource;

import br.edu.sga.entity.Curso;
import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.Modulo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/api/matriz-curricular")
public class MatrizCurricularResource {
    @GET
    public Map<String, Object> inicial() {
        List<Curso> cursos = Curso.list("order by nome");
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("cursos", cursos.stream().map(this::dadosCurso).toList());
        resposta.put("matriz", cursos.isEmpty() ? null : montarMatriz(cursos.getFirst()));
        return resposta;
    }

    @GET
    @Path("/{cursoId}")
    public Map<String, Object> porCurso(@PathParam("cursoId") Long cursoId) {
        Curso curso = Curso.findById(cursoId);
        if (curso == null) throw new NotFoundException();
        return montarMatriz(curso);
    }

    private Map<String, Object> montarMatriz(Curso curso) {
        Long cursoId = curso.id;
        List<Modulo> modulos = Modulo.list(
                "curso.id = ?1 and anoLetivo is null order by ordem", cursoId);
        List<Disciplina> disciplinas = Disciplina.find("""
                select d from Disciplina d
                left join fetch d.moduloOriginal
                left join fetch d.modulo
                where d.curso.id = ?1 and d.ativo = true
                order by d.nome
                """, cursoId).list();
        Map<Long, List<Disciplina>> disciplinasPorModulo = new HashMap<>();
        for (Disciplina disciplina : disciplinas) {
            Modulo moduloOriginal = disciplina.moduloOriginal != null ? disciplina.moduloOriginal : disciplina.modulo;
            if (moduloOriginal != null && moduloOriginal.id != null) {
                disciplinasPorModulo.computeIfAbsent(moduloOriginal.id, id -> new java.util.ArrayList<>()).add(disciplina);
            }
        }
        List<Map<String, Object>> modulosResposta = modulos.stream().map(modulo -> {
            List<Disciplina> disciplinasModulo = disciplinasPorModulo.getOrDefault(modulo.id, List.of());
            int cargaModulo = disciplinasModulo.stream()
                    .mapToInt(disciplina -> disciplina.cargaHoraria == null ? 0 : disciplina.cargaHoraria)
                    .sum();
            int creditosModulo = disciplinasModulo.stream()
                    .mapToInt(disciplina -> disciplina.creditos == null ? 0 : disciplina.creditos)
                    .sum();

            Map<String, Object> dadosModulo = new LinkedHashMap<>();
            dadosModulo.put("id", modulo.id);
            dadosModulo.put("nome", modulo.nome);
            dadosModulo.put("descricao", modulo.descricao);
            dadosModulo.put("status", modulo.status);
            dadosModulo.put("ordem", modulo.ordem);
            dadosModulo.put("cargaHorariaTotal", cargaModulo);
            dadosModulo.put("creditosTotal", creditosModulo);
            dadosModulo.put("disciplinas", disciplinasModulo.stream()
                    .map(disciplina -> dadosDisciplinaMatriz(disciplina, modulo))
                    .toList());
            return dadosModulo;
        }).toList();

        int cargaCalculada = modulosResposta.stream()
                .mapToInt(modulo -> ((Number) modulo.get("cargaHorariaTotal")).intValue())
                .sum();
        int creditosCalculados = modulosResposta.stream()
                .mapToInt(modulo -> ((Number) modulo.get("creditosTotal")).intValue())
                .sum();
        int cargaTotal = curso.cargaHorariaTotal == null ? cargaCalculada : curso.cargaHorariaTotal;
        int creditosTotal = curso.creditosTotais == null ? creditosCalculados : curso.creditosTotais;

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("curso", dadosCurso(curso));
        resposta.put("cargaHorariaTotal", cargaTotal);
        resposta.put("creditosTotal", creditosTotal);
        resposta.put("modulos", modulosResposta);
        return resposta;
    }

    private Map<String, Object> dadosCurso(Curso curso) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", curso.id);
        dados.put("nome", curso.nome);
        dados.put("gradePdfNome", curso.gradePdfNome);
        return dados;
    }

    private Map<String, Object> dadosDisciplinaMatriz(Disciplina disciplina, Modulo moduloOriginalDaMatriz) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", disciplina.id);
        dados.put("nome", disciplina.nome);
        dados.put("codigo", disciplina.codigo);
        dados.put("moduloAtual", resumoModulo(disciplina.modulo));
        dados.put("remanejada", disciplina.modulo != null && disciplina.modulo.id != null
                && !disciplina.modulo.id.equals(moduloOriginalDaMatriz.id));
        dados.put("cargaHoraria", disciplina.cargaHoraria);
        dados.put("creditos", disciplina.creditos);
        dados.put("tipoComponente", disciplina.tipoComponente);
        dados.put("ementaResumo", disciplina.ementaResumo);
        dados.put("ementaPdfNome", disciplina.ementaPdfNome);
        return dados;
    }

    private Map<String, Object> resumoModulo(Modulo modulo) {
        if (modulo == null) return null;
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", modulo.id);
        dados.put("nome", modulo.nome);
        return dados;
    }
}
