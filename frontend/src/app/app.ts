import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AuthService } from './core/auth.service';
import { AtualizacaoAppService } from './core/atualizacao-app.service';
import { AppLayoutComponent } from './shared/ui/app-layout/app-layout';
import { MenuGrupo } from './shared/ui/sidebar/sidebar';

@Component({
  selector: 'app-root',
  imports: [CommonModule, AppLayoutComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnDestroy {
  private readonly navegacaoSubscription: Subscription;
  private readonly menuProfessor: MenuGrupo[] = [
    { titulo: 'Professor', itens: [{ path: '/area-professor', label: 'Area do professor', icone: 'teacher' }] }
  ];
  private readonly menuAluno: MenuGrupo[] = [
    { titulo: 'Aluno', itens: [{ path: '/area-aluno', label: 'Portal do aluno', icone: 'student' }] }
  ];
  private readonly menuCompleto: MenuGrupo[] = [
    { titulo: 'Principal', itens: [{ path: '/dashboard', label: 'Dashboard', icone: 'dashboard' }] },
    { titulo: 'Academico', itens: [
      { path: '/cursos', label: 'Cursos', icone: 'course' },
      { path: '/matriz-curricular', label: 'Matriz curricular', icone: 'matrix' },
      { path: '/disciplinas', label: 'Disciplinas', icone: 'book' },
      { path: '/modulos', label: 'Modulos', icone: 'layers' },
      { path: '/turmas', label: 'Turmas Acadêmicas', icone: 'users' },
      { path: '/montagem-periodo', label: 'Oferta por modulo', icone: 'calendar' },
      { path: '/ofertas-disciplinas', label: 'Ofertas de disciplinas', icone: 'checklist' },
      { path: '/anos-letivos', label: 'Anos letivos', icone: 'calendar' },
      { path: '/planos-ensino', label: 'Plano de Ensino', icone: 'plan' },
      { path: '/aulas', label: 'Consulta de Aulas', icone: 'lesson' }
    ] },
    { titulo: 'Pessoas', itens: [
      { path: '/alunos', label: 'Alunos', icone: 'student' },
      { path: '/professores', label: 'Professores', icone: 'teacher' }
    ] },
    { titulo: 'Avaliacao', itens: [
      { path: '/diarios-pendentes', label: 'Diarios pendentes', icone: 'checklist' },
      { path: '/matriculas-disciplinas', label: 'Matrículas em Disciplinas', icone: 'document-check' }
    ] },
    { titulo: 'Consultas', itens: [
      { path: '/historicos', label: 'Historico escolar', icone: 'history' },
      { path: '/relatorios', label: 'Relatorios', icone: 'chart' }
    ] },
    { titulo: 'Administracao', itens: [
      { path: '/usuarios', label: 'Usuarios', icone: 'user' },
      { path: '/perfis', label: 'Perfis', icone: 'shield' },
      { path: '/auditoria', label: 'Auditoria', icone: 'history' },
      { path: '/configuracoes', label: 'Configuracoes', icone: 'settings' }
    ] }
  ];
  private readonly menuCoordenador: MenuGrupo[] = this.menuCompleto.map(grupo => ({
    ...grupo,
    itens: grupo.itens.filter(item => !['/usuarios', '/perfis', '/auditoria'].includes(item.path))
  })).filter(grupo => grupo.itens.length);
  private readonly menuGestaoSemDiarios: MenuGrupo[] = this.menuCompleto.map(grupo => ({
    ...grupo,
    itens: grupo.itens.filter(item => item.path !== '/diarios-pendentes')
  })).filter(grupo => grupo.itens.length);

  constructor(
    public auth: AuthService,
    public atualizacaoApp: AtualizacaoAppService,
    private router: Router
  ) {
    this.navegacaoSubscription = this.router.events
      .pipe(filter(evento => evento instanceof NavigationEnd))
      .subscribe(() => this.redirecionarPerfil());
  }

  ngOnDestroy() {
    this.navegacaoSubscription.unsubscribe();
  }

  get menuGrupos(): MenuGrupo[] {
    if (this.auth.usuario()?.perfil === 'PROFESSOR') {
      return this.menuProfessor;
    }
    if (this.auth.usuario()?.perfil === 'ALUNO') {
      return this.menuAluno;
    }

    const perfil = this.auth.usuario()?.perfil;
    if (perfil === 'ADMINISTRADOR') return this.menuCompleto;
    if (perfil === 'COORDENADOR') return this.menuCoordenador;
    return this.menuGestaoSemDiarios;
  }

  private redirecionarPerfil() {
    if (this.router.url.startsWith('/login')) {
      return;
    }
    if (this.auth.usuario()?.perfil === 'PROFESSOR' && !this.router.url.startsWith('/area-professor')) {
      this.router.navigateByUrl('/area-professor');
    }
    if (this.auth.usuario()?.perfil === 'ALUNO' && !this.router.url.startsWith('/area-aluno')) {
      this.router.navigateByUrl('/area-aluno');
    }
  }
}
