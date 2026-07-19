package br.edu.sga.service;

import br.edu.sga.entity.Aluno;
import br.edu.sga.entity.MatriculaDisciplina;
import br.edu.sga.entity.OfertaDisciplina;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import br.edu.sga.enums.StatusOfertaDisciplina;
import br.edu.sga.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class IntegridadeAcademicaService {
    private static final List<StatusOfertaDisciplina> OFERTAS_ATIVAS = List.of(
            StatusOfertaDisciplina.PLANEJADA, StatusOfertaDisciplina.ABERTA,
            StatusOfertaDisciplina.EM_ANDAMENTO, StatusOfertaDisciplina.AGUARDANDO_HOMOLOGACAO);
    private static final List<StatusMatriculaDisciplina> MATRICULAS_ATIVAS = List.of(
            StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO);
    private static final Pattern HORAS = Pattern.compile("(?<!\\d)(2[0-3]|[01]?\\d)(?:[:h]([0-5]\\d))?");
    private static final List<List<String>> DIAS = List.of(
            List.of("segunda-feira", "segunda", "seg"),
            List.of("terca-feira", "terca", "ter"),
            List.of("quarta-feira", "quarta", "qua"),
            List.of("quinta-feira", "quinta", "qui"),
            List.of("sexta-feira", "sexta", "sex"),
            List.of("sabado", "sab"),
            List.of("domingo", "dom"));

    public void validarDatasOferta(OfertaDisciplina oferta) {
        if (oferta.dataInicio != null && oferta.dataFim != null
                && oferta.dataFim.isBefore(oferta.dataInicio)) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "A data de término não pode ser anterior à data de início");
        }
        if (oferta.periodoLetivo != null && oferta.periodoLetivo.anoLetivo != null
                && !oferta.anoLetivo.id.equals(oferta.periodoLetivo.anoLetivo.id)) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "O período letivo não pertence ao ano letivo da oferta");
        }
        validarLimites(oferta.dataInicio, oferta.dataFim,
                oferta.anoLetivo.dataInicio, oferta.anoLetivo.dataFim,
                "A oferta deve ocorrer dentro do ano letivo selecionado");
        if (oferta.periodoLetivo != null) {
            validarLimites(oferta.dataInicio, oferta.dataFim,
                    oferta.periodoLetivo.dataInicio, oferta.periodoLetivo.dataFim,
                    "A oferta deve ocorrer dentro do período letivo selecionado");
        }
    }

    public void validarConflitosOferta(OfertaDisciplina oferta, Long idAtual) {
        if (!OFERTAS_ATIVAS.contains(oferta.status)) return;
        List<OfertaDisciplina> professor = OfertaDisciplina.list(
                "professor = ?1 and status in ?2 order by id", oferta.professor, OFERTAS_ATIVAS);
        if (professor.stream().anyMatch(existente -> diferente(existente, idAtual)
                && sobrepoe(oferta, existente))) {
            throw new ApiException(Response.Status.CONFLICT,
                    "O professor já possui outra oferta neste horário");
        }
        if (oferta.sala == null || oferta.sala.isBlank()) return;
        List<OfertaDisciplina> sala = OfertaDisciplina.list(
                "lower(sala) = ?1 and status in ?2 order by id",
                oferta.sala.trim().toLowerCase(Locale.ROOT), OFERTAS_ATIVAS);
        if (sala.stream().anyMatch(existente -> diferente(existente, idAtual)
                && sobrepoe(oferta, existente))) {
            throw new ApiException(Response.Status.CONFLICT, "A sala já está ocupada neste horário");
        }
    }

    public void validarConflitoAluno(Aluno aluno, OfertaDisciplina novaOferta) {
        List<MatriculaDisciplina> matriculas = MatriculaDisciplina.list(
                "aluno = ?1 and status in ?2 and ofertaDisciplina.status in ?3 order by id",
                aluno, MATRICULAS_ATIVAS, OFERTAS_ATIVAS);
        if (matriculas.stream().map(item -> item.ofertaDisciplina)
                .anyMatch(existente -> !existente.id.equals(novaOferta.id) && sobrepoe(novaOferta, existente))) {
            throw new ApiException(Response.Status.CONFLICT,
                    "O aluno já possui outra disciplina neste horário");
        }
    }

    public void validarDataNaOferta(LocalDate data, OfertaDisciplina oferta, String registro) {
        if (data == null) return;
        LocalDate inicio = inicio(oferta);
        LocalDate fim = fim(oferta);
        if (inicio != null && data.isBefore(inicio) || fim != null && data.isAfter(fim)) {
            throw new ApiException(Response.Status.BAD_REQUEST,
                    "A data " + registro + " deve estar dentro do intervalo da oferta");
        }
    }

    boolean sobrepoe(OfertaDisciplina primeira, OfertaDisciplina segunda) {
        return datasSobrepoem(primeira, segunda) && horariosSobrepoem(primeira.horario, segunda.horario);
    }

    private boolean datasSobrepoem(OfertaDisciplina primeira, OfertaDisciplina segunda) {
        LocalDate inicioPrimeira = inicio(primeira);
        LocalDate fimPrimeira = fim(primeira);
        LocalDate inicioSegunda = inicio(segunda);
        LocalDate fimSegunda = fim(segunda);
        return !(fimPrimeira != null && inicioSegunda != null && fimPrimeira.isBefore(inicioSegunda)
                || fimSegunda != null && inicioPrimeira != null && fimSegunda.isBefore(inicioPrimeira));
    }

    private boolean horariosSobrepoem(String primeiro, String segundo) {
        Horario a = horario(primeiro);
        Horario b = horario(segundo);
        if (!a.dias().isEmpty() && !b.dias().isEmpty()) {
            Set<Integer> comuns = new HashSet<>(a.dias());
            comuns.retainAll(b.dias());
            if (comuns.isEmpty()) return false;
        }
        if (a.inicio() != null && b.inicio() != null) {
            return a.inicio() <= b.fim() && b.inicio() <= a.fim();
        }
        return a.normalizado().equals(b.normalizado());
    }

    private Horario horario(String valor) {
        String normalizado = normalizar(valor);
        Set<Integer> dias = new HashSet<>();
        for (int i = 0; i < DIAS.size(); i++) {
            for (String nome : DIAS.get(i)) {
                if (Pattern.compile("(^|\\W)" + Pattern.quote(nome) + "($|\\W)")
                        .matcher(normalizado).find()) {
                    dias.add(i);
                    break;
                }
            }
        }
        List<Integer> horarios = new ArrayList<>();
        Matcher matcher = HORAS.matcher(normalizado);
        while (matcher.find() && horarios.size() < 2) {
            int hora = Integer.parseInt(matcher.group(1));
            int minuto = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            horarios.add(hora * 60 + minuto);
        }
        Integer inicio = horarios.isEmpty() ? null : horarios.getFirst();
        Integer fim = horarios.size() < 2 ? inicio : horarios.get(1);
        return new Horario(normalizado, dias, inicio, fim);
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
    }

    private LocalDate inicio(OfertaDisciplina oferta) {
        if (oferta.dataInicio != null) return oferta.dataInicio;
        if (oferta.periodoLetivo != null && oferta.periodoLetivo.dataInicio != null) {
            return oferta.periodoLetivo.dataInicio;
        }
        return oferta.anoLetivo == null ? null : oferta.anoLetivo.dataInicio;
    }

    private LocalDate fim(OfertaDisciplina oferta) {
        if (oferta.dataFim != null) return oferta.dataFim;
        if (oferta.periodoLetivo != null && oferta.periodoLetivo.dataFim != null) {
            return oferta.periodoLetivo.dataFim;
        }
        return oferta.anoLetivo == null ? null : oferta.anoLetivo.dataFim;
    }

    private void validarLimites(LocalDate inicio, LocalDate fim, LocalDate limiteInicio,
                                LocalDate limiteFim, String mensagem) {
        if (inicio != null && limiteInicio != null && inicio.isBefore(limiteInicio)
                || fim != null && limiteFim != null && fim.isAfter(limiteFim)) {
            throw new ApiException(Response.Status.BAD_REQUEST, mensagem);
        }
    }

    private boolean diferente(OfertaDisciplina oferta, Long idAtual) {
        return idAtual == null || !idAtual.equals(oferta.id);
    }

    private record Horario(String normalizado, Set<Integer> dias, Integer inicio, Integer fim) {}
}
