package br.edu.sga.dto;

import br.edu.sga.enums.ResultadoAcademico;
import br.edu.sga.enums.StatusMatriculaDisciplina;
import java.time.LocalDate;
import java.util.List;

public record AlunosTurmaDTO(
        int quantidadeAlunos,
        int quantidadeMatriculas,
        List<AlunoDTO> alunos
) {
    public record AlunoDTO(
            Long alunoId,
            String nomeAluno,
            String curso,
            int quantidadeDisciplinas,
            List<DisciplinaDTO> disciplinas
    ) {}

    public record DisciplinaDTO(
            Long matriculaId,
            Long ofertaId,
            Long disciplinaId,
            String nomeDisciplina,
            String horario,
            StatusMatriculaDisciplina situacaoMatricula,
            ResultadoAcademico resultadoAcademico,
            LocalDate dataMatricula,
            int quantidadeVinculosDuplicados
    ) {}
}
