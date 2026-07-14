alter table disciplinas
    add column if not exists tipo_componente varchar(30) not null default 'OBRIGATORIA';

update disciplinas
set tipo_componente = 'OPTATIVA'
where lower(nome) like '%optativa%';

update disciplinas
set tipo_componente = 'COMPLEMENTAR'
where lower(nome) like 'práticas ministeriais%'
   or lower(nome) like 'praticas ministeriais%';
