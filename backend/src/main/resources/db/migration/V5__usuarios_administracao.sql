alter table usuarios add column if not exists data_criacao timestamp;
alter table usuarios add column if not exists ultimo_acesso timestamp;
alter table usuarios add column if not exists observacoes varchar(2000);

update usuarios set data_criacao = coalesce(data_criacao, now());
