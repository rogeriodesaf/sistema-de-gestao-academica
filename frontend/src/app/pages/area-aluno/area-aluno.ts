import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { finalize, forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';

type Aba = 'resumo' | 'frequencia' | 'avaliacoes' | 'materiais';

@Component({
  selector: 'app-area-aluno',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './area-aluno.html',
  styleUrl: './area-aluno.scss'
})
export class AreaAlunoPage implements OnInit {
  perfil: any;
  disciplinas: any[] = [];
  disciplina?: any;
  frequencia?: any;
  aulas: any[] = [];
  resultado?: any;
  arquivos: any[] = [];
  historico?: any;
  aba: Aba = 'resumo';
  carregando = false;
  carregandoDetalhe = false;
  baixandoHistorico = false;
  mensagem = '';

  constructor(private api: ApiService, private cd: ChangeDetectorRef) {}

  ngOnInit() {
    this.carregando = true;
    forkJoin({
      perfil: this.api.buscar('aluno', 'me'),
      disciplinas: this.api.listar('aluno/disciplinas')
    }).pipe(finalize(() => {
      this.carregando = false;
      this.cd.detectChanges();
    })).subscribe({
      next: dados => {
        this.perfil = dados.perfil;
        this.disciplinas = Array.isArray(dados.disciplinas) ? dados.disciplinas : [];
      },
      error: erro => this.erro(erro, 'Nao foi possivel carregar o portal do aluno.')
    });
  }

  selecionar(item: any) {
    this.limparDetalhe();
    this.carregandoDetalhe = true;
    this.api.buscar('aluno/disciplinas', item.ofertaId).pipe(finalize(() => {
      this.carregandoDetalhe = false;
      this.cd.detectChanges();
    })).subscribe({
      next: detalhe => this.disciplina = detalhe,
      error: erro => this.erro(erro, 'Nao foi possivel abrir a disciplina.')
    });
  }

  trocarAba(aba: Aba) {
    this.aba = aba;
    if (!this.disciplina || aba === 'resumo') return;
    const id = this.disciplina.ofertaId;
    this.carregandoDetalhe = true;
    const requisicao = aba === 'frequencia'
      ? forkJoin({ frequencia: this.api.buscarAcao('aluno/disciplinas', id, 'frequencia'), aulas: this.api.buscarAcao('aluno/disciplinas', id, 'aulas') })
      : aba === 'avaliacoes'
        ? this.api.buscarAcao('aluno/disciplinas', id, 'avaliacoes')
        : this.api.buscarAcao('aluno/disciplinas', id, 'arquivos');
    requisicao.pipe(finalize(() => {
      this.carregandoDetalhe = false;
      this.cd.detectChanges();
    })).subscribe({
      next: (dados: any) => {
        if (aba === 'frequencia') {
          this.frequencia = dados.frequencia;
          this.aulas = dados.aulas || [];
        } else if (aba === 'avaliacoes') {
          this.resultado = dados;
        } else {
          this.arquivos = Array.isArray(dados) ? dados : [];
        }
      },
      error: erro => this.erro(erro, 'Nao foi possivel carregar esta secao.')
    });
  }

  carregarHistorico() {
    if (this.historico) return;
    this.carregandoDetalhe = true;
    this.api.buscar('aluno', 'historico').pipe(finalize(() => {
      this.carregandoDetalhe = false;
      this.cd.detectChanges();
    })).subscribe({
      next: dados => this.historico = dados,
      error: erro => this.erro(erro, 'Nao foi possivel carregar o historico.')
    });
  }

  baixarHistorico() {
    this.baixandoHistorico = true;
    this.api.baixar('aluno/historico/pdf').pipe(finalize(() => {
      this.baixandoHistorico = false;
      this.cd.detectChanges();
    })).subscribe({
      next: blob => this.salvarBlob(blob, 'historico-escolar.pdf'),
      error: erro => this.erro(erro, 'Nao foi possivel gerar o historico em PDF.')
    });
  }

  abrirArquivo(arquivo: any, download = false) {
    const sufixo = download ? '/download' : '';
    this.api.baixar(`aluno/arquivos/${arquivo.id}${sufixo}`).subscribe({
      next: blob => {
        if (download) this.salvarBlob(blob, arquivo.nome);
        else {
          const url = URL.createObjectURL(blob);
          window.open(url, '_blank', 'noopener');
          setTimeout(() => URL.revokeObjectURL(url), 60000);
        }
      },
      error: erro => this.erro(erro, 'Nao foi possivel abrir o PDF.')
    });
  }

  periodosHistorico(): string[] {
    return this.historico ? Object.keys(this.historico.porPeriodo || {}) : [];
  }

  formatar(valor: any): string {
    return valor === null || valor === undefined || valor === '' ? '-' : String(valor);
  }

  private limparDetalhe() {
    this.disciplina = undefined;
    this.frequencia = undefined;
    this.aulas = [];
    this.resultado = undefined;
    this.arquivos = [];
    this.aba = 'resumo';
    this.mensagem = '';
  }

  private salvarBlob(blob: Blob, nome: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nome;
    link.click();
    URL.revokeObjectURL(url);
  }

  private erro(erro: any, padrao: string) {
    this.mensagem = erro?.error?.mensagem || erro?.error?.message || padrao;
  }
}
