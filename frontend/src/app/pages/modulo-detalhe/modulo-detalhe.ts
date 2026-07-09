import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-modulo-detalhe',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent],
  templateUrl: './modulo-detalhe.html',
  styleUrl: './modulo-detalhe.scss'
})
export class ModuloDetalhePage implements OnInit {
  modulo = signal<any | undefined>(undefined);
  disciplinas = signal<any[]>([]);
  mensagem = signal('');
  carregando = signal(true);

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.mensagem.set('Modulo nao encontrado.');
      this.carregando.set(false);
      return;
    }

    this.api.buscar('modulos', id).subscribe({
      next: modulo => {
        this.modulo.set(modulo);
        this.carregarDisciplinas(modulo.id);
      },
      error: err => {
        this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel carregar o modulo.');
        this.carregando.set(false);
      }
    });
  }

  carregarDisciplinas(moduloId: number) {
    this.api.listar('disciplinas').subscribe({
      next: disciplinas => {
        this.disciplinas.set((disciplinas || []).filter(disciplina => disciplina.modulo?.id === moduloId));
        this.carregando.set(false);
      },
      error: err => {
        this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel carregar as disciplinas do modulo.');
        this.carregando.set(false);
      }
    });
  }

  rotuloOpcao(opcao: any) {
    if (!opcao) return '';
    return opcao.nome || `Registro ${opcao.id}`;
  }
}
