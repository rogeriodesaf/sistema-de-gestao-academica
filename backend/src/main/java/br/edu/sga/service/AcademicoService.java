package br.edu.sga.service;

import br.edu.sga.entity.*;
import br.edu.sga.enums.*;
import br.edu.sga.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AcademicoService {
    @Transactional
    public Matricula matricular(Matricula matricula) {
        if (matricula.aluno == null || matricula.turma == null || matricula.disciplina == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Aluno, turma e disciplina sao obrigatorios");
        }
        if (matricula.curso == null) {
            matricula.curso = matricula.turma.curso;
        }
        matricula.status = StatusMatricula.ATIVA;
        matricula.persist();
        criarOuAtualizarHistorico(matricula);
        return matricula;
    }

    @Transactional
    public Nota salvarNota(Nota nota) {
        nota.mediaFinal = calcularMedia(nota);
        nota.situacao = situacaoNota(nota.mediaFinal);
        if (nota.id == null) {
            nota.persist();
        }
        atualizarHistoricoPorNota(nota);
        return nota;
    }

    @Transactional
    public Frequencia salvarFrequencia(Frequencia frequencia) {
        if (frequencia.id == null) {
            frequencia.persist();
        }
        atualizarFrequenciaHistorico(frequencia.aluno, frequencia.aula.disciplina, frequencia.aula.turma);
        return frequencia;
    }

    public List<String> pendenciasEncerramento(Long turmaId, Long disciplinaId) {
        Turma turma = Turma.findById(turmaId);
        Disciplina disciplina = Disciplina.findById(disciplinaId);
        List<String> pendencias = new ArrayList<>();
        if (PlanoEnsino.count("turma = ?1 and disciplina = ?2", turma, disciplina) == 0) {
            pendencias.add("Plano de ensino nao cadastrado");
        }
        if (AulaMinistrada.count("turma = ?1 and disciplina = ?2", turma, disciplina) == 0) {
            pendencias.add("Nenhuma aula ministrada registrada");
        }
        if (Frequencia.count("aula.turma = ?1 and aula.disciplina = ?2", turma, disciplina) == 0) {
            pendencias.add("Frequencia nao lancada");
        }
        if (Nota.count("turma = ?1 and disciplina = ?2", turma, disciplina) == 0) {
            pendencias.add("Notas nao lancadas");
        }
        if (HistoricoEscolar.count("turma = ?1 and disciplina = ?2 and situacao in ?3", turma, disciplina, List.of(StatusHistorico.CURSANDO, StatusHistorico.PENDENTE)) > 0) {
            pendencias.add("Alunos sem resultado final no historico");
        }
        Long carga = AulaMinistrada.find("turma = ?1 and disciplina = ?2", turma, disciplina).stream()
                .map(AulaMinistrada.class::cast)
                .map(a -> a.cargaHorariaAula == null ? 0 : a.cargaHorariaAula)
                .reduce(0, Integer::sum).longValue();
        if (disciplina != null && disciplina.cargaHoraria != null && carga < disciplina.cargaHoraria) {
            pendencias.add("Carga horaria registrada menor que a prevista");
        }
        return pendencias;
    }

    @Transactional
    public void encerrarDisciplina(Long turmaId, Long disciplinaId) {
        List<String> pendencias = pendenciasEncerramento(turmaId, disciplinaId);
        if (!pendencias.isEmpty()) {
            throw new ApiException(Response.Status.CONFLICT, "Pendencias: " + String.join("; ", pendencias));
        }
        VinculoProfessorDisciplinaTurma.update("status = ?1 where turma.id = ?2 and disciplina.id = ?3", StatusVinculo.CONCLUIDO, turmaId, disciplinaId);
    }

    private void criarOuAtualizarHistorico(Matricula matricula) {
        HistoricoEscolar historico = HistoricoEscolar.find("aluno = ?1 and turma = ?2 and disciplina = ?3", matricula.aluno, matricula.turma, matricula.disciplina).firstResult();
        if (historico == null) {
            historico = new HistoricoEscolar();
            historico.aluno = matricula.aluno;
            historico.turma = matricula.turma;
            historico.curso = matricula.curso;
            historico.disciplina = matricula.disciplina;
            historico.cargaHoraria = matricula.disciplina.cargaHoraria;
            historico.periodoCursado = matricula.turma.anoPeriodo;
            historico.persist();
        }
    }

    private BigDecimal calcularMedia(Nota nota) {
        List<BigDecimal> valores = new ArrayList<>();
        if (nota.nota1 != null) valores.add(nota.nota1);
        if (nota.nota2 != null) valores.add(nota.nota2);
        if (nota.trabalho != null) valores.add(nota.trabalho);
        if (nota.avaliacaoFinal != null) valores.add(nota.avaliacaoFinal);
        if (valores.isEmpty()) return null;
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_UP);
    }

    private StatusNota situacaoNota(BigDecimal media) {
        if (media == null) return StatusNota.PENDENTE;
        if (media.compareTo(BigDecimal.valueOf(7)) >= 0) return StatusNota.APROVADO;
        if (media.compareTo(BigDecimal.valueOf(5)) >= 0) return StatusNota.RECUPERACAO;
        return StatusNota.REPROVADO;
    }

    private void atualizarHistoricoPorNota(Nota nota) {
        HistoricoEscolar historico = HistoricoEscolar.find("aluno = ?1 and turma = ?2 and disciplina = ?3", nota.aluno, nota.turma, nota.disciplina).firstResult();
        if (historico != null) {
            historico.notaFinal = nota.mediaFinal;
            historico.situacao = nota.situacao == StatusNota.APROVADO ? StatusHistorico.APROVADO
                    : nota.situacao == StatusNota.REPROVADO ? StatusHistorico.REPROVADO : StatusHistorico.PENDENTE;
        }
    }

    private void atualizarFrequenciaHistorico(Aluno aluno, Disciplina disciplina, Turma turma) {
        long total = Frequencia.count("aluno = ?1 and aula.disciplina = ?2 and aula.turma = ?3", aluno, disciplina, turma);
        long presentes = Frequencia.count("aluno = ?1 and aula.disciplina = ?2 and aula.turma = ?3 and presente = true", aluno, disciplina, turma);
        HistoricoEscolar historico = HistoricoEscolar.find("aluno = ?1 and disciplina = ?2 and turma = ?3", aluno, disciplina, turma).firstResult();
        if (historico != null && total > 0) {
            historico.frequenciaFinal = BigDecimal.valueOf(presentes * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
