import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html'
})
export class DashboardPage implements OnInit {
  dados: Record<string, any> = {};

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.dashboard().subscribe(dados => this.dados = dados);
  }

  metricas() {
    return Object.entries(this.dados)
      .filter(([, valor]) => typeof valor === 'number')
      .map(([chave, valor]) => ({ chave, valor }));
  }

  graficos() {
    return Object.entries(this.dados)
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
}
