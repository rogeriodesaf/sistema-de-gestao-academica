alter table frequencias add column if not exists matricula_disciplina_id bigint references matriculas_disciplinas(id);
alter table frequencias add column if not exists status varchar(40);

update frequencias
set status = case
    when presente then 'PRESENTE'
    when coalesce(justificativa, '') <> '' then 'JUSTIFICADO'
    else 'AUSENTE'
end
where status is null;

update frequencias f
set matricula_disciplina_id = m.id
from aulas_ministradas a
join matriculas_disciplinas m on m.oferta_disciplina_id = a.oferta_disciplina_id
where f.aula_id = a.id
  and f.aluno_id = m.aluno_id
  and f.matricula_disciplina_id is null;

alter table frequencias alter column status set not null;
