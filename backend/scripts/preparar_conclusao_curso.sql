begin;

insert into usuarios (id, nome, email, senha_hash, perfil, ativo, data_criacao)
values (900001, 'Aluno Concluinte Teste', 'concluinte.teste@sga.local',
        'AQIDBAUGBwgJCgsMDQ4PEA==:v5U0fegnZFabSPNZmHlkN968C9KNLT6lokKiEffekKM=',
        'ALUNO', true, current_timestamp)
on conflict (email) do update set nome = excluded.nome, senha_hash = excluded.senha_hash,
    perfil = excluded.perfil, ativo = true;

insert into usuarios (id, nome, email, senha_hash, perfil, ativo, data_criacao)
values (900002, 'Coordenador Teste', 'coordenador.teste@sga.local',
        'AQIDBAUGBwgJCgsMDQ4PEA==:2DsjcmvE9yTkl/ahQeDWoEVk1TzyacrPtEZWr5oVXYU=',
        'COORDENADOR', true, current_timestamp)
on conflict (email) do update set nome = excluded.nome, senha_hash = excluded.senha_hash,
    perfil = excluded.perfil, ativo = true;

insert into alunos (id, nome, cpf, email, status, data_ingresso, usuario_id, curso_id, observacoes)
select 900001, 'Aluno Concluinte Teste', '999.900.001-00', 'concluinte.teste@sga.local',
       'ATIVO', current_date, u.id, c.id, 'Cenario idempotente de conclusao do curso'
from usuarios u
join cursos c on lower(c.nome) = 'curso de teologia'
where u.email = 'concluinte.teste@sga.local'
on conflict (id) do update set curso_id = excluded.curso_id, usuario_id = excluded.usuario_id,
    status = 'ATIVO', email = excluded.email;

insert into matriculas (id, aluno_id, curso_id, data_matricula, status)
select 900001, a.id, a.curso_id, current_date, 'EM_ANDAMENTO'
from alunos a
where a.id = 900001
  and not exists (select 1 from matriculas m where m.aluno_id = a.id
      and m.curso_id = a.curso_id and m.turma_id is null and m.disciplina_id is null);

update matriculas set status = 'EM_ANDAMENTO', data_conclusao = null
where aluno_id = 900001 and curso_id = (select curso_id from alunos where id = 900001)
  and turma_id is null and disciplina_id is null;

insert into anos_letivos (id, turma_id, ano, data_inicio, data_fim, status)
select 900001, null, 2026, date '2026-01-01', date '2026-12-31', 'EM_ANDAMENTO'
where not exists (select 1 from anos_letivos where turma_id is null and ano = 2026);

insert into modulos (id, nome, descricao, ordem, curso_id, ano_letivo_id,
                     data_inicio, data_fim, status, ativo)
select 900001, 'Modulo de Conclusao 2026', 'Modulo do cenario de conclusao', 999,
       c.id, a.id, date '2026-07-01', date '2026-08-31', 'ABERTO', true
from cursos c cross join lateral (
    select id from anos_letivos where turma_id is null and ano = 2026 order by id limit 1
) a
where lower(c.nome) = 'curso de teologia'
  and not exists (select 1 from modulos where nome = 'Modulo de Conclusao 2026');

insert into turmas (id, nome, curso_id, ano_letivo_id, descricao, turno,
                    quantidade_maxima_alunos, data_inicio, data_termino, status)
select 900001, 'Conclusao Teologia 2026', c.id, a.id,
       'Agrupamento administrativo do cenario de conclusao', 'NOITE', 10,
       date '2026-07-01', date '2026-08-31', 'EM_ANDAMENTO'
from cursos c
cross join lateral (
    select id from anos_letivos where turma_id is null and ano = 2026 order by id limit 1
) a
where lower(c.nome) = 'curso de teologia'
  and not exists (select 1 from turmas where id = 900001);

insert into ofertas_disciplinas
    (id, turma_id, ano_letivo_id, periodo_letivo_id, curso_id, modulo_id,
     disciplina_id, professor_id, vagas, horario, sala, carga_horaria_prevista,
     carga_horaria_ministrada, data_inicio, data_fim, status)
select 900001, t.id, a.id, null, c.id, m.id, d.id, p.id, 10,
       'Segunda-feira, 19h', 'Sala Conclusao', d.carga_horaria, 0,
       date '2026-07-01', date '2026-08-31', 'EM_ANDAMENTO'
from turmas t
join cursos c on lower(c.nome) = 'curso de teologia'
join disciplinas d on d.curso_id = c.id and d.codigo = 'STCJP-M01-01'
join professores p on p.email = 'professor@seminario.local'
join modulos m on m.nome = 'Modulo de Conclusao 2026'
join anos_letivos a on a.id = m.ano_letivo_id
where t.id = 900001
  and not exists (select 1 from ofertas_disciplinas where id = 900001);

insert into matriculas_disciplinas
    (id, aluno_id, curso_id, oferta_disciplina_id, data_matricula, status, observacoes)
select 900001, 900001, c.id, 900001, current_date, 'MATRICULADO',
       'Ultima disciplina do cenario de conclusao'
from cursos c
where lower(c.nome) = 'curso de teologia'
  and not exists (select 1 from matriculas_disciplinas
                  where aluno_id = 900001 and oferta_disciplina_id = 900001);

delete from arquivos_professor where oferta_disciplina_id = 900001
   or aula_id in (select id from aulas_ministradas where oferta_disciplina_id = 900001)
   or avaliacao_id in (select id from avaliacoes where oferta_disciplina_id = 900001);
delete from notas_avaliacoes where matricula_disciplina_id = 900001
   or avaliacao_id in (select id from avaliacoes where oferta_disciplina_id = 900001);
delete from frequencias where matricula_disciplina_id = 900001
   or aula_id in (select id from aulas_ministradas where oferta_disciplina_id = 900001);
delete from historicos_escolares where aluno_id = 900001 and oferta_disciplina_id = 900001;
delete from avaliacoes where oferta_disciplina_id = 900001;
delete from aulas_ministradas where oferta_disciplina_id = 900001;
delete from notas where oferta_disciplina_id = 900001;

update ofertas_disciplinas set status = 'EM_ANDAMENTO', carga_horaria_ministrada = 0,
    data_encerramento = null, encerrado_por_professor_id = null,
    data_homologacao = null, homologado_por_usuario_id = null,
    data_reabertura = null, reaberto_por_usuario_id = null, motivo_reabertura = null
where id = 900001;

update matriculas_disciplinas set status = 'MATRICULADO', nota_final = null,
    frequencia_final = null, data_consolidacao = null
where id = 900001;

insert into historicos_escolares
    (id, aluno_id, curso_id, turma_id, disciplina_id, professor_responsavel_id,
     carga_horaria, nota_final, frequencia_final, situacao, periodo_cursado,
     oferta_disciplina_id, matricula_disciplina_id)
select 900100 + row_number() over (order by d.id), 900001, c.id, null, d.id,
       p.id, d.carga_horaria, 8.00, 90.00, 'APROVADO',
       'Integralizacao anterior', null, null
from cursos c
join disciplinas d on d.curso_id = c.id and d.ativo = true
join professores p on p.email = 'professor@seminario.local'
where lower(c.nome) = 'curso de teologia'
  and d.codigo <> 'STCJP-M01-01'
  and not exists (select 1 from historicos_escolares h
                  where h.aluno_id = 900001 and h.disciplina_id = d.id
                    and h.situacao = 'APROVADO');

commit;
