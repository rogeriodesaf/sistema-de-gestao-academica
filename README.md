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
- Modelo academico flexivel para seminarios e outras instituicoes: Curso -> Turma -> Ano Letivo -> Periodo Letivo -> Oferta de Disciplina -> Matricula em Disciplina -> Historico Escolar.
- Turmas com descricao, turno, quantidade maxima de alunos e status de planejamento/abertura/andamento/encerramento.
- Cadastro de anos letivos e periodos letivos, permitindo modulos, semestres, bimestres, trimestres ou outro formato.
- Ofertas de disciplinas por turma, ano letivo e periodo, com professor, vagas, carga horaria e status.
- Matricula em disciplina com controle de vagas e criacao automatica de historico escolar.
- Matricula antiga por turma/disciplina mantida por compatibilidade.
- Lancamento de notas com calculo automatico de media e atualizacao do historico.
- Frequencia por aula com consolidacao percentual no historico.
- Encerramento de disciplina com lista de pendencias.
- Dashboard com ano/periodo atual, turmas ativas, ofertas abertas, disciplinas encerradas, alunos matriculados e vagas disponiveis.
- Relatorios de alunos por turma, alunos por disciplina, disciplinas por periodo, carga horaria ministrada, historico, notas e frequencia.
- Angular com login, layout administrativo, menu lateral, dashboard e telas principais.

## Rotas academicas principais

- `GET/POST /api/turmas`
- `GET/POST /api/anos-letivos`
- `GET/POST /api/periodos-letivos`
- `GET/POST /api/ofertas-disciplinas`
- `GET/POST /api/matriculas-disciplinas`
- `GET /api/relatorios/alunos-por-turma?turmaId=1`
- `GET /api/relatorios/alunos-por-disciplina?ofertaDisciplinaId=1`
- `GET /api/relatorios/disciplinas-por-periodo?periodoLetivoId=1`
- `GET /api/relatorios/carga-horaria-ministrada?ofertaDisciplinaId=1`
