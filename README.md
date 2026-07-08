# SGA - Sistema de Gestao Academica

Sistema web para gestao academica de seminarios de teologia.

## Stack

- Backend: Java 21, Quarkus, REST, Hibernate/Panache, PostgreSQL, Flyway, Jakarta Validation, JWT, Swagger/OpenAPI.
- Frontend: Angular, TypeScript, rotas protegidas, layout administrativo responsivo.
- Infra: Docker, Docker Compose e configuracao por variaveis de ambiente.

## Estrutura

```text
sistema-de-gestao-academica/
├── backend/
├── frontend/
├── docker/
├── docs/
├── scripts/
├── docker-compose.yml
└── README.md
```

## Execucao com Docker

```bash
cp .env.example .env
docker compose up --build
```

- Frontend: http://localhost:8088
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/q/swagger-ui

Usuario inicial:

- E-mail: `admin@sga.local`
- Senha: `admin123`

## Desenvolvimento local

Backend:

```bash
cd backend
mvn quarkus:dev
```

Frontend:

```bash
cd frontend
npm install
npm start -- --proxy-config proxy.conf.json
```

## Variaveis de ambiente

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_SECONDS`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`
- `CORS_ORIGINS`

## Deploy no Render

Crie um PostgreSQL no Render e configure um Web Service para o backend usando `backend/Dockerfile`.

Variaveis recomendadas:

```text
DB_HOST=<host do postgres>
DB_PORT=5432
DB_NAME=<database>
DB_USER=<usuario>
DB_PASSWORD=<senha>
JWT_SECRET=<segredo forte>
ADMIN_EMAIL=<email inicial>
ADMIN_PASSWORD=<senha inicial>
CORS_ORIGINS=<url do frontend>
```

Para o frontend, publique `frontend/Dockerfile` ou gere `npm run build` e hospede o conteudo de `dist/frontend/browser`.

## Funcionalidades implementadas nesta base

- Login com JWT e senha criptografada por PBKDF2.
- Usuario administrador inicial por seed de startup.
- CRUDs REST para alunos, professores, cursos, disciplinas, turmas, vinculos, planos de ensino, aulas e historicos.
- Matricula com criacao automatica de historico escolar.
- Lancamento de notas com calculo automatico de media e atualizacao do historico.
- Frequencia por aula com consolidacao percentual no historico.
- Encerramento de disciplina com lista de pendencias.
- Dashboard e endpoints de relatorios.
- Angular com login, layout administrativo, menu lateral, dashboard e telas principais.
