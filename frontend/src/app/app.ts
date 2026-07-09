import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  menuGrupos = [
    { titulo: 'Dashboard', itens: [{ path: '/dashboard', label: 'Dashboard', icone: '⌂' }] },
    { titulo: 'Cadastros', itens: [
      { path: '/alunos', label: 'Alunos', icone: '◉' },
      { path: '/professores', label: 'Professores', icone: '◆' },
      { path: '/cursos', label: 'Cursos', icone: '▣' },
      { path: '/disciplinas', label: 'Disciplinas', icone: '▤' },
      { path: '/modulos', label: 'Modulos', icone: '▧' }
    ] },
    { titulo: 'Organizacao Academica', itens: [
      { path: '/anos-letivos', label: 'Anos letivos', icone: '□' },
      { path: '/periodos-letivos', label: 'Periodos letivos', icone: '◫' },
      { path: '/turmas', label: 'Turmas', icone: '▦' },
      { path: '/matriz-curricular', label: 'Matriz curricular', icone: '▥' },
      { path: '/montagem-periodo', label: 'Montagem do periodo', icone: '◇' },
      { path: '/ofertas-disciplinas', label: 'Ofertas de disciplinas', icone: '◈' }
    ] },
    { titulo: 'Academico', itens: [
      { path: '/matriculas-disciplinas', label: 'Matriculas em disciplinas', icone: '✓' },
      { path: '/planos-ensino', label: 'Plano de ensino', icone: '✎' },
      { path: '/aulas', label: 'Aulas', icone: '◌' },
      { path: '/frequencias', label: 'Frequencia', icone: '▢' },
      { path: '/notas', label: 'Notas', icone: '●' }
    ] },
    { titulo: 'Consultas', itens: [
      { path: '/historicos', label: 'Historico escolar', icone: '◎' },
      { path: '/relatorios', label: 'Relatorios', icone: '◧' }
    ] },
    { titulo: 'Administracao', itens: [
      { path: '/usuarios', label: 'Usuarios', icone: '◍' },
      { path: '/perfis', label: 'Perfis', icone: '◐' },
      { path: '/configuracoes', label: 'Configuracoes', icone: '⚙' }
    ] }
  ];

  constructor(public auth: AuthService) {}
}
