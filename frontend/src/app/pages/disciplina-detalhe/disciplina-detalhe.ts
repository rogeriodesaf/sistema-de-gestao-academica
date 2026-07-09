import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';
import { PdfCardComponent } from '../../shared/ui/pdf-card/pdf-card';

@Component({
  selector: 'app-disciplina-detalhe',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, PdfCardComponent],
  templateUrl: './disciplina-detalhe.html',
  styleUrl: './disciplina-detalhe.scss'
})
export class DisciplinaDetalhePage implements OnInit {
  disciplina = signal<any | undefined>(undefined);
  mensagem = signal('');
  carregando = signal(true);
  queryParams: Record<string, any> = {};

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit() {
    this.queryParams = { ...this.route.snapshot.queryParams };
    this.carregar(this.idDaRota());
  }

  carregar(id = this.idDaRota()) {
    if (!id) {
      this.mensagem.set('Disciplina nao encontrada.');
      this.carregando.set(false);
      return;
    }
    this.carregando.set(true);
    this.mensagem.set('');
    window.setTimeout(() => {
      if (this.carregando()) {
        this.carregando.set(false);
        this.mensagem.set('Nao foi possivel carregar os dados da disciplina.');
      }
    }, 12000);
    this.api.buscarDisciplina(id).pipe(
      timeout({ each: 10000 }),
      finalize(() => this.carregando.set(false))
    ).subscribe({
      next: disciplina => {
        this.disciplina.set(disciplina);
      },
      error: err => {
        this.disciplina.set(undefined);
        this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel carregar a disciplina. Verifique sua conexao e tente novamente.');
      }
    });
  }

  private idDaRota() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) return Number(idParam);
    const [, idNaUrl] = window.location.pathname.match(/\/disciplinas\/(\d+)/) || [];
    return Number(idNaUrl);
  }

  rotuloOpcao(opcao: any) {
    if (!opcao) return '';
    const partes = [
      opcao.nome,
      opcao.codigo,
      opcao.ano,
      opcao.disciplina?.nome,
      opcao.modulo?.nome,
      opcao.turma?.nome,
      opcao.periodoLetivo?.nome
    ].filter(Boolean);
    return partes.length ? partes.join(' - ') : `Registro ${opcao.id}`;
  }

  tipoDisciplina() {
    return (this.disciplina()?.nome || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().includes('optativa')
      ? 'Optativa'
      : 'Obrigatoria';
  }

  enviarEmenta(arquivo: File) {
    const disciplina = this.disciplina();
    if (!disciplina) return;
    if (arquivo.type !== 'application/pdf') {
      this.mensagem.set('Envie um arquivo PDF');
      return;
    }
    this.api.enviarArquivo('disciplinas', disciplina.id, 'ementa-pdf', arquivo).subscribe({
      next: () => {
        this.mensagem.set('Ementa enviada com sucesso');
        this.carregar(disciplina.id);
      },
      error: err => this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel enviar a ementa')
    });
  }

  removerEmenta() {
    const disciplina = this.disciplina();
    if (!disciplina) return;
    this.api.removerArquivo('disciplinas', disciplina.id, 'ementa-pdf').subscribe({
      next: () => {
        this.mensagem.set('Ementa removida');
        this.carregar(disciplina.id);
      },
      error: err => this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel remover a ementa')
    });
  }

  abrirEmenta() {
    this.baixarOuAbrir(false);
  }

  baixarEmenta() {
    this.baixarOuAbrir(true);
  }

  private baixarOuAbrir(download: boolean) {
    const disciplina = this.disciplina();
    if (!disciplina) return;
    this.api.baixarArquivo('disciplinas', disciplina.id, 'ementa-pdf').subscribe({
      next: arquivo => {
        const url = URL.createObjectURL(new Blob([arquivo], { type: 'application/pdf' }));
        if (download) {
          const link = document.createElement('a');
          link.href = url;
          link.download = disciplina.ementaPdfNome || 'ementa.pdf';
          link.click();
          URL.revokeObjectURL(url);
          return;
        }
        window.open(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: err => this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel acessar a ementa')
    });
  }
}
