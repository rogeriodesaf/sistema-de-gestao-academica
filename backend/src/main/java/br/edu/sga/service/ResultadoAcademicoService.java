package br.edu.sga.service;

import br.edu.sga.entity.Avaliacao;
import br.edu.sga.entity.HistoricoEscolar;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.NotaAvaliacao;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ResultadoAcademicoService {
    @ConfigProperty(name = "sga.academico.media-minima")
    BigDecimal mediaMinima;

    public record NotaResultado(Long avaliacaoId, String nome, String descricao, Integer ordem,
                                java.time.LocalDate data, BigDecimal notaMaxima, BigDecimal peso,
                                BigDecimal nota, String observacao, String status) {}

    public record Resultado(BigDecimal media, BigDecimal mediaMinima, String situacao,
                            boolean completo, boolean definitivo, String statusOferta,
                            String mensagem, List<NotaResultado> avaliacoes) {}

    public Resultado calcular(MatriculaDisciplina matricula,
                              FrequenciaAcademicaService.ResumoFrequencia frequencia) {
        return calcular(matricula, frequencia, false);
    }

    public Resultado calcularPreliminar(MatriculaDisciplina matricula,
                                        FrequenciaAcademicaService.ResumoFrequencia frequencia) {
        return calcular(matricula, frequencia, true);
    }

    private Resultado calcular(MatriculaDisciplina matricula,
                               FrequenciaAcademicaService.ResumoFrequencia frequencia,
                               boolean exibirPreliminar) {
        List<Avaliacao> avaliacoes = Avaliacao.list(
                "ofertaDisciplina = ?1 order by ordem, id", matricula.ofertaDisciplina);
        List<NotaResultado> itens = avaliacoes.stream().map(avaliacao -> {
            NotaAvaliacao nota = NotaAvaliacao.find(
                    "avaliacao = ?1 and matriculaDisciplina = ?2", avaliacao, matricula).firstResult();
            return new NotaResultado(avaliacao.id, avaliacao.nome, avaliacao.descricao, avaliacao.ordem,
                    avaliacao.data, avaliacao.notaMaxima, avaliacao.peso, nota == null ? null : nota.nota,
                    nota == null ? null : nota.observacao, nota == null ? "NAO_LANCADA" : "LANCADA");
        }).toList();
        BigDecimal media = calcularMedia(matricula, avaliacoes);
        boolean completo = !avaliacoes.isEmpty() && itens.stream().allMatch(item -> item.nota() != null);
        String preliminar = situacaoFinal(media, completo, frequencia);
        boolean definitivo = matricula.ofertaDisciplina.status == StatusOfertaDisciplina.CONCLUIDA;
        String situacao = definitivo || exibirPreliminar ? preliminar : "EM_ANDAMENTO";
        String statusOferta = matricula.ofertaDisciplina.status == null
                ? StatusOfertaDisciplina.EM_ANDAMENTO.name() : matricula.ofertaDisciplina.status.name();
        String mensagem = definitivo ? "Resultado homologado."
                : statusOferta.equals(StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO.name())
                ? "O diario foi encerrado e aguarda homologacao."
                : "Resultado parcial; ainda nao e definitivo.";
        return new Resultado(media, mediaMinima, situacao, completo, definitivo, statusOferta, mensagem, itens);
    }

    public String situacaoFinal(BigDecimal media, boolean completo,
                                FrequenciaAcademicaService.ResumoFrequencia frequencia) {
        if (!completo || media == null) return "EM_ANDAMENTO";
        if (frequencia != null && "REPROVADO_POR_FALTA".equals(frequencia.situacao())) {
            return "REPROVADO_POR_FREQUENCIA";
        }
        return media.compareTo(mediaMinima) >= 0 ? "APROVADO" : "REPROVADO_POR_NOTA";
    }

    public BigDecimal calcularMedia(MatriculaDisciplina matricula, List<Avaliacao> avaliacoes) {
        BigDecimal soma = BigDecimal.ZERO;
        BigDecimal pesos = BigDecimal.ZERO;
        for (Avaliacao avaliacao : avaliacoes) {
            NotaAvaliacao nota = NotaAvaliacao.find(
                    "avaliacao = ?1 and matriculaDisciplina = ?2", avaliacao, matricula).firstResult();
            if (nota == null) continue;
            soma = soma.add(nota.nota.divide(avaliacao.notaMaxima, 8, RoundingMode.HALF_UP)
                    .multiply(avaliacao.peso));
            pesos = pesos.add(avaliacao.peso);
        }
        return pesos.signum() == 0 ? null : soma.divide(pesos, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.TEN).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void atualizarMedias(OfertaDisciplina oferta) {
        List<Avaliacao> avaliacoes = Avaliacao.list("ofertaDisciplina = ?1 order by ordem, id", oferta);
        List<MatriculaDisciplina> matriculas = MatriculaDisciplina.list(
                "ofertaDisciplina = ?1 and status in ?2 order by aluno.nome", oferta,
                List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO));
        matriculas.forEach(matricula -> {
            matricula.notaFinal = calcularMedia(matricula, avaliacoes);
            sincronizarHistorico(matricula);
        });
    }

    private void sincronizarHistorico(MatriculaDisciplina matricula) {
        HistoricoEscolar historico = HistoricoEscolar.find(
                "aluno = ?1 and ofertaDisciplina = ?2", matricula.aluno, matricula.ofertaDisciplina).firstResult();
        if (historico == null) return;
        historico.notaFinal = matricula.notaFinal;
    }
}
