package br.edu.sga.resource;

import br.edu.sga.entity.Curso;
import br.edu.sga.entity.Disciplina;
import br.edu.sga.entity.Modulo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/api/matriz-curricular")
public class MatrizCurricularResource {
    @GET
    @Path("/{cursoId}")
    public Map<String, Object> porCurso(@PathParam("cursoId") Long cursoId) {
        Curso curso = Curso.findById(cursoId);
        if (curso == null) throw new NotFoundException();

        List<Modulo> modulos = Modulo.list(
                "curso.id = ?1 and anoLetivo is null order by ordem", cursoId);
        List<Map<String, Object>> modulosResposta = modulos.stream().map(modulo -> {
            List<Disciplina> disciplinas = Disciplina.list(
                    "(moduloOriginal.id = ?1 or (moduloOriginal is null and modulo.id = ?1)) order by nome", modulo.id);
            int cargaModulo = disciplinas.stream()
                    .mapToInt(disciplina -> disciplina.cargaHoraria == null ? 0 : disciplina.cargaHoraria)
                    .sum();
            int creditosModulo = disciplinas.stream()
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
            dadosModulo.put("disciplinas", disciplinas.stream()
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
        resposta.put("curso", curso);
        resposta.put("cargaHorariaTotal", cargaTotal);
        resposta.put("creditosTotal", creditosTotal);
        resposta.put("modulos", modulosResposta);
        return resposta;
    }

    private Map<String, Object> dadosDisciplinaMatriz(Disciplina disciplina, Modulo moduloOriginalDaMatriz) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", disciplina.id);
        dados.put("nome", disciplina.nome);
        dados.put("codigo", disciplina.codigo);
        dados.put("curso", disciplina.curso);
        dados.put("moduloOriginal", disciplina.moduloOriginal != null ? disciplina.moduloOriginal : moduloOriginalDaMatriz);
        dados.put("moduloAtual", disciplina.modulo);
        dados.put("remanejada", disciplina.modulo != null && disciplina.modulo.id != null
                && !disciplina.modulo.id.equals(moduloOriginalDaMatriz.id));
        dados.put("professorResponsavel", disciplina.professorResponsavel);
        dados.put("cargaHoraria", disciplina.cargaHoraria);
        dados.put("creditos", disciplina.creditos);
        dados.put("ementa", disciplina.ementa);
        dados.put("ementaResumo", disciplina.ementaResumo);
        dados.put("ementaPdfNome", disciplina.ementaPdfNome);
        dados.put("bibliografia", disciplina.bibliografia);
        dados.put("ativo", disciplina.ativo);
        return dados;
    }
}
