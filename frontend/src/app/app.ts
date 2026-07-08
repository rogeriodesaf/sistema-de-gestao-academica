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
  menu = [
    { path: '/dashboard', label: 'Dashboard' },
    { path: '/alunos', label: 'Alunos' },
    { path: '/professores', label: 'Professores' },
    { path: '/cursos', label: 'Cursos' },
    { path: '/disciplinas', label: 'Disciplinas' },
    { path: '/turmas', label: 'Turmas' },
    { path: '/matriculas', label: 'Matriculas' },
    { path: '/planos-ensino', label: 'Plano de ensino' },
    { path: '/aulas', label: 'Aulas' },
    { path: '/frequencias', label: 'Frequencia' },
    { path: '/notas', label: 'Notas' },
    { path: '/historicos', label: 'Historico' },
    { path: '/relatorios', label: 'Relatorios' }
  ];

  constructor(public auth: AuthService) {}
}
