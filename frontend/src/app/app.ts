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
    { titulo: 'Principal', itens: [{ path: '/dashboard', label: 'Dashboard', icone: 'D' }] },
    { titulo: 'Academico', itens: [
      { path: '/cursos', label: 'Cursos', icone: 'C' },
      { path: '/disciplinas', label: 'Disciplinas', icone: 'D' },
      { path: '/modulos', label: 'Modulos', icone: 'M' },
      { path: '/turmas', label: 'Turmas', icone: 'T' },
      { path: '/matriz-curricular', label: 'Matriz curricular', icone: 'G' },
      { path: '/ofertas-disciplinas', label: 'Ofertas de disciplinas', icone: 'O' },
      { path: '/montagem-periodo', label: 'Montagem do periodo', icone: 'P' }
    ] },
    { titulo: 'Pessoas', itens: [
      { path: '/alunos', label: 'Alunos', icone: 'A' },
      { path: '/professores', label: 'Professores', icone: 'P' }
    ] },
    { titulo: 'Organizacao Academica', itens: [
      { path: '/anos-letivos', label: 'Anos letivos', icone: 'A' },
      { path: '/periodos-letivos', label: 'Periodos letivos', icone: 'P' },
      { path: '/matriculas-disciplinas', label: 'Matriculas em disciplinas', icone: 'M' },
      { path: '/planos-ensino', label: 'Plano de ensino', icone: 'E' },
      { path: '/aulas', label: 'Aulas', icone: 'L' },
      { path: '/frequencias', label: 'Frequencia', icone: 'F' },
      { path: '/notas', label: 'Notas', icone: 'N' }
    ] },
    { titulo: 'Consultas', itens: [
      { path: '/historicos', label: 'Historico escolar', icone: 'H' },
      { path: '/relatorios', label: 'Relatorios', icone: 'R' }
    ] },
    { titulo: 'Administracao', itens: [
      { path: '/usuarios', label: 'Usuarios', icone: 'U' },
      { path: '/perfis', label: 'Perfis', icone: 'P' },
      { path: '/configuracoes', label: 'Configuracoes', icone: 'S' }
    ] }
  ];

  constructor(public auth: AuthService) {}
}
