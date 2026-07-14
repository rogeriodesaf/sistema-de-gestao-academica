create sequence avaliacoes_SEQ start with 1 increment by 50;
create sequence notas_avaliacoes_SEQ start with 1 increment by 50;
create sequence arquivos_professor_SEQ start with 1 increment by 50;

create table avaliacoes (
    id bigint primary key,
    oferta_disciplina_id bigint not null references ofertas_disciplinas(id),
    professor_id bigint not null references professores(id),
    nome varchar(255) not null,
    descricao varchar(2000),
    ordem_avaliacao integer not null,
    data_avaliacao date,
    nota_maxima numeric(7,2) not null,
    peso numeric(7,2) not null,
    data_criacao timestamp not null,
    unique (oferta_disciplina_id, ordem_avaliacao),
    unique (oferta_disciplina_id, nome)
);

create table notas_avaliacoes (
    id bigint primary key,
    avaliacao_id bigint not null references avaliacoes(id),
    aluno_id bigint not null references alunos(id),
    matricula_disciplina_id bigint not null references matriculas_disciplinas(id),
    nota numeric(7,2) not null,
    observacao varchar(2000),
    data_atualizacao timestamp not null,
    unique (avaliacao_id, aluno_id)
);

create table arquivos_professor (
    id bigint primary key,
    professor_id bigint not null references professores(id),
    oferta_disciplina_id bigint not null references ofertas_disciplinas(id),
    aula_id bigint references aulas_ministradas(id),
    avaliacao_id bigint references avaliacoes(id),
    tipo_vinculo varchar(40) not null,
    titulo varchar(255) not null,
    nome_original varchar(255) not null,
    mime_type varchar(120) not null,
    tamanho bigint not null,
    caminho varchar(2000) not null,
    data_envio timestamp not null
);
