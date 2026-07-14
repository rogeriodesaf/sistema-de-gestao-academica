alter table anos_letivos alter column turma_id drop not null;

create unique index if not exists ux_anos_letivos_independentes_ano
    on anos_letivos (ano) where turma_id is null;

alter table modulos add column if not exists ano_letivo_id bigint references anos_letivos(id);
alter table modulos add column if not exists data_inicio date;
alter table modulos add column if not exists data_fim date;
alter table modulos alter column curso_id drop not null;
alter table modulos drop constraint if exists modulos_curso_id_ordem_key;

create unique index if not exists ux_modulos_curriculares_curso_ordem
    on modulos (curso_id, ordem) where ano_letivo_id is null and curso_id is not null;
create unique index if not exists ux_modulos_ano_ordem
    on modulos (ano_letivo_id, ordem) where ano_letivo_id is not null;

alter table ofertas_disciplinas alter column periodo_letivo_id drop not null;
