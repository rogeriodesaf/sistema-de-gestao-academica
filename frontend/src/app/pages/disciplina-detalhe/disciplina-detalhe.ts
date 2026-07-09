import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
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
  disciplina?: any;
  mensagem = '';
  carregando = true;
  queryParams: Record<string, any> = {};

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit() {
    this.queryParams = { ...this.route.snapshot.queryParams };
    this.carregar(this.idDaRota());
  }

  carregar(id = this.idDaRota()) {
    if (!id) {
      this.mensagem = 'Disciplina nao encontrada.';
      this.carregando = false;
      return;
    }
    this.carregando = true;
    this.mensagem = '';
    window.setTimeout(() => {
      if (this.carregando) {
        this.carregando = false;
        this.mensagem = 'Nao foi possivel carregar os dados da disciplina.';
      }
    }, 12000);
    this.api.buscarDisciplina(id).pipe(
      timeout({ each: 10000 }),
      finalize(() => this.carregando = false)
    ).subscribe({
      next: disciplina => {
        this.disciplina = disciplina;
      },
      error: err => {
        this.disciplina = undefined;
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel carregar a disciplina. Verifique sua conexao e tente novamente.';
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
    return (this.disciplina?.nome || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().includes('optativa')
      ? 'Optativa'
      : 'Obrigatoria';
  }

  enviarEmenta(arquivo: File) {
    if (!this.disciplina) return;
    if (arquivo.type !== 'application/pdf') {
      this.mensagem = 'Envie um arquivo PDF';
      return;
    }
    this.api.enviarArquivo('disciplinas', this.disciplina.id, 'ementa-pdf', arquivo).subscribe({
      next: () => {
        this.mensagem = 'Ementa enviada com sucesso';
        this.carregar(this.disciplina.id);
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel enviar a ementa'
    });
  }

  removerEmenta() {
    if (!this.disciplina) return;
    this.api.removerArquivo('disciplinas', this.disciplina.id, 'ementa-pdf').subscribe({
      next: () => {
        this.mensagem = 'Ementa removida';
        this.carregar(this.disciplina.id);
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel remover a ementa'
    });
  }

  abrirEmenta() {
    this.baixarOuAbrir(false);
  }

  baixarEmenta() {
    this.baixarOuAbrir(true);
  }

  private baixarOuAbrir(download: boolean) {
    if (!this.disciplina) return;
    this.api.baixarArquivo('disciplinas', this.disciplina.id, 'ementa-pdf').subscribe({
      next: arquivo => {
        const url = URL.createObjectURL(new Blob([arquivo], { type: 'application/pdf' }));
        if (download) {
          const link = document.createElement('a');
          link.href = url;
          link.download = this.disciplina.ementaPdfNome || 'ementa.pdf';
          link.click();
          URL.revokeObjectURL(url);
          return;
        }
        window.open(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel acessar a ementa'
    });
  }
}
