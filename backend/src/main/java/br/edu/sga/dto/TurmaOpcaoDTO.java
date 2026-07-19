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
        Long periodoLetivoId
) {
    public static TurmaOpcaoDTO de(Turma turma) {
        return new TurmaOpcaoDTO(
                turma.id,
                turma.nome,
                turma.status,
                turma.anoLetivo == null ? null : turma.anoLetivo.id,
                turma.anoLetivo == null ? null : turma.anoLetivo.ano,
                turma.anoPeriodo,
                turma.curso == null ? null : turma.curso.id,
                turma.periodoLetivo == null ? null : turma.periodoLetivo.id
        );
    }
}
