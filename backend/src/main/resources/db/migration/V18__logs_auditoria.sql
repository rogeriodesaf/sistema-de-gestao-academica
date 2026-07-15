create table logs_auditoria (
    id bigserial primary key,
    usuario_id bigint references usuarios(id) on delete set null,
    usuario_nome varchar(255),
    usuario_email varchar(255),
    perfil varchar(50),
    acao varchar(30) not null,
    metodo varchar(10) not null,
    rota varchar(500) not null,
    status_http integer not null,
    sucesso boolean not null,
    data_hora timestamp not null default current_timestamp
);

create index idx_logs_auditoria_data_hora on logs_auditoria (data_hora desc);
create index idx_logs_auditoria_usuario on logs_auditoria (usuario_id);
