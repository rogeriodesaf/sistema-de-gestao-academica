import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-turma-detalhe',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './turma-detalhe.html',
  styleUrl: './turma-detalhe.scss'
})
export class TurmaDetalhePage implements OnInit {
  turma = signal<any | undefined>(undefined);
  matriculas = signal<any[]>([]);
  carregando = signal(true);
  mensagem = signal('');

  constructor(private route: ActivatedRoute, private router: Router, private api: ApiService) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.mensagem.set('Turma nao encontrada.');
      this.carregando.set(false);
      return;
    }

    forkJoin({
      turma: this.api.buscar('turmas', id),
      matriculas: this.api.listar('matriculas-disciplinas')
    }).subscribe({
      next: dados => {
        this.turma.set(dados.turma);
        this.matriculas.set((dados.matriculas || []).filter(item => item.ofertaDisciplina?.turma?.id === id));
        this.carregando.set(false);
      },
      error: err => {
        this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel carregar os dados da turma.');
        this.carregando.set(false);
      }
    });
  }

  alunosAtivos() {
    return this.matriculas().filter(item => !['CANCELADA', 'TRANCADA'].includes(item.status));
  }

  ocupacao() {
    const total = this.turma()?.quantidadeMaximaAlunos || 0;
    return `${this.alunosAtivos().length}/${total}`;
  }

  capacidade() {
    return this.turma()?.quantidadeMaximaAlunos || 0;
  }

  matriculados() {
    return this.alunosAtivos().length;
  }

  vagasDisponiveis() {
    return Math.max(this.capacidade() - this.matriculados(), 0);
  }

  statusLabel(status: string) {
    const labels: Record<string, string> = {
      ABERTA: 'Aberta',
      EM_ANDAMENTO: 'Em andamento',
      ENCERRADA: 'Encerrada',
      CANCELADA: 'Cancelada',
      PLANEJADA: 'Planejada',
      CONCLUIDA: 'Concluida'
    };
    return labels[status] || status || 'Nao informada';
  }

  voltar() {
    this.router.navigate(['/turmas']);
  }

  editar() {
    this.router.navigate(['/turmas'], { queryParams: { editar: this.turma()?.id } });
  }

  verMatriculas() {
    this.router.navigate(['/matriculas-disciplinas'], { queryParams: { turmaId: this.turma()?.id } });
  }

  abrirDiario() {
    this.mensagem.set('Diario da turma sera integrado a notas e frequencia em uma etapa futura.');
  }

  encerrar() {
    const turma = this.turma();
    if (!turma || !confirm('Deseja encerrar esta turma? Todas as ofertas, diários e homologações devem estar concluídos.')) return;
    this.api.atualizar('turmas', turma.id, { ...turma, status: 'ENCERRADA' }).subscribe({
      next: () => {
        this.turma.set({ ...turma, status: 'ENCERRADA' });
        this.mensagem.set('Turma encerrada.');
      },
      error: err => this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel encerrar a turma.')
    });
  }
}
