with curso_alvo as (
    select id
    from cursos
    where lower(trim(nome)) in ('curso de teologia', 'teologia ministerial')
      and ativo = true
    order by case when lower(trim(nome)) = 'curso de teologia' then 0 else 1 end, id
    limit 1
),
ano_alvo as (
    select id, data_inicio, data_fim
    from anos_letivos
    where ano = 2026
      and legado = false
    order by id
    limit 1
),
periodo_alvo as (
    select p.id, p.data_inicio, p.data_fim
    from periodos_letivos p
    join ano_alvo a on a.id = p.ano_letivo_id
    where p.status in ('PLANEJADO', 'ABERTO', 'EM_ANDAMENTO')
    order by p.ordem, p.id
    limit 1
)
insert into turmas (
    nome,
    curso_id,
    ano_letivo_id,
    periodo_letivo_id,
    ano_periodo,
    turno,
    quantidade_maxima_alunos,
    data_inicio,
    data_termino,
    status
)
select
    'Turma Teologia 2026',
    c.id,
    a.id,
    p.id,
    '2026',
    'Noite',
    30,
    coalesce(p.data_inicio, a.data_inicio),
    coalesce(p.data_fim, a.data_fim),
    'ABERTA'
from curso_alvo c
cross join ano_alvo a
left join periodo_alvo p on true
where not exists (
    select 1
    from turmas t
    where lower(trim(t.nome)) = lower('Turma Teologia 2026')
);
