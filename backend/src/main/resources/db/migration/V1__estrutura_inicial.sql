create table usuarios (
    id bigserial primary key,
    nome varchar(255) not null,
    email varchar(255) not null unique,
    senha_hash varchar(500) not null,
    perfil varchar(40) not null,
    ativo boolean not null default true
);

create table alunos (
    id bigserial primary key,
    nome varchar(255) not null,
    cpf varchar(20) unique,
    email varchar(255),
    telefone varchar(50),
    data_nascimento date,
    endereco varchar(500),
    status varchar(40) not null,
    data_ingresso date,
    observacoes varchar(2000),
    usuario_id bigint references usuarios(id)
);

create table professores (
    id bigserial primary key,
    nome varchar(255) not null,
    cpf varchar(20) unique,
    email varchar(255),
    telefone varchar(50),
    formacao varchar(500),
    ativo boolean not null default true,
    usuario_id bigint references usuarios(id)
);

create table cursos (
    id bigserial primary key,
    nome varchar(255) not null,
    descricao varchar(2000),
    carga_horaria_total integer,
    ativo boolean not null default true
);

create table disciplinas (
    id bigserial primary key,
    nome varchar(255) not null,
    codigo varchar(80) unique,
    carga_horaria integer,
    ementa varchar(4000),
    bibliografia varchar(4000),
    ativo boolean not null default true
);

create table turmas (
    id bigserial primary key,
    nome varchar(255) not null,
    curso_id bigint references cursos(id),
    ano_periodo varchar(80),
    data_inicio date,
    data_termino date,
    status varchar(40) not null
);

create table vinculos_professor_disciplina_turma (
    id bigserial primary key,
    professor_id bigint references professores(id),
    disciplina_id bigint references disciplinas(id),
    turma_id bigint references turmas(id),
    periodo varchar(80),
    status varchar(40) not null
);

create table matriculas (
    id bigserial primary key,
    aluno_id bigint references alunos(id),
    curso_id bigint references cursos(id),
    turma_id bigint references turmas(id),
    disciplina_id bigint references disciplinas(id),
    data_matricula date,
    status varchar(40) not null,
    unique (aluno_id, turma_id, disciplina_id)
);

create table historicos_escolares (
    id bigserial primary key,
    aluno_id bigint references alunos(id),
    curso_id bigint references cursos(id),
    turma_id bigint references turmas(id),
    disciplina_id bigint references disciplinas(id),
    professor_responsavel_id bigint references professores(id),
    carga_horaria integer,
    nota_final numeric(5,2),
    frequencia_final numeric(5,2),
    situacao varchar(40) not null,
    periodo_cursado varchar(80),
    unique (aluno_id, turma_id, disciplina_id)
);

create table planos_ensino (
    id bigserial primary key,
    disciplina_id bigint references disciplinas(id),
    turma_id bigint references turmas(id),
    objetivos varchar(4000),
    ementa varchar(4000),
    conteudo_programatico varchar(4000),
    metodologia varchar(4000),
    criterios_avaliacao varchar(4000),
    bibliografia_basica varchar(4000),
    bibliografia_complementar varchar(4000),
    data_cadastro timestamp,
    ultima_atualizacao timestamp,
    unique (disciplina_id, turma_id)
);

create table aulas_ministradas (
    id bigserial primary key,
    disciplina_id bigint references disciplinas(id),
    turma_id bigint references turmas(id),
    professor_id bigint references professores(id),
    data_aula date,
    conteudo_ministrado varchar(4000),
    observacoes varchar(2000),
    carga_horaria_aula integer
);

create table frequencias (
    id bigserial primary key,
    aula_id bigint references aulas_ministradas(id),
    aluno_id bigint references alunos(id),
    presente boolean not null,
    justificativa varchar(500),
    observacao varchar(2000),
    unique (aula_id, aluno_id)
);

create table notas (
    id bigserial primary key,
    aluno_id bigint references alunos(id),
    disciplina_id bigint references disciplinas(id),
    turma_id bigint references turmas(id),
    nota1 numeric(5,2),
    nota2 numeric(5,2),
    trabalho numeric(5,2),
    avaliacao_final numeric(5,2),
    media_final numeric(5,2),
    situacao varchar(40) not null,
    unique (aluno_id, disciplina_id, turma_id)
);
