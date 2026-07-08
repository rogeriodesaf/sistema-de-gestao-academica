create sequence if not exists modulos_SEQ start with 1 increment by 50;

create table if not exists modulos (
    id bigserial primary key,
    nome varchar(255) not null,
    descricao varchar(2000),
    ordem integer not null,
    curso_id bigint not null references cursos(id),
    ativo boolean not null default true,
    unique (curso_id, ordem)
);

alter table alunos add column if not exists curso_id bigint references cursos(id);

alter table disciplinas add column if not exists curso_id bigint references cursos(id);
alter table disciplinas add column if not exists modulo_id bigint references modulos(id);
alter table disciplinas add column if not exists ementa_resumo varchar(4000);
alter table disciplinas add column if not exists ementa_pdf_caminho varchar(1000);
alter table disciplinas add column if not exists ementa_pdf_nome varchar(255);
alter table disciplinas add column if not exists ementa_pdf_tipo varchar(120);
alter table disciplinas add column if not exists ementa_pdf_tamanho bigint;

alter table ofertas_disciplinas add column if not exists curso_id bigint references cursos(id);
alter table ofertas_disciplinas add column if not exists modulo_id bigint references modulos(id);
alter table ofertas_disciplinas add column if not exists horario varchar(255);
alter table ofertas_disciplinas add column if not exists sala varchar(120);

alter table matriculas_disciplinas add column if not exists curso_id bigint references cursos(id);
alter table matriculas_disciplinas add column if not exists periodo_letivo_id bigint references periodos_letivos(id);
alter table matriculas_disciplinas add column if not exists nota_final numeric(5,2);
alter table matriculas_disciplinas add column if not exists frequencia_final numeric(5,2);

alter table planos_ensino add column if not exists oferta_disciplina_id bigint references ofertas_disciplinas(id);
alter table planos_ensino add column if not exists observacoes varchar(4000);
alter table planos_ensino add column if not exists plano_pdf_caminho varchar(1000);
alter table planos_ensino add column if not exists plano_pdf_nome varchar(255);
alter table planos_ensino add column if not exists plano_pdf_tipo varchar(120);
alter table planos_ensino add column if not exists plano_pdf_tamanho bigint;
