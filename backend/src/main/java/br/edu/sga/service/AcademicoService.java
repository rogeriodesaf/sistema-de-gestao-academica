package br.edu.sga.service;

import br.edu.sga.entity.*;
import br.edu.sga.enums.*;
import br.edu.sga.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AcademicoService {
    @Inject FrequenciaAcademicaService frequenciaAcademicaService;
    @Inject ProfessorUsuarioService professorUsuarioService;
    @Inject IntegridadeAcademicaService integridadeAcademicaService;
    @Inject EntityManager entityManager;
    @Transactional
    public MatriculaDisciplina matricularEmDisciplina(MatriculaDisciplina matricula) {
        if (matricula.aluno == null || matricula.ofertaDisciplina == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Aluno e oferta de disciplina sao obrigatorios");
        }
        OfertaDisciplina oferta = entityManager.find(OfertaDisciplina.class,
                matricula.ofertaDisciplina.id, LockModeType.PESSIMISTIC_WRITE);
        Aluno aluno = Aluno.findById(matricula.aluno.id);
        if (oferta == null || aluno == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Aluno ou oferta de disciplina nao encontrados");
        }
        MatriculaDisciplina existente = MatriculaDisciplina.find(
                "aluno = ?1 and ofertaDisciplina = ?2 order by id", aluno, oferta).firstResult();
        if (existente != null) {
            String orientacao = existente.status == StatusMatriculaDisciplina.CANCELADO
                    || existente.status == StatusMatriculaDisciplina.TRANCADO
                    ? " Reative o registro existente explicitamente para preservar o histórico."
                    : "";
            throw new ApiException(Response.Status.CONFLICT,
                    "O aluno já está matriculado nesta oferta de disciplina." + orientacao);
        }
        if (!List.of(StatusOfertaDisciplina.PLANEJADA, StatusOfertaDisciplina.ABERTA,
                StatusOfertaDisciplina.EM_ANDAMENTO).contains(oferta.status)) {
            throw new ApiException(Response.Status.CONFLICT,
                    "Não é possível matricular alunos nesta situação da oferta");
        }
        Integer limiteVagas = oferta.vagas;
        if (limiteVagas != null) {
            long ocupadas = MatriculaDisciplina.count("ofertaDisciplina = ?1 and status in ?2",
                    oferta, List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO));
            if (ocupadas >= limiteVagas) {
                throw new ApiException(Response.Status.CONFLICT,
                        "Não há vagas disponíveis nesta oferta de disciplina");
            }
        }
        integridadeAcademicaService.validarConflitoAluno(aluno, oferta);
        matricula.aluno = aluno;
        matricula.ofertaDisciplina = oferta;
        matricula.curso = aluno.curso != null ? aluno.curso : oferta.curso;
        matricula.periodoLetivo = oferta.periodoLetivo;
        matricula.status = StatusMatriculaDisciplina.ATIVA;
        matricula.resultadoAcademico = ResultadoAcademico.EM_ANDAMENTO;
        matricula.notaFinal = null;
        matricula.frequenciaFinal = null;
        matricula.dataConsolidacao = null;
        matricula.persist();
        return matricula;
    }

    @Transactional
    public Matricula matricular(Matricula matricula) {
        if (matricula.aluno == null || matricula.turma == null || matricula.disciplina == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Aluno, turma e disciplina sao obrigatorios");
        }
        if (matricula.curso == null) {
            matricula.curso = matricula.aluno != null && matricula.aluno.curso != null ? matricula.aluno.curso : matricula.turma.curso;
        }
        matricula.status = StatusMatricula.ATIVA;
        matricula.persist();
        criarOuAtualizarHistorico(matricula);
        return matricula;
    }

    @Transactional
    public Nota salvarNota(Nota nota) {
        return salvarNota(nota, null, null);
    }

    @Transactional
    public Nota salvarNota(Nota nota, Perfil perfil, Long usuarioId) {
        if (nota.ofertaDisciplina != null && nota.ofertaDisciplina.id != null) {
            OfertaDisciplina oferta = OfertaDisciplina.findById(nota.ofertaDisciplina.id);
            if (oferta != null) {
                exigirOfertaEmAndamento(oferta);
                nota.ofertaDisciplina = oferta;
                nota.turma = oferta.turma;
                nota.disciplina = oferta.disciplina;
            }
        }
        validarProfessorVinculado(perfil, usuarioId, nota.ofertaDisciplina, nota.disciplina, nota.turma);
        nota.mediaFinal = calcularMedia(nota);
        nota.situacao = situacaoNota(nota.mediaFinal);
        if (nota.id == null) {
            nota.persist();
        }
        if (nota.ofertaDisciplina == null) atualizarHistoricoPorNota(nota);
        return nota;
    }

    @Transactional
    public Frequencia salvarFrequencia(Frequencia frequencia) {
        return salvarFrequencia(frequencia, null, null);
    }

    @Transactional
    public Frequencia salvarFrequencia(Frequencia frequencia, Perfil perfil, Long usuarioId) {
        if (frequencia.aula == null || frequencia.aula.id == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Aula e obrigatoria para lancar frequencia");
        }
        AulaMinistrada aula = AulaMinistrada.findById(frequencia.aula.id);
        if (aula == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Aula nao encontrada");
        }
        exigirOfertaEmAndamento(aula.ofertaDisciplina);
        frequencia.aula = aula;
        validarProfessorVinculado(perfil, usuarioId, aula.ofertaDisciplina, aula.disciplina, aula.turma);
        if (aula.ofertaDisciplina != null) {
            MatriculaDisciplina matricula = frequencia.matriculaDisciplina == null
                    || frequencia.matriculaDisciplina.id == null ? null
                    : MatriculaDisciplina.findById(frequencia.matriculaDisciplina.id);
            if (matricula == null && frequencia.aluno != null && frequencia.aluno.id != null) {
                matricula = MatriculaDisciplina.find(
                        "aluno.id = ?1 and ofertaDisciplina = ?2 and status in ?3",
                        frequencia.aluno.id, aula.ofertaDisciplina,
                        List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO)).firstResult();
            }
            if (matricula == null || !aula.ofertaDisciplina.id.equals(matricula.ofertaDisciplina.id)
                    || !List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO)
                    .contains(matricula.status)) {
                throw new ApiException(Response.Status.BAD_REQUEST,
                        "A frequência deve pertencer a um aluno matriculado nesta oferta");
            }
            if (frequencia.id == null && Frequencia.count(
                    "aula = ?1 and aluno = ?2", aula, matricula.aluno) > 0) {
                throw new ApiException(Response.Status.CONFLICT,
                        "A frequência deste aluno já foi registrada para esta aula");
            }
            frequencia.matriculaDisciplina = matricula;
            frequencia.aluno = matricula.aluno;
            frequencia.presente = frequencia.status == StatusFrequencia.PRESENTE;
        }
        if (frequencia.id == null) {
            frequencia.persist();
        }
        if (frequencia.aula.ofertaDisciplina != null) {
            frequenciaAcademicaService.recalcularOferta(frequencia.aula.ofertaDisciplina);
        } else {
            atualizarFrequenciaHistorico(frequencia.aluno, frequencia.aula.disciplina, frequencia.aula.turma);
        }
        return frequencia;
    }

    private void validarProfessorVinculado(Perfil perfil, Long usuarioId, OfertaDisciplina oferta, Disciplina disciplina, Turma turma) {
        if (perfil != Perfil.PROFESSOR) {
            return;
        }
        Professor professor = professorUsuarioService.identificarProfessor(usuarioId);
        if (oferta == null || oferta.id == null) {
            throw new ApiException(Response.Status.FORBIDDEN,
                    "Acesso docente exige uma oferta de disciplina vinculada ao professor");
        }
        OfertaDisciplina ofertaCompleta = OfertaDisciplina.findById(oferta.id);
        if (ofertaCompleta == null || !mesmoProfessor(professor, ofertaCompleta.professor)) {
            throw new ApiException(Response.Status.FORBIDDEN, "Professor nao vinculado a esta oferta de disciplina");
        }
    }

    private boolean mesmoProfessor(Professor logado, Professor vinculado) {
        return logado != null && vinculado != null && logado.id != null && logado.id.equals(vinculado.id);
    }

    private void exigirOfertaEmAndamento(OfertaDisciplina oferta) {
        if (oferta != null && oferta.status != StatusOfertaDisciplina.EM_ANDAMENTO) {
            throw new ApiException(Response.Status.CONFLICT,
                    "O diario esta bloqueado para edicao enquanto nao estiver EM_ANDAMENTO");
        }
    }

    public List<String> pendenciasEncerramento(Long turmaId, Long disciplinaId) {
        Turma turma = Turma.findById(turmaId);
        Disciplina disciplina = Disciplina.findById(disciplinaId);
        List<String> pendencias = new ArrayList<>();
        if (PlanoEnsino.count("disciplina", disciplina) == 0) {
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
        HistoricoEscolar historico;
        if (nota.ofertaDisciplina != null) {
            historico = HistoricoEscolar.find("aluno = ?1 and ofertaDisciplina = ?2", nota.aluno, nota.ofertaDisciplina).firstResult();
        } else {
            historico = HistoricoEscolar.find("aluno = ?1 and turma = ?2 and disciplina = ?3", nota.aluno, nota.turma, nota.disciplina).firstResult();
        }
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
