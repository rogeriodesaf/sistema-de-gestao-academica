import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';

type Aba = 'resumo' | 'plano' | 'frequencia' | 'avaliacoes' | 'materiais';

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
  plano?: any;
  arquivos: any[] = [];
  historico?: any;
  visualizandoHistorico = false;
  aba: Aba = 'resumo';
  carregando = false;
  carregandoDetalhe = false;
  carregandoHistorico = false;
  baixandoHistorico = false;
  mensagem = '';
  private ofertaAnteriorId?: number;
  private posicaoLista = 0;
  private posicaoPortal = 0;

  constructor(private api: ApiService, private cd: ChangeDetectorRef) {}

  ngOnInit() {
    this.carregando = true;
    this.api.buscar('aluno', 'portal').pipe(finalize(() => {
      this.carregando = false;
      this.cd.detectChanges();
    })).subscribe({
      next: dados => {
        this.perfil = (dados as any)?.perfil;
        this.disciplinas = Array.isArray((dados as any)?.disciplinas) ? (dados as any).disciplinas : [];
      },
      error: erro => this.erro(erro, 'Não foi possível carregar o Portal do Aluno.')
    });
  }

  get disciplinasEmAndamento() {
    return this.disciplinas.filter(item => !item.resultadoDefinitivo);
  }

  get disciplinasConcluidas() {
    return this.disciplinas.filter(item => item.resultadoDefinitivo);
  }

  selecionar(item: any) {
    this.ofertaAnteriorId = item.ofertaId;
    this.posicaoLista = window.scrollY;
    this.limparDetalhe();
    this.disciplina = { ...item };
    this.carregandoDetalhe = true;
    this.focarInicioDetalhe();
    this.api.buscarAcao('aluno/disciplinas', item.ofertaId, 'detalhes').pipe(finalize(() => {
      this.carregandoDetalhe = false;
      this.cd.detectChanges();
    })).subscribe({
      next: (detalhes: any) => {
        this.disciplina = detalhes?.disciplina;
        this.frequencia = detalhes?.frequencia;
        this.aulas = Array.isArray(detalhes?.aulas) ? detalhes.aulas : [];
        this.resultado = detalhes?.resultado;
        this.plano = detalhes?.plano;
        this.arquivos = Array.isArray(detalhes?.arquivos) ? detalhes.arquivos : [];
      },
      error: erro => {
        this.disciplina = undefined;
        this.erro(erro, 'Não foi possível abrir a disciplina.');
      }
    });
  }

  voltarParaDisciplinas() {
    this.limparDetalhe();
    window.setTimeout(() => {
      const seletor = `[data-oferta-id="${this.ofertaAnteriorId}"]`;
      (document.querySelector(seletor) as HTMLElement | null)?.focus({ preventScroll: true });
      window.scrollTo({ top: this.posicaoLista, behavior: 'auto' });
    });
  }

  private focarInicioDetalhe() {
    window.setTimeout(() => {
      window.scrollTo({ top: 0, behavior: 'auto' });
      (document.querySelector('.voltar-disciplinas') as HTMLElement | null)?.focus();
    });
  }

  trocarAba(aba: Aba) {
    this.aba = aba;
    if (aba === 'materiais' && this.disciplina?.ofertaId) this.carregarMateriais();
  }

  private carregarMateriais() {
    this.api.obter(`aluno/disciplinas/${this.disciplina.ofertaId}/arquivos?t=${Date.now()}`).subscribe({
      next: dados => {
        this.arquivos = Array.isArray(dados) ? dados : [];
        this.cd.detectChanges();
      },
      error: erro => this.erro(erro, 'Não foi possível atualizar os materiais.')
    });
  }

  carregarHistorico() {
    this.posicaoPortal = window.scrollY;
    this.visualizandoHistorico = true;
    this.mensagem = '';
    this.focarInicioHistorico();
    if (this.historico) return;
    this.carregandoHistorico = true;
    this.api.buscar('aluno', 'historico').pipe(finalize(() => {
      this.carregandoHistorico = false;
      this.cd.detectChanges();
    })).subscribe({
      next: dados => this.historico = dados,
      error: erro => this.erro(erro, 'Não foi possível carregar o histórico.')
    });
  }

  voltarAoPortal() {
    this.visualizandoHistorico = false;
    this.mensagem = '';
    window.setTimeout(() => {
      (document.querySelector('.abrir-historico') as HTMLElement | null)?.focus({ preventScroll: true });
      window.scrollTo({ top: this.posicaoPortal, behavior: 'auto' });
    });
  }

  private focarInicioHistorico() {
    window.setTimeout(() => {
      window.scrollTo({ top: 0, behavior: 'auto' });
      (document.querySelector('.voltar-historico') as HTMLElement | null)?.focus();
    });
  }

  baixarHistorico() {
    this.baixandoHistorico = true;
    this.api.baixar('aluno/historico/pdf').pipe(finalize(() => {
      this.baixandoHistorico = false;
      this.cd.detectChanges();
    })).subscribe({
      next: blob => this.salvarBlob(blob, 'historico-escolar.pdf'),
      error: erro => this.erro(erro, 'Não foi possível gerar o histórico em PDF.')
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
      error: erro => this.erro(erro, 'Arquivo indisponível no momento.')
    });
  }

  abrirPlano(download = false) {
    if (!this.disciplina?.ofertaId) return;
    const sufixo = download ? '/download' : '';
    this.api.baixar(`aluno/disciplinas/${this.disciplina.ofertaId}/plano/pdf${sufixo}`).subscribe({
      next: blob => {
        if (download) this.salvarBlob(blob, this.plano?.arquivoNome || 'plano-de-ensino.pdf');
        else {
          const url = URL.createObjectURL(blob);
          window.open(url, '_blank', 'noopener');
          setTimeout(() => URL.revokeObjectURL(url), 60000);
        }
      },
      error: erro => this.erro(erro, 'Plano de ensino em PDF indisponível no momento.')
    });
  }

  periodosHistorico(): string[] {
    return this.historico ? Object.keys(this.historico.porPeriodo || {}) : [];
  }

  formatar(valor: any): string {
    return valor === null || valor === undefined || valor === '' ? '-' : String(valor);
  }

  percentual(valor: any): string {
    return valor === null || valor === undefined ? 'Frequência ainda não disponível.' : `${valor}%`;
  }

  enumTexto(valor: string): string {
    if (!valor) return '-';
    return ({
      ATIVA: 'Ativa', MATRICULADO: 'Ativa', TRANCADO: 'Trancada', CANCELADO: 'Cancelada',
      EM_ANDAMENTO: 'Em andamento', APROVADO: 'Aprovado',
      REPROVADO_POR_NOTA: 'Reprovado por nota',
      REPROVADO_POR_FREQUENCIA: 'Reprovado por frequência',
      REPROVADO_POR_NOTA_E_FREQUENCIA: 'Reprovado por nota e frequência',
      PLANEJADA: 'Planejada', ABERTA: 'Aberta', AGUARDANDO_HOMOLOGACAO: 'Aguardando homologação',
      ENCERRADA: 'Encerrada', CONCLUIDA: 'Concluída', CANCELADA: 'Cancelada',
      HOMOLOGADO: 'Homologado', ENCERRADO: 'Encerrado',
      PRESENTE: 'Presente', AUSENTE: 'Ausente', JUSTIFICADO: 'Justificada',
      NAO_REGISTRADA: 'Ainda não registrada', LANCADA: 'Lançada', NAO_LANCADA: 'Ainda não lançada',
      CONCLUIDO: 'Concluído', CURSANDO: 'Cursando', PENDENTE: 'Pendente'
    } as Record<string, string>)[valor] || valor.replaceAll('_', ' ').toLowerCase();
  }

  private limparDetalhe() {
    this.disciplina = undefined;
    this.frequencia = undefined;
    this.aulas = [];
    this.resultado = undefined;
    this.plano = undefined;
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
