import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';

export type Tema = 'claro' | 'escuro';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly chave = 'sga-tema';
  tema: Tema;

  constructor(@Inject(DOCUMENT) private document: Document) {
    this.tema = localStorage.getItem(this.chave) === 'escuro' ? 'escuro' : 'claro';
    this.aplicar();
  }

  alternar() {
    this.tema = this.tema === 'claro' ? 'escuro' : 'claro';
    localStorage.setItem(this.chave, this.tema);
    this.aplicar();
  }

  private aplicar() {
    this.document.documentElement.dataset['theme'] = this.tema;
    this.document.documentElement.style.colorScheme = this.tema === 'escuro' ? 'dark' : 'light';
  }
}
