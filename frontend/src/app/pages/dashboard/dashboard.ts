import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';
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

  constructor(private api: ApiService, private changeDetector: ChangeDetectorRef) {}

  ngOnInit() {
    this.api.dashboard().pipe(
      finalize(() => {
        this.carregando.set(false);
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: dados => {
        this.dados.set(dados || {});
      },
      error: err => {
        this.erro.set(err?.error?.mensagem || 'Não foi possível carregar o dashboard. Tente novamente.');
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
        itens: Object.entries(valor as Record<string, number>).map(([rotulo, total]) => ({
          rotulo: this.enumTexto(rotulo),
          total
        }))
      }));
  }

  label(chave: string) {
    const labels: Record<string, string> = {
      alunos: 'Alunos',
      alunosAtivos: 'Alunos ativos',
      professores: 'Professores',
      professoresAtivos: 'Professores ativos',
      cursos: 'Cursos',
      cursosAtivos: 'Cursos ativos',
      disciplinas: 'Disciplinas',
      ofertas: 'Ofertas de disciplinas',
      matriculas: 'Matrículas',
      matriculasAtivas: 'Matrículas ativas',
      turmas: 'Turmas acadêmicas',
      ofertasPorStatus: 'Ofertas por situação',
      matriculasPorStatus: 'Matrículas por situação'
    };
    return labels[chave] || chave.replace(/([A-Z])/g, ' $1').replaceAll('-', ' ').trim();
  }

  enumTexto(valor: string) {
    const textos: Record<string, string> = {
      PLANEJADA: 'Planejada', ABERTA: 'Aberta', EM_ANDAMENTO: 'Em andamento',
      ENCERRADA: 'Encerrada', CANCELADA: 'Cancelada', ATIVA: 'Ativa',
      INATIVA: 'Inativa', TRANCADA: 'Trancada', CONCLUIDA: 'Concluída'
    };
    return textos[valor] || valor.replaceAll('_', ' ').toLowerCase().replace(/^./, letra => letra.toUpperCase());
  }

  maior(itens: { total: number }[]) {
    return Math.max(...itens.map(item => item.total), 1);
  }

  vazio() {
    return !this.carregando() && !this.erro() && this.metricas().length === 0 && this.graficos().length === 0;
  }
}
