alter table cursos add column if not exists grade_pdf_caminho varchar(1000);
alter table cursos add column if not exists grade_pdf_nome varchar(255);
alter table cursos add column if not exists grade_pdf_tipo varchar(120);
alter table cursos add column if not exists grade_pdf_tamanho bigint;

alter table disciplinas add column if not exists creditos integer;
alter table disciplinas add column if not exists professor_responsavel_id bigint references professores(id);

alter table modulos add column if not exists status varchar(40);
update modulos set status = case when ativo then 'ABERTO' else 'INATIVO' end where status is null;
alter table modulos alter column status set not null;
