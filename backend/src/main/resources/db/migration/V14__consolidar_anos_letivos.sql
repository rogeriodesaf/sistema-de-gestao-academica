alter table anos_letivos add column if not exists legado boolean not null default false;

create temporary table anos_letivos_oficiais on commit drop as
select distinct on (ano) ano, id as oficial_id
from anos_letivos
order by ano,
         (turma_id is null) desc,
         (data_inicio is not null and data_fim is not null) desc,
         id;

update anos_letivos ano
set legado = ano.id <> oficial.oficial_id
from anos_letivos_oficiais oficial
where oficial.ano = ano.ano;

update anos_letivos ano
set turma_id = null
from anos_letivos_oficiais oficial
where ano.id = oficial.oficial_id;

update modulos referencia
set ano_letivo_id = oficial.oficial_id
from anos_letivos antigo
join anos_letivos_oficiais oficial on oficial.ano = antigo.ano
where referencia.ano_letivo_id = antigo.id
  and antigo.id <> oficial.oficial_id;

update ofertas_disciplinas referencia
set ano_letivo_id = oficial.oficial_id
from anos_letivos antigo
join anos_letivos_oficiais oficial on oficial.ano = antigo.ano
where referencia.ano_letivo_id = antigo.id
  and antigo.id <> oficial.oficial_id;

update periodos_letivos referencia
set ano_letivo_id = oficial.oficial_id
from anos_letivos antigo
join anos_letivos_oficiais oficial on oficial.ano = antigo.ano
where referencia.ano_letivo_id = antigo.id
  and antigo.id <> oficial.oficial_id
  and not exists (
      select 1
      from periodos_letivos existente
      where existente.ano_letivo_id = oficial.oficial_id
        and existente.ordem = referencia.ordem
  )
  and referencia.id = (
      select min(candidato.id)
      from periodos_letivos candidato
      join anos_letivos ano_candidato on ano_candidato.id = candidato.ano_letivo_id
      where ano_candidato.ano = antigo.ano
        and candidato.ordem = referencia.ordem
  );

update turmas referencia
set ano_letivo_id = oficial.oficial_id
from anos_letivos antigo
join anos_letivos_oficiais oficial on oficial.ano = antigo.ano
where referencia.ano_letivo_id = antigo.id
  and antigo.id <> oficial.oficial_id;

create unique index if not exists ux_anos_letivos_oficiais_ano
    on anos_letivos (ano) where legado = false;
