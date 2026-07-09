import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { CadastroPage } from './pages/cadastro/cadastro';
import { DashboardPage } from './pages/dashboard/dashboard';
import { DisciplinaDetalhePage } from './pages/disciplina-detalhe/disciplina-detalhe';
import { LoginPage } from './pages/login/login';
import { ModuloDetalhePage } from './pages/modulo-detalhe/modulo-detalhe';
import { OfertaDetalhePage } from './pages/oferta-detalhe/oferta-detalhe';
import { PlanejamentoAcademicoPage } from './pages/planejamento-academico/planejamento-academico';
import { PerfisPage } from './pages/perfis/perfis';
import { TurmaDetalhePage } from './pages/turma-detalhe/turma-detalhe';
import { TurmasPage } from './pages/turmas/turmas';
import { UsuarioDetalhePage } from './pages/usuario-detalhe/usuario-detalhe';
import { UsuariosPage } from './pages/usuarios/usuarios';

const camposBase = {
  alunos: ['curso.id', 'nome', 'cpf', 'email', 'telefone', 'dataNascimento', 'endereco', 'status', 'dataIngresso', 'observacoes'],
  professores: ['nome', 'cpf', 'email', 'telefone', 'formacao', 'ativo'],
  cursos: ['nome', 'descricao', 'ativo'],
  modulos: ['curso.id', 'nome', 'descricao', 'ordem', 'status', 'ativo'],
  disciplinas: ['curso.id', 'modulo.id', 'professorResponsavel.id', 'nome', 'codigo', 'cargaHoraria', 'creditos', 'ementaResumo', 'bibliografia', 'ativo'],
  turmas: ['disciplina.id', 'professor.id', 'nome', 'anoLetivo.id', 'periodoLetivo.id', 'curso.id', 'turno', 'horario', 'sala', 'quantidadeMaximaAlunos', 'dataInicio', 'dataTermino', 'status', 'descricao'],
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
  { path: 'disciplinas/:id', component: DisciplinaDetalhePage, canActivate: [authGuard] },
  { path: 'modulos/:id', component: ModuloDetalhePage, canActivate: [authGuard] },
  { path: 'montagem-periodo', component: PlanejamentoAcademicoPage, canActivate: [authGuard] },
  { path: 'ofertas-disciplinas/:id', component: OfertaDetalhePage, canActivate: [authGuard] },
  { path: 'turmas', component: TurmasPage, canActivate: [authGuard] },
  { path: 'turmas/:id', component: TurmaDetalhePage, canActivate: [authGuard] },
  { path: 'usuarios', component: UsuariosPage, canActivate: [authGuard] },
  { path: 'usuarios/:id', component: UsuarioDetalhePage, canActivate: [authGuard] },
  { path: 'perfis', component: PerfisPage, canActivate: [authGuard] },
  ...Object.entries(camposBase).map(([path, campos]) => ({
    path,
    component: CadastroPage,
    canActivate: [authGuard],
    data: { titulo: path.replaceAll('-', ' '), endpoint: path === 'montagem-periodo' ? 'ofertas-disciplinas' : path, campos }
  })),
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' }
];
