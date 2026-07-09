create sequence if not exists perfil_permissoes_SEQ start with 1 increment by 50;

create table if not exists perfil_permissoes (
    id bigserial primary key,
    perfil varchar(40) not null,
    area varchar(80) not null,
    recurso varchar(120) not null,
    visualizar boolean not null default false,
    criar boolean not null default false,
    editar boolean not null default false,
    excluir boolean not null default false,
    unique (perfil, recurso)
);
