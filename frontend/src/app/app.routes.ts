import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { CadastroPage } from './pages/cadastro/cadastro';
import { DashboardPage } from './pages/dashboard/dashboard';
import { LoginPage } from './pages/login/login';

const camposBase = {
  alunos: ['curso.id', 'nome', 'cpf', 'email', 'telefone', 'dataNascimento', 'endereco', 'status', 'dataIngresso', 'observacoes'],
  professores: ['nome', 'cpf', 'email', 'telefone', 'formacao', 'ativo'],
  cursos: ['nome', 'descricao', 'cargaHorariaTotal', 'ativo'],
  modulos: ['curso.id', 'nome', 'descricao', 'ordem', 'ativo'],
  disciplinas: ['curso.id', 'modulo.id', 'nome', 'codigo', 'cargaHoraria', 'ementaResumo', 'ementa', 'bibliografia', 'ativo'],
  turmas: ['curso.id', 'nome', 'descricao', 'turno', 'quantidadeMaximaAlunos', 'anoPeriodo', 'dataInicio', 'dataTermino', 'status'],
  'anos-letivos': ['turma.id', 'ano', 'dataInicio', 'dataFim', 'status'],
  'periodos-letivos': ['anoLetivo.id', 'nome', 'ordem', 'tipo', 'dataInicio', 'dataFim', 'status'],
  'ofertas-disciplinas': ['turma.id', 'anoLetivo.id', 'periodoLetivo.id', 'curso.id', 'modulo.id', 'disciplina.id', 'professor.id', 'vagas', 'horario', 'sala', 'cargaHorariaPrevista', 'cargaHorariaMinistrada', 'dataInicio', 'dataFim', 'status'],
  'montagem-periodo': ['turma.id', 'anoLetivo.id', 'periodoLetivo.id', 'curso.id', 'modulo.id', 'disciplina.id', 'professor.id', 'vagas', 'horario', 'sala', 'cargaHorariaPrevista', 'dataInicio', 'dataFim', 'status'],
  'matriz-curricular': [],
  matriculas: ['aluno.id', 'turma.id', 'disciplina.id'],
  'matriculas-disciplinas': ['aluno.id', 'ofertaDisciplina.id', 'dataMatricula', 'status', 'notaFinal', 'frequenciaFinal', 'observacoes'],
  'planos-ensino': ['ofertaDisciplina.id', 'disciplina.id', 'turma.id', 'objetivos', 'ementa', 'conteudoProgramatico', 'metodologia', 'criteriosAvaliacao', 'bibliografiaBasica', 'bibliografiaComplementar', 'observacoes'],
  aulas: ['ofertaDisciplina.id', 'disciplina.id', 'turma.id', 'professor.id', 'dataAula', 'conteudoMinistrado', 'observacoes', 'cargaHorariaAula'],
  frequencias: ['aula.id', 'aluno.id', 'presente', 'justificativa', 'observacao'],
  notas: ['aluno.id', 'ofertaDisciplina.id', 'disciplina.id', 'turma.id', 'nota1', 'nota2', 'trabalho', 'avaliacaoFinal'],
  historicos: ['aluno.id', 'turma.id', 'disciplina.id', 'notaFinal', 'frequenciaFinal', 'situacao', 'periodoCursado'],
  relatorios: [],
  usuarios: [],
  perfis: [],
  configuracoes: []
};

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'dashboard', component: DashboardPage, canActivate: [authGuard] },
  ...Object.entries(camposBase).map(([path, campos]) => ({
    path,
    component: CadastroPage,
    canActivate: [authGuard],
    data: { titulo: path.replaceAll('-', ' '), endpoint: path === 'montagem-periodo' ? 'ofertas-disciplinas' : path, campos }
  })),
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' }
];
