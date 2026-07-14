import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemeService } from '../../../core/theme.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './topbar.html',
  styleUrl: './topbar.scss'
})
export class TopbarComponent {
  @Input() usuario: any;
  @Output() abrirMenu = new EventEmitter<void>();
  @Output() sair = new EventEmitter<void>();

  constructor(public tema: ThemeService) {}

  get perfilDescricao(): string {
    const descricoes: Record<string, string> = {
      ADMINISTRADOR: 'Administrador',
      COORDENADOR: 'Coordenador',
      SECRETARIA: 'Secretaria',
      PROFESSOR: 'Professor',
      ALUNO: 'Aluno'
    };

    return descricoes[this.usuario?.perfil] || 'Usuario';
  }
}
