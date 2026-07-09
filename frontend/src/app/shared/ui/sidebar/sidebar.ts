import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MenuIconComponent } from '../menu-icon/menu-icon';

export interface MenuItem {
  path: string;
  label: string;
  icone: string;
}

export interface MenuGrupo {
  titulo: string;
  itens: MenuItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, MenuIconComponent],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class SidebarComponent {
  @Input() grupos: MenuGrupo[] = [];
  @Input() mobile = false;
  @Output() navegar = new EventEmitter<void>();
}
