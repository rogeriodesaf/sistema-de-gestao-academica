update matriculas_disciplinas
set status = 'ATIVA'
where status in (
    'MATRICULADO',
    'CONCLUIDA',
    'CONCLUIDO',
    'REPROVADA',
    'REPROVADO_POR_NOTA',
    'REPROVADO_POR_FREQUENCIA'
);
