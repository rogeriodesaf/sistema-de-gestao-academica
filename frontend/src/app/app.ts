import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { AppLayoutComponent } from './shared/ui/app-layout/app-layout';
import { MenuGrupo } from './shared/ui/sidebar/sidebar';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, AppLayoutComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  menuGrupos: MenuGrupo[] = [
    { titulo: 'Principal', itens: [{ path: '/dashboard', label: 'Dashboard', icone: 'dashboard' }] },
    { titulo: 'Academico', itens: [
      { path: '/cursos', label: 'Cursos', icone: 'course' },
      { path: '/matriz-curricular', label: 'Matriz curricular', icone: 'matrix' },
      { path: '/disciplinas', label: 'Disciplinas', icone: 'book' },
      { path: '/modulos', label: 'Modulos', icone: 'layers' },
      { path: '/montagem-periodo', label: 'Oferta por modulo', icone: 'calendar' },
      { path: '/ofertas-disciplinas', label: 'Ofertas de disciplinas', icone: 'checklist' },
      { path: '/turmas', label: 'Turmas', icone: 'users' },
      { path: '/anos-letivos', label: 'Anos letivos', icone: 'calendar' },
      { path: '/planos-ensino', label: 'Planos de ensino', icone: 'plan' },
      { path: '/aulas', label: 'Aulas', icone: 'lesson' }
    ] },
    { titulo: 'Pessoas', itens: [
      { path: '/alunos', label: 'Alunos', icone: 'student' },
      { path: '/professores', label: 'Professores', icone: 'teacher' }
    ] },
    { titulo: 'Avaliacao', itens: [
      { path: '/matriculas-disciplinas', label: 'Matriculas', icone: 'document-check' },
      { path: '/frequencias', label: 'Frequencia', icone: 'attendance' },
      { path: '/notas', label: 'Notas', icone: 'grade' }
    ] },
    { titulo: 'Consultas', itens: [
      { path: '/historicos', label: 'Historico escolar', icone: 'history' },
      { path: '/relatorios', label: 'Relatorios', icone: 'chart' }
    ] },
    { titulo: 'Administracao', itens: [
      { path: '/usuarios', label: 'Usuarios', icone: 'user' },
      { path: '/perfis', label: 'Perfis', icone: 'shield' },
      { path: '/configuracoes', label: 'Configuracoes', icone: 'settings' }
    ] }
  ];

  constructor(public auth: AuthService) {}
}
