import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-oferta-detalhe',
  standalone: true,
  imports: [CommonModule, PageHeaderComponent],
  templateUrl: './oferta-detalhe.html',
  styleUrl: './oferta-detalhe.scss'
})
export class OfertaDetalhePage implements OnInit {
  oferta = signal<any | undefined>(undefined);
  matriculas = signal<any[]>([]);
  mensagem = signal('');
  carregando = signal(true);

  constructor(private route: ActivatedRoute, private router: Router, private api: ApiService) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.mensagem.set('Oferta nao encontrada.');
      this.carregando.set(false);
      return;
    }

    forkJoin({
      oferta: this.api.buscar('ofertas-disciplinas', id),
      matriculas: this.api.listar('matriculas-disciplinas')
    }).subscribe({
      next: dados => {
        this.oferta.set(dados.oferta);
        this.matriculas.set((dados.matriculas || []).filter(item => item.ofertaDisciplina?.id === id));
        this.carregando.set(false);
      },
      error: err => {
        this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel carregar a oferta academica.');
        this.carregando.set(false);
      }
    });
  }

  moduloOriginal() {
    const disciplina = this.oferta()?.disciplina;
    return disciplina?.moduloOriginal || disciplina?.modulo;
  }

  alunosAtivos() {
    return this.matriculas().filter(item => !['CANCELADA', 'TRANCADA'].includes(item.status));
  }

  ocupacao() {
    const oferta = this.oferta();
    return `${this.alunosAtivos().length}/${oferta?.vagas || oferta?.turma?.quantidadeMaximaAlunos || 0}`;
  }

  voltar() {
    this.router.navigate(['/montagem-periodo']);
  }

  editar() {
    this.router.navigate(['/montagem-periodo'], { queryParams: { editar: this.oferta()?.id } });
  }

  encerrar() {
    const oferta = this.oferta();
    if (!oferta || !confirm('Deseja encerrar esta oferta?')) return;
    this.api.atualizar('ofertas-disciplinas', oferta.id, { ...oferta, status: 'ENCERRADA' }).subscribe({
      next: () => {
        this.mensagem.set('Oferta encerrada.');
        this.oferta.set({ ...oferta, status: 'ENCERRADA' });
      },
      error: err => this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel encerrar a oferta.')
    });
  }
}
