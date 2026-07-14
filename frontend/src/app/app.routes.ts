import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { AreaProfessorPage } from './pages/area-professor/area-professor';
import { AreaAlunoPage } from './pages/area-aluno/area-aluno';
import { DiariosPendentesPage } from './pages/diarios-pendentes/diarios-pendentes';
import { ConsultaAulasPage } from './pages/consulta-aulas/consulta-aulas';
import { CadastroPage } from './pages/cadastro/cadastro';
import { DashboardPage } from './pages/dashboard/dashboard';
import { DisciplinaDetalhePage } from './pages/disciplina-detalhe/disciplina-detalhe';
import { LoginPage } from './pages/login/login';
import { ModuloDetalhePage } from './pages/modulo-detalhe/modulo-detalhe';
import { OfertaDetalhePage } from './pages/oferta-detalhe/oferta-detalhe';
import { PlanejamentoAcademicoPage } from './pages/planejamento-academico/planejamento-academico';
import { PerfisPage } from './pages/perfis/perfis';
import { UsuarioDetalhePage } from './pages/usuario-detalhe/usuario-detalhe';
import { UsuariosPage } from './pages/usuarios/usuarios';

const camposBase = {
  alunos: ['curso.id', 'nome', 'cpf', 'email', 'telefone', 'dataNascimento', 'endereco', 'status', 'dataIngresso', 'observacoes'],
  professores: ['nome', 'cpf', 'email', 'telefone', 'formacao', 'ativo'],
  cursos: ['nome', 'descricao', 'cargaHorariaTotal', 'creditosTotais', 'ativo'],
  modulos: ['anoLetivo.id', 'curso.id', 'nome', 'descricao', 'ordem', 'dataInicio', 'dataFim', 'status', 'ativo'],
  disciplinas: ['curso.id', 'modulo.id', 'professorResponsavel.id', 'nome', 'codigo', 'cargaHoraria', 'creditos', 'ementaResumo', 'bibliografia', 'ativo'],
  'anos-letivos': ['ano', 'dataInicio', 'dataFim', 'status'],
  'periodos-letivos': ['anoLetivo.id', 'nome', 'ordem', 'tipo', 'dataInicio', 'dataFim', 'status'],
  'ofertas-disciplinas': ['turma.id', 'anoLetivo.id', 'curso.id', 'modulo.id', 'disciplina.id', 'professor.id', 'vagas', 'horario', 'sala', 'cargaHorariaPrevista', 'cargaHorariaMinistrada', 'dataInicio', 'dataFim', 'status'],
  'montagem-periodo': ['turma.id', 'anoLetivo.id', 'periodoLetivo.id', 'curso.id', 'modulo.id', 'disciplina.id', 'professor.id', 'vagas', 'horario', 'sala', 'cargaHorariaPrevista', 'dataInicio', 'dataFim', 'status'],
  'matriz-curricular': [],
  matriculas: ['aluno.id', 'turma.id', 'disciplina.id'],
  'matriculas-disciplinas': ['aluno.id', 'ofertaDisciplina.id', 'dataMatricula', 'status', 'observacoes'],
  'planos-ensino': ['disciplina.id', 'objetivos', 'conteudoProgramatico', 'metodologia', 'bibliografiaBasica', 'bibliografiaComplementar', 'observacoes'],
  historicos: ['aluno.id', 'turma.id', 'disciplina.id', 'notaFinal', 'frequenciaFinal', 'situacao', 'periodoCursado'],
  relatorios: [],
  usuarios: [],
  perfis: [],
  configuracoes: []
};

const perfisGestao = ['ADMINISTRADOR', 'COORDENADOR', 'SECRETARIA'];
const perfisAdministracao = ['ADMINISTRADOR'];
const perfisProfessor = ['PROFESSOR'];
const perfisAluno = ['ALUNO'];
const perfisCoordenacao = ['ADMINISTRADOR', 'COORDENADOR'];

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'dashboard', component: DashboardPage, canActivate: [authGuard], data: { perfis: perfisGestao } },
  { path: 'area-professor', component: AreaProfessorPage, canActivate: [authGuard], data: { perfis: perfisProfessor } },
  { path: 'area-aluno', component: AreaAlunoPage, canActivate: [authGuard], data: { perfis: perfisAluno } },
  { path: 'diarios-pendentes', component: DiariosPendentesPage, canActivate: [authGuard], data: { perfis: perfisCoordenacao } },
  { path: 'aulas', component: ConsultaAulasPage, canActivate: [authGuard], data: { perfis: perfisCoordenacao } },
  { path: 'disciplinas/:id', component: DisciplinaDetalhePage, canActivate: [authGuard], data: { perfis: perfisGestao } },
  { path: 'modulos/:id', component: ModuloDetalhePage, canActivate: [authGuard], data: { perfis: perfisGestao } },
  { path: 'montagem-periodo', component: PlanejamentoAcademicoPage, canActivate: [authGuard], data: { perfis: perfisGestao } },
  { path: 'ofertas-disciplinas/:id', component: OfertaDetalhePage, canActivate: [authGuard], data: { perfis: perfisGestao } },
  { path: 'usuarios', component: UsuariosPage, canActivate: [authGuard], data: { perfis: perfisAdministracao } },
  { path: 'usuarios/:id', component: UsuarioDetalhePage, canActivate: [authGuard], data: { perfis: perfisAdministracao } },
  { path: 'perfis', component: PerfisPage, canActivate: [authGuard], data: { perfis: perfisAdministracao } },
  ...Object.entries(camposBase).map(([path, campos]) => ({
    path,
    component: CadastroPage,
    canActivate: [authGuard],
    data: { titulo: path.replaceAll('-', ' '), endpoint: path === 'montagem-periodo' ? 'ofertas-disciplinas' : path, campos, perfis: perfisGestao }
  })),
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' }
];
