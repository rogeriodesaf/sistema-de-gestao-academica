update ofertas_disciplinas o
set status = 'EM_ANDAMENTO'
where o.status = 'ABERTA'
  and exists (
      select 1
      from matriculas_disciplinas md
      where md.oferta_disciplina_id = o.id
        and md.status in ('ATIVA', 'MATRICULADO')
  );
