import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './dashboard.html'
})
export class DashboardPage implements OnInit {
  dados = signal<Record<string, any>>({});
  carregando = signal(true);
  erro = signal('');

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.dashboard().subscribe({
      next: dados => {
        this.dados.set(dados || {});
        this.carregando.set(false);
      },
      error: err => {
        this.erro.set(err?.error?.mensagem || 'Nao foi possivel carregar o dashboard. Entre novamente e tente outra vez.');
        this.carregando.set(false);
      }
    });
  }

  metricas() {
    return Object.entries(this.dados())
      .filter(([, valor]) => typeof valor === 'number')
      .map(([chave, valor]) => ({ chave, valor }));
  }

  graficos() {
    return Object.entries(this.dados())
      .filter(([, valor]) => valor && typeof valor === 'object' && !Array.isArray(valor))
      .map(([chave, valor]) => ({
        chave,
        itens: Object.entries(valor as Record<string, number>).map(([rotulo, total]) => ({ rotulo, total }))
      }));
  }

  label(chave: string) {
    return chave.replace(/([A-Z])/g, ' $1').replaceAll('-', ' ').trim();
  }

  maior(itens: { total: number }[]) {
    return Math.max(...itens.map(item => item.total), 1);
  }

  vazio() {
    return !this.carregando() && !this.erro() && this.metricas().length === 0 && this.graficos().length === 0;
  }
}
