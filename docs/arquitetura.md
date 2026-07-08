# Arquitetura do SGA

O SGA usa um monorepo com backend Quarkus e frontend Angular.

- `backend`: API REST em Java 21, Quarkus, Hibernate/Panache, PostgreSQL, Flyway, JWT HMAC e Swagger.
- `frontend`: Angular com rotas protegidas, login, dashboard e telas administrativas genericas.
- `docker-compose.yml`: PostgreSQL, API e frontend Nginx para ambiente local.

As regras academicas automaticas ficam em `AcademicoService`: matricula cria historico, notas calculam media e atualizam historico, frequencias consolidam percentual, e encerramento valida pendencias.
