package br.edu.sga.service;

import br.edu.sga.entity.Frequencia;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.enums.StatusFrequencia;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FrequenciaAcademicaService {
    @Inject EntityManager entityManager;
    @ConfigProperty(name = "sga.academico.frequencia-minima")
    BigDecimal frequenciaMinima;

    public record ResumoFrequencia(Long matriculaId, long aulasMinistradas, long presencas, long faltas,
                                   long faltasJustificadas, BigDecimal percentualPresenca,
                                   BigDecimal percentualFaltas, String situacao) {}
    private record FrequenciaCalculo(Long alunoId, StatusFrequencia status, boolean presente,
                                     String justificativa, int cargaHoraria) {}

    @Transactional
    public List<ResumoFrequencia> recalcularOferta(OfertaDisciplina oferta) {
        List<MatriculaDisciplina> matriculas = matriculasAtivas(oferta);
        Map<Long, ResumoFrequencia> resumos = calcular(oferta, matriculas).stream()
                .collect(Collectors.toMap(ResumoFrequencia::matriculaId, Function.identity()));
        for (MatriculaDisciplina matricula : matriculas) {
            ResumoFrequencia resumo = resumos.get(matricula.id);
            matricula.frequenciaFinal = resumo.percentualPresenca();
        }
        return matriculas.stream().map(matricula -> resumos.get(matricula.id)).toList();
    }

    public List<ResumoFrequencia> resumirOferta(OfertaDisciplina oferta) {
        return calcular(oferta, matriculasDaOferta(oferta));
    }

    private List<ResumoFrequencia> calcular(OfertaDisciplina oferta, List<MatriculaDisciplina> matriculas) {
        long aulas = br.edu.sga.entity.AulaMinistrada.count("ofertaDisciplina", oferta);
        List<FrequenciaCalculo> frequencias = entityManager.createQuery("""
                select f.aluno.id, f.status, f.presente, f.justificativa, coalesce(f.aula.cargaHorariaAula, 1)
                from Frequencia f where f.aula.ofertaDisciplina = :oferta
                """, Object[].class).setParameter("oferta", oferta).getResultList().stream()
                .map(item -> new FrequenciaCalculo((Long) item[0], (StatusFrequencia) item[1],
                        (boolean) item[2], (String) item[3], ((Number) item[4]).intValue())).toList();
        Map<Long, List<FrequenciaCalculo>> porAluno = frequencias.stream()
                .collect(Collectors.groupingBy(FrequenciaCalculo::alunoId));

        return matriculas.stream().map(matricula -> {
            List<FrequenciaCalculo> registros = porAluno.getOrDefault(matricula.aluno.id, List.of());
            long presencas = registros.stream().filter(this::presente).count();
            long justificadas = registros.stream().filter(this::justificada).count();
            long faltas = registros.size() - presencas;
            long chamadas = presencas + faltas;
            long horasRegistradas = registros.stream().mapToLong(FrequenciaCalculo::cargaHoraria).sum();
            long horasPresentes = registros.stream().filter(this::presente)
                    .mapToLong(FrequenciaCalculo::cargaHoraria).sum();
            long horasAusentes = horasRegistradas - horasPresentes;
            BigDecimal percentualPresenca = percentual(horasPresentes, horasRegistradas);
            BigDecimal percentualFaltas = percentual(horasAusentes, horasRegistradas);
            return new ResumoFrequencia(matricula.id, aulas, presencas, faltas, justificadas,
                    percentualPresenca, percentualFaltas, situacao(percentualPresenca, chamadas));
        }).toList();
    }

    private List<MatriculaDisciplina> matriculasAtivas(OfertaDisciplina oferta) {
        return MatriculaDisciplina.list("ofertaDisciplina = ?1 and status in ?2 order by aluno.nome", oferta,
                List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO));
    }

    private List<MatriculaDisciplina> matriculasDaOferta(OfertaDisciplina oferta) {
        return MatriculaDisciplina.list("ofertaDisciplina = ?1 and status not in ?2 order by aluno.nome", oferta,
                List.of(StatusMatriculaDisciplina.CANCELADO, StatusMatriculaDisciplina.TRANCADO));
    }

    private boolean presente(FrequenciaCalculo frequencia) {
        return frequencia.status() == StatusFrequencia.PRESENTE
                || frequencia.status() == null && frequencia.presente();
    }

    private boolean justificada(FrequenciaCalculo frequencia) {
        return frequencia.status() == StatusFrequencia.JUSTIFICADO
                || frequencia.status() == null && !frequencia.presente()
                && frequencia.justificativa() != null && !frequencia.justificativa().isBlank();
    }

    private BigDecimal percentual(long quantidade, long total) {
        return total == 0 ? null : BigDecimal.valueOf(quantidade * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private String situacao(BigDecimal percentualPresenca, long chamadas) {
        if (chamadas == 0) return "SEM_CHAMADAS";
        int comparacao = percentualPresenca.compareTo(frequenciaMinima);
        if (comparacao < 0) return "REPROVADO_POR_FALTA";
        if (comparacao == 0) return "LIMITE";
        return "OK";
    }
}
