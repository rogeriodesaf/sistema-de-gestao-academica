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
    public MatriculaDisciplina matricularEmDisciplina(MatriculaDisciplina matricula) {
        if (matricula.aluno == null || matricula.ofertaDisciplina == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Aluno e oferta de disciplina sao obrigatorios");
        }
        OfertaDisciplina oferta = OfertaDisciplina.findById(matricula.ofertaDisciplina.id);
        Aluno aluno = Aluno.findById(matricula.aluno.id);
        if (oferta == null || aluno == null) {
            throw new ApiException(Response.Status.BAD_REQUEST, "Aluno ou oferta de disciplina nao encontrados");
        }
        long jaMatriculado = MatriculaDisciplina.count("aluno = ?1 and ofertaDisciplina.disciplina = ?2 and status not in ?3",
                aluno, oferta.disciplina, List.of(StatusMatriculaDisciplina.CANCELADO, StatusMatriculaDisciplina.TRANCADO));
        if (jaMatriculado > 0) {
            throw new ApiException(Response.Status.CONFLICT, "Aluno ja matriculado nesta disciplina");
        }
        if (oferta.vagas != null) {
            long ocupadas = MatriculaDisciplina.count("ofertaDisciplina = ?1 and status in ?2",
                    oferta, List.of(StatusMatriculaDisciplina.ATIVA, StatusMatriculaDisciplina.MATRICULADO));
            if (ocupadas >= oferta.vagas) {
                throw new ApiException(Response.Status.CONFLICT, "Oferta de disciplina sem vagas disponiveis");
            }
        }
        matricula.aluno = aluno;
        matricula.ofertaDisciplina = oferta;
        matricula.curso = oferta.curso != null ? oferta.curso : (aluno.curso != null ? aluno.curso : oferta.turma == null ? null : oferta.turma.curso);
        matricula.periodoLetivo = oferta.periodoLetivo;
        matricula.status = matricula.status == null ? StatusMatriculaDisciplina.ATIVA : matricula.status;
        matricula.persist();
        criarOuAtualizarHistorico(matricula);
        return matricula;
    }

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
        return salvarNota(nota, null, null);
    }

    @Transactional
    public Nota salvarNota(Nota nota, Perfil perfil, Long usuarioId) {
        if (nota.ofertaDisciplina != null && nota.ofertaDisciplina.id != null) {
            OfertaDisciplina oferta = OfertaDisciplina.findById(nota.ofertaDisciplina.id);
            if (oferta != null) {
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
        atualizarHistoricoPorNota(nota);
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
        frequencia.aula = aula;
        validarProfessorVinculado(perfil, usuarioId, aula.ofertaDisciplina, aula.disciplina, aula.turma);
        if (frequencia.id == null) {
            frequencia.persist();
        }
        if (frequencia.aula.ofertaDisciplina != null) {
            atualizarFrequenciaHistorico(frequencia.aluno, frequencia.aula.ofertaDisciplina);
        } else {
            atualizarFrequenciaHistorico(frequencia.aluno, frequencia.aula.disciplina, frequencia.aula.turma);
        }
        return frequencia;
    }

    private void validarProfessorVinculado(Perfil perfil, Long usuarioId, OfertaDisciplina oferta, Disciplina disciplina, Turma turma) {
        if (perfil != Perfil.PROFESSOR) {
            return;
        }
        Professor professor = Professor.find("usuario.id", usuarioId).firstResult();
        if (professor == null) {
            throw new ApiException(Response.Status.FORBIDDEN, "Professor nao encontrado para o usuario logado");
        }
        if (oferta != null) {
            OfertaDisciplina ofertaCompleta = OfertaDisciplina.findById(oferta.id);
            if (ofertaCompleta != null && mesmoProfessor(professor, ofertaCompleta.professor)) {
                return;
            }
            if (ofertaCompleta != null && ofertaCompleta.disciplina != null && mesmoProfessor(professor, ofertaCompleta.disciplina.professorResponsavel)) {
                return;
            }
        }
        if (disciplina != null) {
            Disciplina disciplinaCompleta = Disciplina.findById(disciplina.id);
            if (disciplinaCompleta != null && mesmoProfessor(professor, disciplinaCompleta.professorResponsavel)) {
                return;
            }
        }
        if (disciplina != null && turma != null && VinculoProfessorDisciplinaTurma.count(
                "professor = ?1 and disciplina.id = ?2 and turma.id = ?3", professor, disciplina.id, turma.id) > 0) {
            return;
        }
        throw new ApiException(Response.Status.FORBIDDEN, "Professor nao vinculado a esta disciplina");
    }

    private boolean mesmoProfessor(Professor logado, Professor vinculado) {
        return logado != null && vinculado != null && logado.id != null && logado.id.equals(vinculado.id);
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

    private void criarOuAtualizarHistorico(MatriculaDisciplina matricula) {
        OfertaDisciplina oferta = matricula.ofertaDisciplina;
        HistoricoEscolar historico = HistoricoEscolar.find("matriculaDisciplina = ?1", matricula).firstResult();
        if (historico == null) {
            historico = HistoricoEscolar.find("aluno = ?1 and ofertaDisciplina = ?2", matricula.aluno, oferta).firstResult();
        }
        if (historico == null) {
            historico = new HistoricoEscolar();
            historico.aluno = matricula.aluno;
            historico.matriculaDisciplina = matricula;
            historico.ofertaDisciplina = oferta;
            historico.turma = oferta.turma;
            historico.curso = matricula.curso != null ? matricula.curso : oferta.curso != null ? oferta.curso : oferta.turma == null ? null : oferta.turma.curso;
            historico.disciplina = oferta.disciplina;
            historico.professorResponsavel = oferta.professor;
            historico.cargaHoraria = oferta.cargaHorariaPrevista != null ? oferta.cargaHorariaPrevista : oferta.disciplina.cargaHoraria;
            historico.periodoCursado = oferta.periodoLetivo == null ? null : oferta.periodoLetivo.nome;
            historico.situacao = StatusHistorico.CURSANDO;
            historico.notaFinal = matricula.notaFinal;
            historico.frequenciaFinal = matricula.frequenciaFinal;
            historico.persist();
        } else {
            historico.matriculaDisciplina = matricula;
            historico.ofertaDisciplina = oferta;
            historico.professorResponsavel = oferta.professor;
            historico.notaFinal = matricula.notaFinal;
            historico.frequenciaFinal = matricula.frequenciaFinal;
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

    private void atualizarFrequenciaHistorico(Aluno aluno, OfertaDisciplina oferta) {
        long total = Frequencia.count("aluno = ?1 and aula.ofertaDisciplina = ?2", aluno, oferta);
        long presentes = Frequencia.count("aluno = ?1 and aula.ofertaDisciplina = ?2 and presente = true", aluno, oferta);
        HistoricoEscolar historico = HistoricoEscolar.find("aluno = ?1 and ofertaDisciplina = ?2", aluno, oferta).firstResult();
        if (historico != null && total > 0) {
            historico.frequenciaFinal = BigDecimal.valueOf(presentes * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
