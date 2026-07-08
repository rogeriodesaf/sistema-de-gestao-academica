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
    { titulo: 'Dashboard', itens: [{ path: '/dashboard', label: 'Dashboard' }] },
    { titulo: 'Cadastros', itens: [
      { path: '/alunos', label: 'Alunos' },
      { path: '/professores', label: 'Professores' },
      { path: '/cursos', label: 'Cursos' },
      { path: '/disciplinas', label: 'Disciplinas' },
      { path: '/modulos', label: 'Modulos' }
    ] },
    { titulo: 'Organizacao Academica', itens: [
      { path: '/anos-letivos', label: 'Anos letivos' },
      { path: '/periodos-letivos', label: 'Periodos letivos' },
      { path: '/turmas', label: 'Turmas' },
      { path: '/matriz-curricular', label: 'Matriz curricular' },
      { path: '/montagem-periodo', label: 'Montagem do periodo' },
      { path: '/ofertas-disciplinas', label: 'Ofertas de disciplinas' }
    ] },
    { titulo: 'Academico', itens: [
      { path: '/matriculas-disciplinas', label: 'Matriculas em disciplinas' },
      { path: '/planos-ensino', label: 'Plano de ensino' },
      { path: '/aulas', label: 'Aulas' },
      { path: '/frequencias', label: 'Frequencia' },
      { path: '/notas', label: 'Notas' }
    ] },
    { titulo: 'Consultas', itens: [
      { path: '/historicos', label: 'Historico escolar' },
      { path: '/relatorios', label: 'Relatorios' }
    ] },
    { titulo: 'Administracao', itens: [
      { path: '/usuarios', label: 'Usuarios' },
      { path: '/perfis', label: 'Perfis' },
      { path: '/configuracoes', label: 'Configuracoes' }
    ] }
  ];

  constructor(public auth: AuthService) {}
}
