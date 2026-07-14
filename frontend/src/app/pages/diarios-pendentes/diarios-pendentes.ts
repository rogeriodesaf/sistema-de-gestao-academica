import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-diarios-pendentes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './diarios-pendentes.html',
  styleUrl: './diarios-pendentes.scss'
})
export class DiariosPendentesPage implements OnInit {
  diarios: any[] = [];
  diario?: any;
  motivo = '';
  mensagem = '';
  tipoMensagem: 'sucesso' | 'erro' = 'sucesso';
  carregando = false;
  processando = false;

  constructor(private api: ApiService, private cd: ChangeDetectorRef) {}

  ngOnInit() { this.carregar(); }

  carregar() {
    this.carregando = true;
    this.api.listar('coordenador/diarios-pendentes').pipe(finalize(() => {
      this.carregando = false;
      this.cd.detectChanges();
    })).subscribe({
      next: dados => this.diarios = Array.isArray(dados) ? dados : [],
      error: erro => this.exibirErro(erro, 'Nao foi possivel carregar os diarios pendentes.')
    });
  }

  visualizar(item: any) {
    this.carregando = true;
    this.motivo = '';
    this.api.buscarAcao('coordenador/ofertas', item.ofertaId, 'diario').pipe(finalize(() => {
      this.carregando = false;
      this.cd.detectChanges();
    })).subscribe({
      next: diario => this.diario = diario,
      error: erro => this.exibirErro(erro, 'Nao foi possivel abrir o diario.')
    });
  }

  homologar() {
    if (!this.diario?.oferta?.ofertaId) return;
    this.processando = true;
    this.api.salvar(`coordenador/ofertas/${this.diario.oferta.ofertaId}/homologar`, {}).pipe(finalize(() => {
      this.processando = false;
      this.cd.detectChanges();
    })).subscribe({
      next: (resposta: any) => {
        if (resposta?.pendencias?.length) {
          this.diario = { ...this.diario, pendencias: resposta.pendencias };
          this.mostrar(resposta.mensagem, 'erro');
          return;
        }
        this.concluirAcao(resposta.mensagem);
      },
      error: erro => this.exibirErro(erro, 'Nao foi possivel homologar o diario.')
    });
  }

  reabrir() {
    if (!this.diario?.oferta?.ofertaId) return;
    if (!this.motivo.trim()) {
      this.mostrar('Informe o motivo da reabertura.', 'erro');
      return;
    }
    this.processando = true;
    this.api.salvar(`coordenador/ofertas/${this.diario.oferta.ofertaId}/reabrir`, { motivo: this.motivo }).pipe(
      finalize(() => {
        this.processando = false;
        this.cd.detectChanges();
      })
    ).subscribe({
      next: (resposta: any) => this.concluirAcao(resposta.mensagem),
      error: erro => this.exibirErro(erro, 'Nao foi possivel reabrir o diario.')
    });
  }

  private concluirAcao(mensagem: string) {
    const id = this.diario.oferta.ofertaId;
    this.diarios = this.diarios.filter(item => item.ofertaId !== id);
    this.diario = undefined;
    this.motivo = '';
    this.mostrar(mensagem, 'sucesso');
  }

  private exibirErro(erro: any, padrao: string) {
    this.mostrar(erro?.error?.mensagem || padrao, 'erro');
  }

  private mostrar(mensagem: string, tipo: 'sucesso' | 'erro') {
    this.mensagem = mensagem;
    this.tipoMensagem = tipo;
  }
}
