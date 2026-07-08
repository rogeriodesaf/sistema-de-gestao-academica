import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { CadastroPage } from './pages/cadastro/cadastro';
import { DashboardPage } from './pages/dashboard/dashboard';
import { LoginPage } from './pages/login/login';

const camposBase = {
  alunos: ['nome', 'cpf', 'email', 'telefone', 'dataNascimento', 'endereco', 'status', 'dataIngresso', 'observacoes'],
  professores: ['nome', 'cpf', 'email', 'telefone', 'formacao', 'ativo'],
  cursos: ['nome', 'descricao', 'cargaHorariaTotal', 'ativo'],
  disciplinas: ['nome', 'codigo', 'cargaHoraria', 'ementa', 'bibliografia', 'ativo'],
  turmas: ['nome', 'anoPeriodo', 'dataInicio', 'dataTermino', 'status'],
  matriculas: ['aluno.id', 'turma.id', 'disciplina.id'],
  'planos-ensino': ['disciplina.id', 'turma.id', 'objetivos', 'ementa', 'conteudoProgramatico', 'metodologia', 'criteriosAvaliacao', 'bibliografiaBasica', 'bibliografiaComplementar'],
  aulas: ['disciplina.id', 'turma.id', 'professor.id', 'dataAula', 'conteudoMinistrado', 'observacoes', 'cargaHorariaAula'],
  frequencias: ['aula.id', 'aluno.id', 'presente', 'justificativa', 'observacao'],
  notas: ['aluno.id', 'disciplina.id', 'turma.id', 'nota1', 'nota2', 'trabalho', 'avaliacaoFinal'],
  historicos: ['aluno.id', 'turma.id', 'disciplina.id', 'notaFinal', 'frequenciaFinal', 'situacao', 'periodoCursado'],
  relatorios: []
};

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'dashboard', component: DashboardPage, canActivate: [authGuard] },
  ...Object.entries(camposBase).map(([path, campos]) => ({
    path,
    component: CadastroPage,
    canActivate: [authGuard],
    data: { titulo: path.replace('-', ' '), endpoint: path, campos }
  })),
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' }
];
