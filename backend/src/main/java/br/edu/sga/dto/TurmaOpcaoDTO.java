package br.edu.sga.dto;

import br.edu.sga.entity.Turma;
import br.edu.sga.enums.StatusTurma;

public record TurmaOpcaoDTO(
        Long id,
        String nome,
        StatusTurma status,
        Long anoLetivoId,
        Integer ano,
        String anoPeriodo,
        Long cursoId,
        Long moduloId
) {
    public static TurmaOpcaoDTO de(Turma turma) {
        return new TurmaOpcaoDTO(
                turma.id,
                turma.nome,
                turma.status,
                turma.anoLetivo == null ? null : turma.anoLetivo.id,
                turma.anoLetivo == null ? null : turma.anoLetivo.ano,
                turma.anoPeriodo,
                turma.curso != null ? turma.curso.id
                        : turma.disciplina == null || turma.disciplina.curso == null ? null : turma.disciplina.curso.id,
                turma.disciplina == null || turma.disciplina.modulo == null ? null : turma.disciplina.modulo.id
        );
    }
}
