create table tokens_redefinicao_senha (
    id bigserial primary key,
    usuario_id bigint not null references usuarios(id),
    token_hash varchar(64) not null unique,
    data_criacao timestamp not null,
    data_expiracao timestamp not null,
    data_utilizacao timestamp,
    situacao varchar(20) not null
);

create index idx_token_redefinicao_hash on tokens_redefinicao_senha(token_hash);
create index idx_token_redefinicao_usuario on tokens_redefinicao_senha(usuario_id);
