alter table matriculas_disciplinas
    add column if not exists resultado_academico varchar(60) not null default 'EM_ANDAMENTO';

alter table historicos_escolares add column if not exists disciplina_nome varchar(255);
alter table historicos_escolares add column if not exists disciplina_codigo varchar(120);
alter table historicos_escolares add column if not exists modulo_nome varchar(255);
alter table historicos_escolares add column if not exists creditos integer;
alter table historicos_escolares add column if not exists professor_nome varchar(255);
alter table historicos_escolares add column if not exists data_homologacao timestamp;

create sequence if not exists migracao_matricula_resultado_pendencia_SEQ start with 1 increment by 50;

create table if not exists migracao_matricula_resultado_pendencia (
    id bigint primary key default nextval('migracao_matricula_resultado_pendencia_SEQ'),
    matricula_id bigint not null references matriculas_disciplinas(id),
    status_original varchar(80),
    resultado_migrado varchar(60),
    problema varchar(2000) not null,
    acao_executada varchar(2000),
    necessita_intervencao_manual boolean not null default true,
    resolvida boolean not null default false,
    data_criacao timestamp not null default current_timestamp,
    data_resolucao timestamp
);

create index if not exists ix_migracao_matricula_resultado_pendente
    on migracao_matricula_resultado_pendencia (resolvida, matricula_id);

-- Recupera primeiro o resultado consolidado ligado diretamente a matricula.
update matriculas_disciplinas m
set resultado_academico = case h.situacao
    when 'APROVADO' then 'APROVADO'
    when 'REPROVADO_POR_NOTA' then 'REPROVADO_POR_NOTA'
    when 'REPROVADO_POR_FREQUENCIA' then 'REPROVADO_POR_FREQUENCIA'
    when 'REPROVADO_POR_NOTA_E_FREQUENCIA' then 'REPROVADO_POR_NOTA_E_FREQUENCIA'
    else m.resultado_academico end
from historicos_escolares h
where h.matricula_disciplina_id = m.id
  and h.situacao in ('APROVADO', 'REPROVADO_POR_NOTA', 'REPROVADO_POR_FREQUENCIA',
                     'REPROVADO_POR_NOTA_E_FREQUENCIA');

-- Para legados sem FK, aceita somente um unico historico da mesma pessoa e oferta.
with historico_unico as (
    select aluno_id, oferta_disciplina_id, min(id) as historico_id
    from historicos_escolares
    where oferta_disciplina_id is not null
    group by aluno_id, oferta_disciplina_id
    having count(*) = 1
)
update matriculas_disciplinas m
set resultado_academico = case h.situacao
    when 'APROVADO' then 'APROVADO'
    when 'REPROVADO_POR_NOTA' then 'REPROVADO_POR_NOTA'
    when 'REPROVADO_POR_FREQUENCIA' then 'REPROVADO_POR_FREQUENCIA'
    when 'REPROVADO_POR_NOTA_E_FREQUENCIA' then 'REPROVADO_POR_NOTA_E_FREQUENCIA'
    else m.resultado_academico end
from historico_unico u
join historicos_escolares h on h.id = u.historico_id
where m.aluno_id = u.aluno_id
  and m.oferta_disciplina_id = u.oferta_disciplina_id
  and m.resultado_academico = 'EM_ANDAMENTO';

-- Valores cujo significado e inequivoco na implementacao anterior.
update matriculas_disciplinas set resultado_academico = 'APROVADO'
where status in ('APROVADO', 'CONCLUIDA', 'CONCLUIDO') and resultado_academico = 'EM_ANDAMENTO';
update matriculas_disciplinas set resultado_academico = 'REPROVADO_POR_NOTA'
where status = 'REPROVADO_POR_NOTA' and resultado_academico = 'EM_ANDAMENTO';
update matriculas_disciplinas set resultado_academico = 'REPROVADO_POR_FREQUENCIA'
where status = 'REPROVADO_POR_FREQUENCIA' and resultado_academico = 'EM_ANDAMENTO';

insert into migracao_matricula_resultado_pendencia
    (matricula_id, status_original, resultado_migrado, problema, acao_executada)
select m.id, m.status, m.resultado_academico,
       case when m.status = 'REPROVADA' or exists (
                select 1 from historicos_escolares h
                where (h.matricula_disciplina_id = m.id
                       or (h.matricula_disciplina_id is null
                           and h.aluno_id = m.aluno_id
                           and h.oferta_disciplina_id = m.oferta_disciplina_id))
                  and h.situacao = 'REPROVADO')
            then 'Status academico generico nao informa se a reprovacao foi por nota ou frequencia.'
            else 'Matricula consolidada sem historico ou resultado academico determinavel com seguranca.' end,
       'Dados preservados; nenhuma classificacao definitiva foi inventada.'
from matriculas_disciplinas m
where m.resultado_academico = 'EM_ANDAMENTO'
  and (m.status = 'REPROVADA' or m.data_consolidacao is not null or exists (
        select 1 from historicos_escolares h
        where (h.matricula_disciplina_id = m.id
               or (h.matricula_disciplina_id is null
                   and h.aluno_id = m.aluno_id
                   and h.oferta_disciplina_id = m.oferta_disciplina_id))
          and h.situacao = 'REPROVADO'));

-- Normaliza somente estados administrativos cujo resultado ja foi preservado.
update matriculas_disciplinas
set status = 'ATIVA'
where status = 'MATRICULADO'
   or (resultado_academico <> 'EM_ANDAMENTO'
       and status in ('APROVADO', 'CONCLUIDA', 'CONCLUIDO',
                      'REPROVADO_POR_NOTA', 'REPROVADO_POR_FREQUENCIA'));

-- Congela o retrato historico disponivel sem substituir valores ja registrados.
update historicos_escolares h
set disciplina_nome = coalesce(h.disciplina_nome,
        (select d.nome from disciplinas d where d.id = h.disciplina_id)),
    disciplina_codigo = coalesce(h.disciplina_codigo,
        (select d.codigo from disciplinas d where d.id = h.disciplina_id)),
    modulo_nome = coalesce(h.modulo_nome,
        (select mo.nome from ofertas_disciplinas o left join modulos mo on mo.id = o.modulo_id
         where o.id = h.oferta_disciplina_id)),
    creditos = coalesce(h.creditos,
        (select d.creditos from disciplinas d where d.id = h.disciplina_id)),
    professor_nome = coalesce(h.professor_nome,
        (select p.nome from professores p where p.id = h.professor_responsavel_id)),
    data_homologacao = coalesce(h.data_homologacao,
        (select o.data_homologacao from ofertas_disciplinas o where o.id = h.oferta_disciplina_id));
