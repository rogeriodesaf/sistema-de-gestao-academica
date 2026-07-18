create sequence if not exists migracao_turma_oferta_pendencia_SEQ start with 1 increment by 50;

create table if not exists migracao_turma_oferta_pendencia (
    id bigint primary key default nextval('migracao_turma_oferta_pendencia_SEQ'),
    turma_id bigint references turmas(id),
    oferta_id bigint references ofertas_disciplinas(id),
    tipo_pendencia varchar(100) not null,
    campos_ausentes varchar(2000),
    divergencias varchar(4000),
    acao_automatica varchar(2000),
    motivo_intervencao_manual varchar(4000),
    resolvida boolean not null default false,
    data_criacao timestamp not null default current_timestamp,
    data_resolucao timestamp
);

create index if not exists ix_migracao_turma_oferta_turma
    on migracao_turma_oferta_pendencia (turma_id);
create index if not exists ix_migracao_turma_oferta_resolvida
    on migracao_turma_oferta_pendencia (resolvida, tipo_pendencia);

-- As colunas permanecem fisicamente para compatibilidade historica, mas deixam de
-- restringir o cadastro administrativo de novas turmas.
alter table turmas alter column disciplina_id drop not null;
alter table turmas alter column professor_id drop not null;
alter table turmas alter column horario drop not null;
alter table turmas alter column sala drop not null;

-- Completa apenas referencias administrativas quando todas as ofertas da turma
-- apontam inequivocamente para o mesmo ano, periodo ou curso.
with referencias as (
    select turma_id,
           min(ano_letivo_id) as ano_letivo_id,
           min(periodo_letivo_id) as periodo_letivo_id,
           min(curso_id) as curso_id,
           count(distinct ano_letivo_id) as anos,
           count(distinct periodo_letivo_id) as periodos,
           count(distinct curso_id) as cursos
    from ofertas_disciplinas
    where turma_id is not null
    group by turma_id
)
update turmas t
set ano_letivo_id = case when t.ano_letivo_id is null and r.anos = 1 then r.ano_letivo_id else t.ano_letivo_id end,
    periodo_letivo_id = case when t.periodo_letivo_id is null and r.periodos = 1 then r.periodo_letivo_id else t.periodo_letivo_id end,
    curso_id = case when t.curso_id is null and r.cursos = 1 then r.curso_id else t.curso_id end
from referencias r
where r.turma_id = t.id;

-- Uma oferta correspondente tem a mesma turma, disciplina, ano e periodo.
-- Somente lacunas inequivocas da unica oferta correspondente sao preenchidas.
with correspondencias as (
    select t.id as turma_id, min(o.id) as oferta_id, count(o.id) as quantidade
    from turmas t
    left join ofertas_disciplinas o
      on o.turma_id = t.id
     and o.disciplina_id = t.disciplina_id
     and o.ano_letivo_id = t.ano_letivo_id
     and o.periodo_letivo_id is not distinct from t.periodo_letivo_id
    group by t.id
), unicas as (
    select c.turma_id, c.oferta_id
    from correspondencias c
    where c.quantidade = 1
)
update ofertas_disciplinas o
set curso_id = coalesce(o.curso_id, t.curso_id),
    modulo_id = coalesce(o.modulo_id, d.modulo_id),
    professor_id = coalesce(o.professor_id, t.professor_id),
    horario = coalesce(o.horario, nullif(trim(t.horario), '')),
    sala = coalesce(o.sala, nullif(trim(t.sala), ''))
from unicas u
join turmas t on t.id = u.turma_id
left join disciplinas d on d.id = t.disciplina_id
where o.id = u.oferta_id;

with correspondencias as (
    select t.id as turma_id, min(o.id) as oferta_id, count(o.id) as quantidade
    from turmas t
    left join ofertas_disciplinas o
      on o.turma_id = t.id
     and o.disciplina_id = t.disciplina_id
     and o.ano_letivo_id = t.ano_letivo_id
     and o.periodo_letivo_id is not distinct from t.periodo_letivo_id
    group by t.id
), classificacao as (
    select t.id as turma_id,
           case when c.quantidade = 1 then c.oferta_id end as oferta_id,
           c.quantidade,
           concat_ws(', ',
               case when c.quantidade = 1 and o.professor_id is null then 'professor da oferta' end,
               case when c.quantidade = 1 and o.modulo_id is null then 'modulo da oferta' end,
               case when c.quantidade = 1 and nullif(trim(o.horario), '') is null then 'horario da oferta' end,
               case when c.quantidade = 1 and nullif(trim(o.sala), '') is null then 'sala da oferta' end,
               case when c.quantidade = 1 and o.vagas is null then 'vagas da oferta' end,
               case when c.quantidade = 1 and o.status is null then 'situacao da oferta' end,
               case when c.quantidade = 0 and t.disciplina_id is null then 'disciplina' end,
               case when c.quantidade = 0 and t.professor_id is null then 'professor' end,
               case when c.quantidade = 0 and t.ano_letivo_id is null then 'ano letivo' end,
               case when c.quantidade = 0 and d.modulo_id is null then 'modulo' end,
               case when c.quantidade = 0 and nullif(trim(t.horario), '') is null then 'horario' end,
               case when c.quantidade = 0 and nullif(trim(t.sala), '') is null then 'sala' end,
               case when c.quantidade = 0 then 'vagas' end,
               case when c.quantidade = 0 then 'situacao da oferta' end
           ) as campos_ausentes,
           concat_ws('; ',
               case when c.quantidade > 1 then 'mais de uma oferta corresponde a turma, disciplina, ano e periodo' end,
               case when c.quantidade = 1 and o.curso_id is distinct from t.curso_id
                         and o.curso_id is not null and t.curso_id is not null then 'curso divergente' end,
               case when c.quantidade = 1 and o.modulo_id is distinct from d.modulo_id
                         and o.modulo_id is not null and d.modulo_id is not null then 'modulo divergente' end,
               case when c.quantidade = 1 and o.professor_id is distinct from t.professor_id
                         and o.professor_id is not null and t.professor_id is not null then 'professor divergente' end,
               case when c.quantidade = 1 and lower(trim(o.horario)) is distinct from lower(trim(t.horario))
                         and nullif(trim(o.horario), '') is not null and nullif(trim(t.horario), '') is not null then 'horario divergente' end,
               case when c.quantidade = 1 and lower(trim(o.sala)) is distinct from lower(trim(t.sala))
                         and nullif(trim(o.sala), '') is not null and nullif(trim(t.sala), '') is not null then 'sala divergente' end
           ) as divergencias
    from turmas t
    join correspondencias c on c.turma_id = t.id
    left join ofertas_disciplinas o on o.id = c.oferta_id and c.quantidade = 1
    left join disciplinas d on d.id = t.disciplina_id
)
insert into migracao_turma_oferta_pendencia
    (turma_id, oferta_id, tipo_pendencia, campos_ausentes, divergencias,
     acao_automatica, motivo_intervencao_manual, resolvida, data_resolucao)
select turma_id,
       oferta_id,
       case
           when quantidade = 1 and divergencias = '' and campos_ausentes = '' then 'OFERTA_CORRESPONDENTE_CONSISTENTE'
           when quantidade = 1 then 'OFERTA_CORRESPONDENTE_DIVERGENTE'
           when quantidade = 0 and campos_ausentes = '' then 'SEM_OFERTA_DADOS_COMPLETOS'
           else 'SEM_OFERTA_AMBIGUA_OU_INCOMPLETA'
       end,
       nullif(campos_ausentes, ''),
       nullif(divergencias, ''),
       case when quantidade = 1
            then 'Campos nulos da oferta preenchidos somente quando havia uma unica origem legada inequivoca; divergencias preservadas.'
            else 'Nenhuma oferta criada automaticamente.' end,
       case
           when quantidade = 1 and divergencias = '' and campos_ausentes = '' then null
           when quantidade = 1 and campos_ausentes <> '' then 'Completar manualmente os campos obrigatorios ausentes da oferta.'
           when quantidade = 1 then 'Revisar manualmente as divergencias; a OfertaDisciplina foi preservada como fonte oficial.'
           when quantidade > 1 then 'Selecionar manualmente a oferta correta antes de qualquer consolidacao.'
           else 'Nao ha vagas e situacao de oferta inequivocas na Turma; completar os dados e criar a oferta manualmente.'
       end,
       quantidade = 1 and divergencias = '' and campos_ausentes = '',
       case when quantidade = 1 and divergencias = '' and campos_ausentes = '' then current_timestamp end
from classificacao;
