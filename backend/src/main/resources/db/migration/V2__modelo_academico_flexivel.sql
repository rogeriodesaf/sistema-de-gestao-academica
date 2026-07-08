create sequence anos_letivos_SEQ start with 1 increment by 50;
create sequence periodos_letivos_SEQ start with 1 increment by 50;
create sequence ofertas_disciplinas_SEQ start with 1 increment by 50;
create sequence matriculas_disciplinas_SEQ start with 1 increment by 50;

alter table turmas add column if not exists descricao varchar(2000);
alter table turmas add column if not exists turno varchar(80);
alter table turmas add column if not exists quantidade_maxima_alunos integer;

create table anos_letivos (
    id bigserial primary key,
    turma_id bigint not null references turmas(id),
    ano integer not null,
    data_inicio date,
    data_fim date,
    status varchar(40) not null,
    unique (turma_id, ano)
);

create table periodos_letivos (
    id bigserial primary key,
    ano_letivo_id bigint not null references anos_letivos(id),
    nome varchar(255) not null,
    ordem integer not null,
    tipo varchar(40) not null,
    data_inicio date,
    data_fim date,
    status varchar(40) not null,
    unique (ano_letivo_id, ordem)
);

create table ofertas_disciplinas (
    id bigserial primary key,
    turma_id bigint not null references turmas(id),
    ano_letivo_id bigint not null references anos_letivos(id),
    periodo_letivo_id bigint not null references periodos_letivos(id),
    disciplina_id bigint not null references disciplinas(id),
    professor_id bigint references professores(id),
    vagas integer,
    carga_horaria_prevista integer,
    carga_horaria_ministrada integer,
    data_inicio date,
    data_fim date,
    status varchar(40) not null,
    unique (turma_id, ano_letivo_id, periodo_letivo_id, disciplina_id)
);

create table matriculas_disciplinas (
    id bigserial primary key,
    aluno_id bigint not null references alunos(id),
    oferta_disciplina_id bigint not null references ofertas_disciplinas(id),
    data_matricula date,
    status varchar(40) not null,
    observacoes varchar(2000),
    unique (aluno_id, oferta_disciplina_id)
);

alter table historicos_escolares add column if not exists oferta_disciplina_id bigint references ofertas_disciplinas(id);
alter table historicos_escolares add column if not exists matricula_disciplina_id bigint references matriculas_disciplinas(id);
alter table notas add column if not exists oferta_disciplina_id bigint references ofertas_disciplinas(id);
alter table aulas_ministradas add column if not exists oferta_disciplina_id bigint references ofertas_disciplinas(id);
