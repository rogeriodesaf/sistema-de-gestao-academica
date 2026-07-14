alter table cursos add column if not exists creditos_totais integer;
alter table matriculas add column if not exists data_conclusao date;

update cursos
set carga_horaria_total = 3424,
    creditos_totais = 190
where lower(nome) = lower('Curso de Teologia');

insert into matriculas (aluno_id, curso_id, data_matricula, status)
select a.id, a.curso_id, coalesce(a.data_ingresso, current_date), 'EM_ANDAMENTO'
from alunos a
where a.curso_id is not null
  and not exists (
      select 1
      from matriculas m
      where m.aluno_id = a.id
        and m.curso_id = a.curso_id
        and m.turma_id is null
        and m.disciplina_id is null
  );
