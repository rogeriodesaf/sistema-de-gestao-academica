alter table disciplinas add column if not exists modulo_original_id bigint references modulos(id);

update disciplinas
set modulo_original_id = modulo_id
where modulo_original_id is null
  and modulo_id is not null;

alter table turmas add column if not exists disciplina_id bigint references disciplinas(id);
alter table turmas add column if not exists professor_id bigint references professores(id);
alter table turmas add column if not exists ano_letivo_id bigint references anos_letivos(id);
alter table turmas add column if not exists periodo_letivo_id bigint references periodos_letivos(id);
alter table turmas add column if not exists horario varchar(255);
alter table turmas add column if not exists sala varchar(120);

update turmas t
set disciplina_id = origem.disciplina_id,
    professor_id = origem.professor_id,
    ano_letivo_id = origem.ano_letivo_id,
    periodo_letivo_id = origem.periodo_letivo_id,
    horario = origem.horario,
    sala = origem.sala
from (
    select distinct on (turma_id)
        turma_id,
        disciplina_id,
        professor_id,
        ano_letivo_id,
        periodo_letivo_id,
        horario,
        sala
    from ofertas_disciplinas
    where turma_id is not null
    order by turma_id, id
) origem
where t.id = origem.turma_id
  and t.disciplina_id is null;
