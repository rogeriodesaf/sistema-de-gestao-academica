import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

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
}
