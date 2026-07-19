update turmas t
set status = 'ABERTA'
where t.status = 'ENCERRADA'
  and exists (
      select 1
      from ofertas_disciplinas o
      where o.turma_id = t.id
        and (o.status <> 'CONCLUIDA' or o.data_homologacao is null)
  );
