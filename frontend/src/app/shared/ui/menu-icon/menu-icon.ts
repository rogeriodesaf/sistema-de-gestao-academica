import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-menu-icon',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './menu-icon.html',
  styleUrl: './menu-icon.scss'
})
export class MenuIconComponent {
  @Input() nome = 'circle';
}
