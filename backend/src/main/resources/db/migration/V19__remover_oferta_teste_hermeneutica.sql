create temporary table ofertas_teste_hermeneutica on commit drop as
select o.id
from ofertas_disciplinas o
join disciplinas d on d.id = o.disciplina_id
join turmas t on t.id = o.turma_id
left join professores p on p.id = o.professor_id
where upper(trim(d.codigo)) = 'HER-101'
  and lower(trim(t.nome)) = lower('Turma Teologia 2026')
  and lower(trim(coalesce(p.nome, ''))) = lower('Professor Exemplo')
  and lower(trim(coalesce(o.horario, ''))) = lower('Segunda 19h')
  and lower(trim(coalesce(o.sala, ''))) = lower('Sala 1');

insert into logs_auditoria
    (usuario_nome, perfil, acao, metodo, rota, status_http, sucesso, data_hora)
select 'Sistema', 'SISTEMA', 'LIMPEZA_DADOS_TESTE', 'MIGRATION',
       'ofertas-disciplinas/' || id, 200, true, current_timestamp
from ofertas_teste_hermeneutica;

delete from arquivos_professor
where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica);

delete from notas_avaliacoes
where avaliacao_id in (
    select id from avaliacoes
    where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica)
)
or matricula_disciplina_id in (
    select id from matriculas_disciplinas
    where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica)
);

delete from frequencias
where aula_id in (
    select id from aulas_ministradas
    where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica)
)
or matricula_disciplina_id in (
    select id from matriculas_disciplinas
    where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica)
);

delete from historicos_escolares
where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica)
   or matricula_disciplina_id in (
       select id from matriculas_disciplinas
       where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica)
   );

delete from notas
where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica);

delete from planos_ensino
where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica);

delete from avaliacoes
where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica);

delete from aulas_ministradas
where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica);

delete from matriculas_disciplinas
where oferta_disciplina_id in (select id from ofertas_teste_hermeneutica);

delete from ofertas_disciplinas
where id in (select id from ofertas_teste_hermeneutica);
