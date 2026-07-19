import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, HostListener, Inject, Input, OnDestroy, Output, ViewChild } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { MenuGrupo, SidebarComponent } from '../sidebar/sidebar';
import { TopbarComponent } from '../topbar/topbar';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopbarComponent],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.scss'
})
export class AppLayoutComponent implements OnDestroy {
  @ViewChild('menuMobile') menuMobile?: ElementRef<HTMLElement>;
  @Input() menuGrupos: MenuGrupo[] = [];
  @Input() usuario: any;
  @Output() sair = new EventEmitter<void>();

  menuAberto = false;
  readonly anoAtual = new Date().getFullYear();

  constructor(@Inject(DOCUMENT) private document: Document) {}

  @HostListener('document:keydown.escape')
  fecharComEsc() {
    this.fecharMenu();
  }

  ngOnDestroy() {
    this.liberarRolagem();
  }

  abrirMenu() {
    this.menuAberto = true;
    this.document.body.style.overflow = 'hidden';
    setTimeout(() => this.menuMobile?.nativeElement.focus());
  }

  fecharMenu() {
    if (!this.menuAberto) return;
    this.menuAberto = false;
    this.liberarRolagem();
  }

  emitirSair() {
    this.fecharMenu();
    this.sair.emit();
  }

  private liberarRolagem() {
    this.document.body.style.overflow = '';
  }
}
